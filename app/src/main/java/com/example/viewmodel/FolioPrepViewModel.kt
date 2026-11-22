package com.example.viewmodel

import android.app.Application
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
import com.example.domain.OcrExtractor
import com.example.domain.ParsedStudyPayload
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InputSourceTab(val label: String) {
    TEXT("Text"),
    LINK("Link"),
    IMAGE("Image OCR")
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
    val mcqCount: Int = 0,
    val mains10Count: Int = 0,
    val mains15Count: Int = 0,
    val promptText: String = "",
    val isPromptManuallyEdited: Boolean = false,
    val yearChangedWhileCustomPrompt: Boolean = false,
    val installedAiApps: List<InstalledAiApp> = emptyList(),
    val pendingAiImport: Boolean = false,
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

data class LibraryFilterState(
    val searchQuery: String = "",
    val selectedTag: String? = null
)

class FolioPrepViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyRepository(
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

    val appSettings: StateFlow<AppSettingsConfig> = repository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettingsConfig()
    )

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState: StateFlow<LibraryFilterState> = _filterState.asStateFlow()

    val allDecks: StateFlow<List<DeckSummary>> = repository.deckSummaries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredDecks: StateFlow<List<DeckSummary>> = combine(
        allDecks,
        _filterState
    ) { decks, filter ->
        decks.filter { deck ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                deck.title.contains(filter.searchQuery, ignoreCase = true) ||
                deck.tagsSerialized.contains(filter.searchQuery, ignoreCase = true)
            val matchesTag = filter.selectedTag == null ||
                deck.tagList().any { it.equals(filter.selectedTag, ignoreCase = true) }
            matchesQuery && matchesTag
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableLibraryTags: StateFlow<List<String>> = allDecks.combine(_filterState) { decks, _ ->
        val savedTags = decks.flatMap { it.tagList() }
        (UpscSubjects.allTags + savedTags).distinctBy { it.lowercase() }
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

    init {
        viewModelScope.launch {
            combine(examYearConfig, appSettings) { years, settings ->
                years to settings
            }.collect { (years, settings) ->
                _studioState.update { current ->
                    if (current.isPromptManuallyEdited) {
                        current.copy(yearChangedWhileCustomPrompt = true)
                    } else {
                        current.copy(
                            promptText = buildPromptForState(current, years, settings),
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

    // --- Library Filters & Deck Operations ---

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
        _studioState.update { current ->
            val updated = current.copy(
                sourceText = "",
                detectedSourceTitle = "",
                selectedImageUris = emptyList(),
                chunks = emptyList(),
                selectedChunkIndex = 0,
                extractionStatusMessage = null,
                extractionError = null
            )
            syncPromptIfAuto(updated)
        }
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
                            extractionStatusMessage = "Extracted locally via Jsoup · Stripped ~${article.reductionPercent}% HTML noise"
                        )
                        syncPromptIfAuto(updated)
                    }
                },
                onFailure = { error ->
                    _studioState.update {
                        it.copy(
                            isExtracting = false,
                            extractionError = error.message ?: "Failed to extract article text locally."
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
                            extractionStatusMessage = "On-device OCR complete (${uris.size} image${if (uris.size > 1) "s" else ""})"
                        )
                        syncPromptIfAuto(updated)
                    }
                },
                onFailure = { error ->
                    _studioState.update {
                        it.copy(
                            isExtracting = false,
                            extractionError = error.message ?: "On-device OCR failed to read the image."
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
        if (raw.startsWith("Act strictly as a senior UPSC", ignoreCase = true) ||
            raw.startsWith(UpscPromptBuilder.DEFAULT_SKILL_PROMPT, ignoreCase = true) ||
            (customSkill.isNotEmpty() && raw.startsWith(customSkill, ignoreCase = true))
        ) return

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
                val parsed = JsonPayloadParser.parse(raw, fallbackTitle).getOrNull() ?: return

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
        val result = JsonPayloadParser.parse(rawText, fallbackTitle)
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
                    it.copy(importErrorMessage = error.message ?: "Unable to parse JSON payload.")
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
                    orderIndex = index
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
            _studioState.update {
                it.copy(
                    draftImport = null,
                    transientToastMessage = "Saved offline deck with ${mcqEntities.size} MCQs & ${subjectiveEntities.size} Mains cards"
                )
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
        return UpscPromptBuilder.buildPrompt(
            sourceText = chunkText,
            latestPrelimsYear = years.latestPrelimsYear,
            latestMainsYear = years.latestMainsYear,
            questionMode = state.questionMode,
            mcqCount = state.mcqCount,
            mains10Count = state.mains10Count,
            mains15Count = state.mains15Count,
            subjectTag = state.selectedSubjectFocus,
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
