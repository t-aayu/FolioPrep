package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettingsConfig
import com.example.data.AppThemeMode
import com.example.data.ExamYearConfig
import com.example.data.ExamYearPreferences
import com.example.data.StudyRepository
import com.example.data.local.DeckDetailBundle
import com.example.data.local.DeckSummary
import com.example.data.local.FolioPrepDatabase
import com.example.data.local.McqQuestionEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.StudyDeckEntity
import com.example.data.local.SubjectiveCardEntity
import com.example.data.local.serializeKeyPoints
import com.example.data.local.serializeTags
import com.example.domain.ArticleExtractor
import com.example.domain.DraftMcq
import com.example.domain.DraftSubjective
import com.example.domain.ExternalAiLauncher
import com.example.domain.InstalledAiApp
import com.example.domain.JsonPayloadParser
import com.example.domain.McqQuestionType
import com.example.domain.McqTypeSelection
import com.example.domain.OcrExtractor
import com.example.domain.ParsedStudyPayload
import com.example.domain.QuestionDifficulty
import com.example.domain.QuestionMode
import com.example.domain.TextChunk
import com.example.domain.TextChunker
import com.example.domain.UpscPromptBuilder
import com.example.domain.UpscSubjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InputSourceTab(val label: String) {
    TEXT("Text"),
    LINK("Link"),
    IMAGE("Photo Scan")
}

data class StudioUiState(
    val inputTab: InputSourceTab = InputSourceTab.TEXT,
    val sourceText: String = "",
    val detectedSourceTitle: String = "",
    val urlInput: String = "",
    val selectedImageUris: List<Uri> = emptyList(),
    val isExtracting: Boolean = false,
    val extractionStatusMessage: String? = null,
    val extractionError: String? = null,
    val chunks: List<TextChunk> = emptyList(),
    val selectedChunkIndex: Int = 0,
    val selectedSubjectFocus: String? = null,
    val questionMode: QuestionMode = QuestionMode.MCQ,
    val selectedDifficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val mcqTypeSelection: McqTypeSelection = McqTypeSelection.AutoRotate,
    val autoPickedMcqType: McqQuestionType = McqQuestionType.THREE_STATEMENT,
    val customMcqTypeText: String = "",
    val mcqCount: Int = 0,
    val mains10Count: Int = 0,
    val mains15Count: Int = 0,
    val promptText: String = "",
    val isPromptManuallyEdited: Boolean = false,
    val yearChangedWhileCustomPrompt: Boolean = false,
    val installedAiApps: List<InstalledAiApp> = emptyList(),
    val pendingAiImport: Boolean = false,
    val pendingNavigateToStudio: Boolean = false,
    val detectedClipboardBanner: ParsedStudyPayload? = null,
    val lastIgnoredClipboardHash: Int? = null,
    val lastAutoPastedClipboardHash: Int? = null,
    val draftImport: ParsedStudyPayload? = null,
    val importErrorMessage: String? = null,
    val transientToastMessage: String? = null
) {
    val activeSourceWordCount: Int get() = TextChunker.countWords(sourceText)
    val activeChunk: TextChunk? get() = chunks.getOrNull(selectedChunkIndex)
    val totalRequestedQuestions: Int
        get() = when (questionMode) {
            QuestionMode.MCQ -> mcqCount
            QuestionMode.SUBJECTIVE -> mains10Count + mains15Count
            QuestionMode.BOTH -> mcqCount + mains10Count + mains15Count
        }
}

enum class LibraryCategory(val label: String) {
    ALL("All"),
    PRELIMS("Prelims - MCQs"),
    MAINS("Mains")
}

data class LibraryFilterState(
    val selectedCategory: LibraryCategory = LibraryCategory.ALL,
    val searchQuery: String = "",
    val selectedTag: String? = null
)

class FolioPrepViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyRepository(
        context = application,
        studyDao = FolioPrepDatabase.getInstance(application).studyDao(),
        examYearPreferences = ExamYearPreferences(application)
    )
    private val articleExtractor = ArticleExtractor()
    private val ocrExtractor = OcrExtractor(application)

    val examYearConfig: StateFlow<ExamYearConfig> = repository.examYearConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExamYearConfig()
    )

    private val appSettingsLoaded: StateFlow<AppSettingsConfig?> = repository.appSettings
        .map<AppSettingsConfig, AppSettingsConfig?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val appSettings: StateFlow<AppSettingsConfig> = appSettingsLoaded
        .map { it ?: AppSettingsConfig() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettingsConfig()
        )

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState: StateFlow<LibraryFilterState> = _filterState.asStateFlow()

    private val decksLoaded: StateFlow<List<DeckSummary>?> = repository.deckSummaries
        .map<List<DeckSummary>, List<DeckSummary>?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val allDecks: StateFlow<List<DeckSummary>> = decksLoaded
        .map { it.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMcqs: StateFlow<List<McqQuestionEntity>> = repository.allMcqs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Prompts the user to restore from a local backup file ONLY when Room has confirmed 0 saved
     * decks (e.g., fresh installation after uninstallation) AND the user hasn't dismissed it yet.
     */
    val shouldShowFreshRestorePrompt: StateFlow<Boolean> = combine(
        decksLoaded,
        appSettingsLoaded
    ) { decks, settings ->
        decks != null && settings != null && decks.isEmpty() && !settings.hasDismissedFreshRestorePrompt
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val filteredDecks: StateFlow<List<DeckSummary>> = combine(
        allDecks,
        _filterState
    ) { decks, filter ->
        decks.filter { deck ->
            val matchesCategory = when (filter.selectedCategory) {
                LibraryCategory.ALL -> true
                LibraryCategory.PRELIMS -> deck.mcqCount > 0
                LibraryCategory.MAINS -> deck.subjectiveCount > 0
            }
            val matchesQuery = filter.searchQuery.isBlank() ||
                deck.title.contains(filter.searchQuery, ignoreCase = true) ||
                deck.tagsSerialized.contains(filter.searchQuery, ignoreCase = true)
            val matchesTag = filter.selectedTag == null ||
                deck.tagList().any { it.equals(filter.selectedTag, ignoreCase = true) }
            matchesCategory && matchesQuery && matchesTag
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableLibraryTags: StateFlow<List<String>> = allDecks.combine(_filterState) { decks, filter ->
        val matchingDecks = when (filter.selectedCategory) {
            LibraryCategory.ALL -> decks
            LibraryCategory.PRELIMS -> decks.filter { it.mcqCount > 0 }
            LibraryCategory.MAINS -> decks.filter { it.subjectiveCount > 0 }
        }
        val baseTags = when (filter.selectedCategory) {
            LibraryCategory.ALL -> UpscSubjects.allTags
            LibraryCategory.PRELIMS -> UpscSubjects.mcqTags
            LibraryCategory.MAINS -> UpscSubjects.mainsTags
        }
        val savedTags = matchingDecks.flatMap { it.tagList() }
        (baseTags + savedTags).distinctBy { it.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UpscSubjects.allTags
    )

    private val _studioState = MutableStateFlow(
        StudioUiState(
            installedAiApps = ExternalAiLauncher.detectInstalledAiApps(application)
        )
    )
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    private var hasInitializedAutoMcqType = false

    init {
        viewModelScope.launch {
            combine(examYearConfig, appSettingsLoaded) { years, loadedSettings ->
                years to loadedSettings
            }.collect { (years, loadedSettings) ->
                val settings = loadedSettings ?: AppSettingsConfig()
                _studioState.update { current ->
                    var updated = current
                    if (loadedSettings != null && !hasInitializedAutoMcqType) {
                        hasInitializedAutoMcqType = true
                        updated = updated.copy(
                            autoPickedMcqType = UpscPromptBuilder.pickNextMcqTypeExcluding(
                                loadedSettings.lastGeneratedMcqType
                            )
                        )
                    }
                    if (updated.isPromptManuallyEdited) {
                        updated.copy(yearChangedWhileCustomPrompt = true)
                    } else {
                        updated.copy(
                            promptText = buildPromptForState(updated, years, settings),
                            yearChangedWhileCustomPrompt = false
                        )
                    }
                }
            }
        }
    }

    // --- Exam Year & App Settings Persistence (DataStore) ---

    fun updatePrelimsYear(year: Int) {
        viewModelScope.launch {
            repository.updatePrelimsYear(year)
            showToast("UPSC Prelims pattern updated to $year & previous years")
        }
    }

    fun updateMainsYear(year: Int) {
        viewModelScope.launch {
            repository.updateMainsYear(year)
            showToast("UPSC Mains pattern updated to $year & previous years")
        }
    }

    fun updateBothExamYears(prelimsYear: Int, mainsYear: Int) {
        viewModelScope.launch {
            repository.updateBothExamYears(prelimsYear, mainsYear)
            showToast("Exam patterns saved: Prelims $prelimsYear · Mains $mainsYear")
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun updateUseCustomSkill(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUseCustomSkill(enabled)
            val label = if (enabled) "Custom Skill enabled" else "Default UPSC CSE Paper Setter enabled"
            showToast(label)
        }
    }

    fun saveCustomSkillText(skillText: String) {
        val cleaned = skillText.trim()
        viewModelScope.launch {
            repository.updateCustomSkillText(cleaned)
            if (cleaned.isEmpty()) {
                showToast("Custom skill cleared; default UPSC CSE Paper Setter will be used")
            } else if (appSettings.value.useCustomSkill) {
                showToast("Custom skill saved and applied to Prompt Studio")
            } else {
                showToast("Custom skill saved — toggle Custom Skill ON to activate it")
            }
        }
    }

    // --- Automatic Local Backup & Restore ---

    fun updateAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoBackupEnabled(enabled)
            if (enabled) {
                val result = repository.performLocalBackup()
                result.fold(
                    onSuccess = { info ->
                        showToast("Auto-backup enabled · Saved ${info.deckCount} deck(s) to ${info.locationLabel}")
                    },
                    onFailure = {
                        showToast("Auto-backup enabled")
                    }
                )
            } else {
                showToast("Automatic local backup paused")
            }
        }
    }

    fun setBackupFolderUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
            }
            repository.updateBackupTreeUri(uri.toString())
            val result = repository.performLocalBackup()
            result.fold(
                onSuccess = { info ->
                    showToast("Backup folder updated · Saved ${info.deckCount} deck(s)")
                },
                onFailure = { err ->
                    showToast("Backup folder selected (write failed: ${err.message ?: "error"})")
                }
            )
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            val result = repository.performLocalBackup()
            result.fold(
                onSuccess = { info ->
                    showToast("Backed up ${info.deckCount} deck(s), ${info.mcqCount} MCQs & ${info.subjectiveCount} Mains to ${info.locationLabel}")
                },
                onFailure = { err ->
                    showToast("Backup failed: ${err.message ?: "Unable to write backup file"}")
                }
            )
        }
    }

    fun restoreFromBackupUri(uri: Uri) {
        viewModelScope.launch {
            val result = repository.restoreFromBackupUri(uri)
            result.fold(
                onSuccess = { info ->
                    val summary = when {
                        info.restoredDecks > 0 ->
                            "Restored ${info.restoredDecks} deck(s) (${info.restoredMcqs} MCQs, ${info.restoredSubjective} Mains)"
                        info.skippedDuplicateDecks > 0 ->
                            "All ${info.skippedDuplicateDecks} deck(s) in this backup are already in your library"
                        else ->
                            "Backup restored (settings synced; 0 decks in file)"
                    }
                    showToast(summary)
                },
                onFailure = { err ->
                    showToast("Restore failed: ${err.message ?: "Invalid FolioPrep backup file"}")
                }
            )
        }
    }

    fun dismissFreshRestorePrompt() {
        viewModelScope.launch {
            repository.dismissFreshRestorePrompt()
        }
    }

    // --- Library Filters & Deck Operations ---

    fun onCategorySelected(category: LibraryCategory) {
        _filterState.update { current ->
            if (current.selectedCategory == category) current
            else current.copy(selectedCategory = category, selectedTag = null)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onTagFilterSelected(tag: String?) {
        _filterState.update { current ->
            val next = if (current.selectedTag == tag) null else tag
            current.copy(selectedTag = next)
        }
    }

    fun observeDeckDetail(deckId: Long): Flow<DeckDetailBundle?> =
        repository.observeDeckDetail(deckId)

    fun observeMcqsByDifficulty(difficulty: String): Flow<List<McqQuestionEntity>> =
        repository.observeMcqsByDifficulty(difficulty)

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            showToast("Deck deleted from offline library")
        }
    }

    fun toggleSubjectiveMastery(cardId: Long, isMastered: Boolean, deckId: Long) {
        viewModelScope.launch {
            repository.toggleSubjectiveMastery(cardId, isMastered, deckId)
        }
    }

    fun recordQuizAttempt(
        deckId: Long,
        mode: String,
        totalQuestions: Int,
        correctCount: Int,
        wrongCount: Int,
        skippedCount: Int,
        negativeFactor: Double,
        netScore: Double,
        maxScore: Double,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            repository.recordQuizAttempt(
                QuizAttemptEntity(
                    deckId = deckId,
                    mode = mode,
                    totalQuestions = totalQuestions,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    skippedCount = skippedCount,
                    negativeFactor = negativeFactor,
                    netScore = netScore,
                    maxScore = maxScore,
                    durationSeconds = durationSeconds
                )
            )
        }
    }

    // --- Studio Input & Local Pre-Processing ---

    fun refreshInstalledAiApps() {
        val apps = ExternalAiLauncher.detectInstalledAiApps(getApplication())
        _studioState.update { it.copy(installedAiApps = apps) }
    }

    fun selectInputTab(tab: InputSourceTab) {
        _studioState.update { it.copy(inputTab = tab, extractionError = null) }
    }

    fun updateUrlInput(url: String) {
        _studioState.update { it.copy(urlInput = url, extractionError = null) }
    }

    fun updateSourceText(newText: String) {
        val chunks = TextChunker.chunkText(newText)
        _studioState.update { current ->
            val nextIndex = current.selectedChunkIndex.coerceIn(0, (chunks.size - 1).coerceAtLeast(0))
            val updated = current.copy(
                sourceText = newText,
                chunks = chunks,
                selectedChunkIndex = nextIndex,
                extractionError = null
            )
            syncPromptIfAuto(updated)
        }
    }

    fun clearSourceText() {
        val nextAutoType = UpscPromptBuilder.pickNextMcqTypeExcluding(appSettings.value.lastGeneratedMcqType)
        _studioState.update { current ->
            val updated = current.copy(
                sourceText = "",
                detectedSourceTitle = "",
                selectedImageUris = emptyList(),
                chunks = emptyList(),
                selectedChunkIndex = 0,
                autoPickedMcqType = nextAutoType,
                extractionStatusMessage = null,
                extractionError = null
            )
            syncPromptIfAuto(updated)
        }
    }

    fun receiveSharedTextFromExternalApp(
        text: String,
        title: String = "",
        isUrlOnly: Boolean = false
    ) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return

        if (isUrlOnly) {
            _studioState.update { current ->
                current.copy(
                    inputTab = InputSourceTab.LINK,
                    urlInput = cleaned,
                    detectedSourceTitle = title.ifBlank { current.detectedSourceTitle },
                    extractionStatusMessage = "Article link received — tap Import Article to load text.",
                    extractionError = null,
                    pendingNavigateToStudio = true,
                    transientToastMessage = "Link loaded into Studio"
                )
            }
        } else {
            val chunks = TextChunker.chunkText(cleaned)
            val wordCount = TextChunker.countWords(cleaned)
            _studioState.update { current ->
                val updated = current.copy(
                    inputTab = InputSourceTab.TEXT,
                    sourceText = cleaned,
                    detectedSourceTitle = title.ifBlank { current.detectedSourceTitle },
                    chunks = chunks,
                    selectedChunkIndex = 0,
                    extractionStatusMessage = "Loaded shared text ($wordCount words)",
                    extractionError = null,
                    pendingNavigateToStudio = true,
                    transientToastMessage = "Selected text loaded into Studio"
                )
                syncPromptIfAuto(updated)
            }
        }
    }

    fun consumeNavigateToStudio() {
        _studioState.update { it.copy(pendingNavigateToStudio = false) }
    }

    fun selectChunk(index: Int) {
        _studioState.update { current ->
            val updated = current.copy(
                selectedChunkIndex = index.coerceIn(0, (current.chunks.size - 1).coerceAtLeast(0))
            )
            syncPromptIfAuto(updated)
        }
    }

    fun extractArticleFromUrl() {
        val url = _studioState.value.urlInput.trim()
        if (url.isEmpty()) {
            _studioState.update { it.copy(extractionError = "Enter an article URL (e.g., The Hindu, PIB, PRS, Indian Express).") }
            return
        }
        viewModelScope.launch {
            _studioState.update {
                it.copy(isExtracting = true, extractionError = null, extractionStatusMessage = null)
            }
            val result = articleExtractor.extractFromUrl(url)
            result.fold(
                onSuccess = { article ->
                    val chunks = TextChunker.chunkText(article.cleanText)
                    _studioState.update { current ->
                        val updated = current.copy(
                            isExtracting = false,
                            sourceText = article.cleanText,
                            detectedSourceTitle = article.title,
                            chunks = chunks,
                            selectedChunkIndex = 0,
                            extractionError = null,
                            extractionStatusMessage = "Article text imported (${TextChunker.countWords(article.cleanText)} words)"
                        )
                        syncPromptIfAuto(updated)
                    }
                },
                onFailure = { error ->
                    _studioState.update {
                        it.copy(
                            isExtracting = false,
                            extractionError = error.message ?: "Could not load text from this article link."
                        )
                    }
                }
            )
        }
    }

    fun runOcrOnImages(uris: List<Uri>, append: Boolean = false) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    isExtracting = true,
                    extractionError = null,
                    extractionStatusMessage = null,
                    selectedImageUris = if (append) (it.selectedImageUris + uris).distinct() else uris
                )
            }
            val result = ocrExtractor.extractTextFromUris(uris)
            result.fold(
                onSuccess = { extracted ->
                    _studioState.update { current ->
                        val mergedText = if (append && current.sourceText.isNotBlank()) {
                            "${current.sourceText.trim()}\n\n$extracted"
                        } else {
                            extracted
                        }
                        val chunks = TextChunker.chunkText(mergedText)
                        val updated = current.copy(
                            isExtracting = false,
                            sourceText = mergedText,
                            detectedSourceTitle = current.detectedSourceTitle.ifBlank { "Scanned UPSC Notes" },
                            chunks = chunks,
                            selectedChunkIndex = 0,
                            extractionError = null,
                            extractionStatusMessage = "Text scanned from ${uris.size} photo${if (uris.size > 1) "s" else ""}"
                        )
                        syncPromptIfAuto(updated)
                    }
                },
                onFailure = { error ->
                    _studioState.update {
                        it.copy(
                            isExtracting = false,
                            extractionError = error.message ?: "Could not read text from the selected photo."
                        )
                    }
                }
            )
        }
    }

    // --- Question Configuration & Editable Prompt ---

    fun selectSubjectFocus(tag: String?) {
        _studioState.update { current ->
            val next = if (current.selectedSubjectFocus == tag) null else tag
            syncPromptIfAuto(current.copy(selectedSubjectFocus = next))
        }
    }

    fun selectQuestionMode(mode: QuestionMode) {
        _studioState.update { current ->
            val allowedTags = UpscSubjects.tagsForMode(mode)
            val preservedFocus = current.selectedSubjectFocus?.let { selected ->
                allowedTags.firstOrNull { it.equals(selected, ignoreCase = true) }
            }
            syncPromptIfAuto(
                current.copy(
                    questionMode = mode,
                    selectedSubjectFocus = preservedFocus
                )
            )
        }
    }

    fun selectDifficulty(difficulty: QuestionDifficulty) {
        _studioState.update { current ->
            syncPromptIfAuto(current.copy(selectedDifficulty = difficulty))
        }
    }

    fun selectMcqTypeOption(selection: McqTypeSelection) {
        _studioState.update { current ->
            val nextAuto = if (selection is McqTypeSelection.AutoRotate &&
                current.mcqTypeSelection !is McqTypeSelection.AutoRotate
            ) {
                UpscPromptBuilder.pickNextMcqTypeExcluding(appSettings.value.lastGeneratedMcqType)
            } else {
                current.autoPickedMcqType
            }
            syncPromptIfAuto(
                current.copy(
                    mcqTypeSelection = selection,
                    autoPickedMcqType = nextAuto
                )
            )
        }
    }

    fun updateCustomMcqTypeText(text: String) {
        _studioState.update { current ->
            syncPromptIfAuto(current.copy(customMcqTypeText = text))
        }
    }

    fun rotateAutoMcqType() {
        val last = appSettings.value.lastGeneratedMcqType
        _studioState.update { current ->
            val excluded = current.autoPickedMcqType.label
            val candidate = McqQuestionType.entries.filterNot {
                it.label.equals(last, ignoreCase = true) || it.label.equals(excluded, ignoreCase = true)
            }.let { pool ->
                if (pool.isNotEmpty()) pool[kotlin.random.Random.nextInt(pool.size)]
                else UpscPromptBuilder.pickNextMcqTypeExcluding(last)
            }
            syncPromptIfAuto(current.copy(autoPickedMcqType = candidate))
        }
    }

    fun adjustMcqCount(delta: Int) {
        _studioState.update { current ->
            val next = (current.mcqCount + delta).coerceIn(0, 30)
            syncPromptIfAuto(current.copy(mcqCount = next))
        }
    }

    fun adjustMains10Count(delta: Int) {
        _studioState.update { current ->
            val next = (current.mains10Count + delta).coerceIn(0, 15)
            syncPromptIfAuto(current.copy(mains10Count = next))
        }
    }

    fun adjustMains15Count(delta: Int) {
        _studioState.update { current ->
            val next = (current.mains15Count + delta).coerceIn(0, 15)
            syncPromptIfAuto(current.copy(mains15Count = next))
        }
    }

    fun updatePromptManually(editedText: String) {
        _studioState.update {
            it.copy(
                promptText = editedText,
                isPromptManuallyEdited = true
            )
        }
    }

    fun resetPromptToTemplate() {
        val years = examYearConfig.value
        val settings = appSettings.value
        _studioState.update { current ->
            current.copy(
                promptText = buildPromptForState(current, years, settings),
                isPromptManuallyEdited = false,
                yearChangedWhileCustomPrompt = false
            )
        }
        showToast("Prompt synced with UPSC CSE ${years.latestPrelimsYear}/${years.latestMainsYear} template")
    }

    // --- Stage 2 & 3: Share & Clipboard Auto-Import ---

    fun markAwaitingAiReturn() {
        val state = _studioState.value
        if (state.questionMode != QuestionMode.SUBJECTIVE) {
            val usedLabel = resolvedMcqTypeLabelForHistory(state)
            if (usedLabel.isNotBlank()) {
                viewModelScope.launch {
                    repository.updateLastGeneratedMcqType(usedLabel)
                }
            }
        }
        _studioState.update { it.copy(pendingAiImport = true, importErrorMessage = null) }
    }

    fun tryAutoPasteClipboard(clipboardText: String?, targetTab: InputSourceTab? = null) {
        val raw = clipboardText?.trim().orEmpty()
        if (raw.isEmpty()) return

        val state = _studioState.value
        val hash = raw.hashCode()
        if (hash == state.lastAutoPastedClipboardHash) return
        if (JsonPayloadParser.looksLikeStudyPayload(raw)) return
        val customSkill = appSettings.value.customSkillText.trim()
        if (UpscPromptBuilder.isGeneratedPrompt(raw, customSkill)) return

        val looksLikeUrl = isLikelyUrl(raw)
        if (looksLikeUrl && (targetTab == null || targetTab == InputSourceTab.LINK)) {
            if (state.urlInput.isBlank()) {
                _studioState.update {
                    it.copy(
                        urlInput = raw,
                        lastAutoPastedClipboardHash = hash,
                        extractionStatusMessage = "Article URL auto-pasted from clipboard",
                        extractionError = null
                    )
                }
            }
        } else if (!looksLikeUrl && (targetTab == null || targetTab == InputSourceTab.TEXT)) {
            if (state.sourceText.isBlank()) {
                val chunks = TextChunker.chunkText(raw)
                _studioState.update { current ->
                    val updated = current.copy(
                        sourceText = raw,
                        chunks = chunks,
                        selectedChunkIndex = 0,
                        lastAutoPastedClipboardHash = hash,
                        extractionStatusMessage = "Source text auto-pasted from clipboard",
                        extractionError = null
                    )
                    syncPromptIfAuto(updated)
                }
            }
        }
    }

    private fun isLikelyUrl(text: String): Boolean {
        if (text.contains('\n') || text.contains(' ')) return false
        val lower = text.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) {
            return true
        }
        return Regex("^[a-z0-9-]+(\\.[a-z0-9-]+)+(/[^\\s]*)?$").matches(lower)
    }

    fun onAppWindowFocused(clipboardText: String?) {
        val state = _studioState.value
        val raw = clipboardText?.trim().orEmpty()
        if (raw.isEmpty()) return

        if (state.pendingAiImport) {
            val hash = raw.hashCode()
            if (hash == state.lastIgnoredClipboardHash) return
            if (JsonPayloadParser.looksLikeStudyPayload(raw)) {
                val fallbackTitle = state.detectedSourceTitle.ifBlank {
                    state.selectedSubjectFocus?.let { "UPSC $it Set" } ?: "UPSC CSE Set"
                }
                val parsed = JsonPayloadParser.parse(
                    rawInput = raw,
                    fallbackTitle = fallbackTitle,
                    defaultDifficulty = state.selectedDifficulty
                ).getOrNull() ?: return

                _studioState.update {
                    it.copy(
                        pendingAiImport = false,
                        detectedClipboardBanner = parsed,
                        lastIgnoredClipboardHash = hash
                    )
                }
                return
            }
        }

        // Otherwise, attempt convenient auto-paste into empty Article URL / Source Text boxes
        tryAutoPasteClipboard(raw, targetTab = state.inputTab)
    }

    fun dismissClipboardBanner() {
        _studioState.update { it.copy(detectedClipboardBanner = null) }
    }

    fun openDetectedClipboardForPreview(): Boolean {
        val detected = _studioState.value.detectedClipboardBanner ?: return false
        val enriched = enrichTagsWithSelectedFocus(detected)
        _studioState.update {
            it.copy(
                draftImport = enriched,
                detectedClipboardBanner = null,
                importErrorMessage = null
            )
        }
        return true
    }

    fun parseAndOpenManualPayload(rawText: String): Boolean {
        val state = _studioState.value
        val fallbackTitle = state.detectedSourceTitle.ifBlank {
            state.selectedSubjectFocus?.let { "UPSC $it Set" } ?: "UPSC CSE Set"
        }
        val result = JsonPayloadParser.parse(
            rawInput = rawText,
            fallbackTitle = fallbackTitle,
            defaultDifficulty = state.selectedDifficulty
        )
        return result.fold(
            onSuccess = { parsed ->
                val enriched = enrichTagsWithSelectedFocus(parsed)
                _studioState.update {
                    it.copy(
                        draftImport = enriched,
                        detectedClipboardBanner = null,
                        pendingAiImport = false,
                        importErrorMessage = null
                    )
                }
                true
            },
            onFailure = { error ->
                _studioState.update {
                    it.copy(importErrorMessage = error.message ?: "Could not read the pasted questions. Please copy the full reply and try again.")
                }
                false
            }
        )
    }

    fun clearImportError() {
        _studioState.update { it.copy(importErrorMessage = null) }
    }

    // --- Stage 4: Draft Preview Editing & Saving to Room ---

    fun updateDraftTitle(title: String) {
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            state.copy(draftImport = draft.copy(title = title))
        }
    }

    fun toggleDraftTag(tag: String) {
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            val nextTags = if (draft.tags.any { it.equals(tag, ignoreCase = true) }) {
                draft.tags.filterNot { it.equals(tag, ignoreCase = true) }
            } else {
                draft.tags + tag
            }
            state.copy(draftImport = draft.copy(tags = nextTags))
        }
    }

    fun addCustomDraftTag(tag: String) {
        val clean = tag.trim()
        if (clean.isEmpty()) return
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            if (draft.tags.any { it.equals(clean, ignoreCase = true) }) return@update state
            state.copy(draftImport = draft.copy(tags = draft.tags + clean))
        }
    }

    fun updateDraftMcq(updated: DraftMcq) {
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            state.copy(
                draftImport = draft.copy(
                    mcqs = draft.mcqs.map { if (it.tempId == updated.tempId) updated else it }
                )
            )
        }
    }

    fun deleteDraftMcq(tempId: String) {
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            state.copy(draftImport = draft.copy(mcqs = draft.mcqs.filterNot { it.tempId == tempId }))
        }
    }

    fun updateDraftSubjective(updated: DraftSubjective) {
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            state.copy(
                draftImport = draft.copy(
                    subjective = draft.subjective.map { if (it.tempId == updated.tempId) updated else it }
                )
            )
        }
    }

    fun deleteDraftSubjective(tempId: String) {
        _studioState.update { state ->
            val draft = state.draftImport ?: return@update state
            state.copy(
                draftImport = draft.copy(
                    subjective = draft.subjective.filterNot { it.tempId == tempId }
                )
            )
        }
    }

    fun saveDraftDeckToLibrary(onSaved: (Long) -> Unit) {
        val state = _studioState.value
        val draft = state.draftImport ?: return
        if (draft.totalCount == 0) {
            showToast("Add or keep at least one question before saving.")
            return
        }
        val years = examYearConfig.value
        val finalTags = draft.tags.ifEmpty {
            listOfNotNull(state.selectedSubjectFocus).ifEmpty { listOf("Current Affairs") }
        }

        viewModelScope.launch {
            val deckEntity = StudyDeckEntity(
                title = draft.title.trim().ifEmpty { "UPSC CSE Study Set" },
                examScope = "UPSC CSE",
                prelimsYear = years.latestPrelimsYear,
                mainsYear = years.latestMainsYear,
                tagsSerialized = serializeTags(finalTags),
                sourceType = state.inputTab.name,
                sourceSnippet = state.sourceText.trim().take(240)
            )
            val mcqEntities = draft.mcqs.mapIndexed { index, item ->
                val opts = item.options + List((4 - item.options.size).coerceAtLeast(0)) { "" }
                McqQuestionEntity(
                    deckId = 0L,
                    question = item.question.trim(),
                    optionA = opts[0],
                    optionB = opts[1],
                    optionC = opts[2],
                    optionD = opts[3],
                    correctIndex = item.correctIndex.coerceIn(0, 3),
                    explanation = item.explanation.trim(),
                    orderIndex = index,
                    difficulty = QuestionDifficulty.fromRaw(item.difficulty).label
                )
            }
            val subjectiveEntities = draft.subjective.mapIndexed { index, item ->
                SubjectiveCardEntity(
                    deckId = 0L,
                    question = item.question.trim(),
                    marks = if (item.marks >= 13) 15 else 10,
                    intro = item.intro.trim(),
                    pointsSerialized = serializeKeyPoints(item.keyPoints),
                    conclusion = item.conclusion.trim(),
                    isMastered = false,
                    orderIndex = index
                )
            }
            val savedId = repository.saveDeck(deckEntity, mcqEntities, subjectiveEntities)
            val usedLabel = if (mcqEntities.isNotEmpty() || state.questionMode != QuestionMode.SUBJECTIVE) {
                resolvedMcqTypeLabelForHistory(state)
            } else {
                ""
            }
            if (usedLabel.isNotBlank()) {
                repository.updateLastGeneratedMcqType(usedLabel)
            }
            val nextAutoType = UpscPromptBuilder.pickNextMcqTypeExcluding(
                usedLabel.ifBlank { appSettings.value.lastGeneratedMcqType }
            )
            _studioState.update { current ->
                val cleared = current.copy(
                    draftImport = null,
                    autoPickedMcqType = nextAutoType,
                    transientToastMessage = "Saved offline deck with ${mcqEntities.size} MCQs & ${subjectiveEntities.size} Mains cards"
                )
                syncPromptIfAuto(cleared)
            }
            onSaved(savedId)
        }
    }

    fun clearDraftImport() {
        _studioState.update { it.copy(draftImport = null) }
    }

    fun consumeToast() {
        _studioState.update { it.copy(transientToastMessage = null) }
    }

    fun showToast(message: String) {
        _studioState.update { it.copy(transientToastMessage = message) }
    }

    private fun enrichTagsWithSelectedFocus(parsed: ParsedStudyPayload): ParsedStudyPayload {
        val focus = _studioState.value.selectedSubjectFocus
        val mergedTags = buildList {
            if (!focus.isNullOrBlank()) add(focus)
            addAll(parsed.tags)
        }.distinct()
        return parsed.copy(tags = mergedTags)
    }

    private fun resolveEffectiveMcqType(state: StudioUiState): Pair<McqQuestionType?, String> {
        if (state.questionMode == QuestionMode.SUBJECTIVE) return null to ""
        return when (val sel = state.mcqTypeSelection) {
            McqTypeSelection.AutoRotate -> state.autoPickedMcqType to ""
            is McqTypeSelection.Preset -> sel.type to ""
            McqTypeSelection.Custom -> null to state.customMcqTypeText.trim()
        }
    }

    private fun resolvedMcqTypeLabelForHistory(state: StudioUiState): String {
        val (preset, custom) = resolveEffectiveMcqType(state)
        return custom.ifBlank { preset?.label.orEmpty() }
    }

    private fun syncPromptIfAuto(state: StudioUiState): StudioUiState {
        if (state.isPromptManuallyEdited) return state
        return state.copy(promptText = buildPromptForState(state, examYearConfig.value, appSettings.value))
    }

    private fun buildPromptForState(
        state: StudioUiState,
        years: ExamYearConfig,
        settings: AppSettingsConfig
    ): String {
        val chunkText = state.chunks.getOrNull(state.selectedChunkIndex)?.text ?: state.sourceText
        val (effectiveMcqType, effectiveCustomType) = resolveEffectiveMcqType(state)
        return UpscPromptBuilder.buildPrompt(
            sourceText = chunkText,
            latestPrelimsYear = years.latestPrelimsYear,
            latestMainsYear = years.latestMainsYear,
            questionMode = state.questionMode,
            mcqCount = state.mcqCount,
            mains10Count = state.mains10Count,
            mains15Count = state.mains15Count,
            subjectTag = state.selectedSubjectFocus,
            difficulty = state.selectedDifficulty,
            mcqQuestionType = effectiveMcqType,
            customMcqQuestionType = effectiveCustomType,
            useCustomSkill = settings.useCustomSkill,
            customSkillText = settings.customSkillText
        )
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FolioPrepViewModel(application) as T
                }
            }
    }
}
