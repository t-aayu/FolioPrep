package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.DeckDetailBundle
import com.example.data.local.DeckSummary
import com.example.data.local.McqQuestionEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.StudyDao
import com.example.data.local.StudyDeckEntity
import com.example.data.local.SubjectiveCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class StudyRepository(
    private val context: Context,
    private val studyDao: StudyDao,
    private val examYearPreferences: ExamYearPreferences
) {
    val examYearConfig: Flow<ExamYearConfig> = examYearPreferences.examYearConfigFlow

    val appSettings: Flow<AppSettingsConfig> = examYearPreferences.appSettingsFlow

    val deckSummaries: Flow<List<DeckSummary>> = studyDao.observeDeckSummaries()

    val allMcqs: Flow<List<McqQuestionEntity>> = studyDao.observeAllMcqs()

    fun observeMcqsByDifficulty(difficulty: String): Flow<List<McqQuestionEntity>> =
        studyDao.observeMcqsByDifficulty(difficulty)

    fun observeDeckDetail(deckId: Long): Flow<DeckDetailBundle?> {
        return combine(
            studyDao.observeDeck(deckId),
            studyDao.observeMcqsForDeck(deckId),
            studyDao.observeSubjectiveForDeck(deckId),
            studyDao.observeAttemptsForDeck(deckId)
        ) { deck, mcqs, subjective, attempts ->
            deck?.let {
                DeckDetailBundle(
                    deck = it,
                    mcqs = mcqs,
                    subjectiveCards = subjective,
                    attempts = attempts
                )
            }
        }
    }

    suspend fun updatePrelimsYear(year: Int) {
        examYearPreferences.setPrelimsYear(year)
        autoBackupIfEnabled()
    }

    suspend fun updateMainsYear(year: Int) {
        examYearPreferences.setMainsYear(year)
        autoBackupIfEnabled()
    }

    suspend fun updateBothExamYears(prelimsYear: Int, mainsYear: Int) {
        examYearPreferences.setBothYears(prelimsYear, mainsYear)
        autoBackupIfEnabled()
    }

    suspend fun updateThemeMode(mode: AppThemeMode) = examYearPreferences.setThemeMode(mode)

    suspend fun updateUseCustomSkill(enabled: Boolean) {
        examYearPreferences.setUseCustomSkill(enabled)
        autoBackupIfEnabled()
    }

    suspend fun updateCustomSkillText(skillText: String) {
        examYearPreferences.setCustomSkillText(skillText)
        autoBackupIfEnabled()
    }

    suspend fun updateAutoBackupEnabled(enabled: Boolean) {
        examYearPreferences.setAutoBackupEnabled(enabled)
        if (enabled) {
            autoBackupIfEnabled()
        }
    }

    suspend fun updateBackupTreeUri(uriString: String) {
        examYearPreferences.setBackupTreeUri(uriString)
        autoBackupIfEnabled()
    }

    suspend fun dismissFreshRestorePrompt() {
        examYearPreferences.setDismissedFreshRestorePrompt(true)
    }

    suspend fun updateLastGeneratedMcqType(typeLabel: String) {
        examYearPreferences.setLastGeneratedMcqType(typeLabel)
    }

    suspend fun saveDeck(
        deck: StudyDeckEntity,
        mcqs: List<McqQuestionEntity>,
        subjectiveCards: List<SubjectiveCardEntity>
    ): Long {
        val id = studyDao.saveFullDeck(deck, mcqs, subjectiveCards)
        examYearPreferences.setDismissedFreshRestorePrompt(true)
        autoBackupIfEnabled()
        return id
    }

    suspend fun deleteDeck(deckId: Long) {
        studyDao.deleteDeckById(deckId)
        autoBackupIfEnabled()
    }

    suspend fun toggleSubjectiveMastery(cardId: Long, isMastered: Boolean, deckId: Long) {
        studyDao.updateSubjectiveMastery(cardId, isMastered)
        studyDao.updateDeckLastPracticed(deckId, System.currentTimeMillis())
        autoBackupIfEnabled()
    }

    suspend fun recordQuizAttempt(attempt: QuizAttemptEntity) {
        studyDao.insertAttempt(attempt)
        studyDao.updateDeckLastPracticed(attempt.deckId, attempt.completedAt)
        autoBackupIfEnabled()
    }

    suspend fun performLocalBackup(): Result<BackupWriteResult> {
        val settings = appSettings.first()
        val years = examYearConfig.first()
        val snapshot = buildCurrentSnapshot(years, settings)
        val result = LocalBackupManager.writeBackup(
            context = context,
            snapshot = snapshot,
            customTreeUriString = settings.backupTreeUri
        )
        result.onSuccess { info ->
            examYearPreferences.setLastBackupInfo(info.timestamp, info.deckCount)
        }
        return result
    }

    suspend fun autoBackupIfEnabled() {
        val settings = appSettings.first()
        if (!settings.autoBackupEnabled) return
        if (studyDao.getDeckCount() == 0) return
        performLocalBackup()
    }

    suspend fun restoreFromBackupUri(uri: Uri): Result<BackupRestoreResult> {
        val parsed = LocalBackupManager.readBackupFromUri(context, uri)
        return parsed.mapCatching { snapshot ->
            val existingDecks = studyDao.getAllDecksOnce()
            val existingKeys = existingDecks.map { "${it.title.trim().lowercase()}|${it.createdAt}" }.toHashSet()

            var restoredDecks = 0
            var skipped = 0
            var restoredMcqs = 0
            var restoredSubjective = 0

            snapshot.decks.forEach { bundle ->
                val key = "${bundle.deck.title.trim().lowercase()}|${bundle.deck.createdAt}"
                if (existingKeys.contains(key)) {
                    skipped++
                } else {
                    studyDao.saveRestoredDeck(
                        deck = bundle.deck,
                        mcqs = bundle.mcqs,
                        subjectiveCards = bundle.subjectiveCards,
                        attempts = bundle.attempts
                    )
                    existingKeys.add(key)
                    restoredDecks++
                    restoredMcqs += bundle.mcqs.size
                    restoredSubjective += bundle.subjectiveCards.size
                }
            }

            examYearPreferences.setBothYears(snapshot.latestPrelimsYear, snapshot.latestMainsYear)
            if (snapshot.customSkillText.isNotBlank()) {
                examYearPreferences.setCustomSkillText(snapshot.customSkillText)
                examYearPreferences.setUseCustomSkill(snapshot.useCustomSkill)
            }
            examYearPreferences.setDismissedFreshRestorePrompt(true)

            BackupRestoreResult(
                restoredDecks = restoredDecks,
                skippedDuplicateDecks = skipped,
                restoredMcqs = restoredMcqs,
                restoredSubjective = restoredSubjective
            )
        }
    }

    private suspend fun buildCurrentSnapshot(
        years: ExamYearConfig,
        settings: AppSettingsConfig
    ): BackupSnapshot {
        val decks = studyDao.getAllDecksOnce()
        val bundles = decks.map { deck ->
            DeckDetailBundle(
                deck = deck,
                mcqs = studyDao.getMcqsForDeckOnce(deck.id),
                subjectiveCards = studyDao.getSubjectiveForDeckOnce(deck.id),
                attempts = studyDao.getAttemptsForDeckOnce(deck.id)
            )
        }
        return BackupSnapshot(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            latestPrelimsYear = years.latestPrelimsYear,
            latestMainsYear = years.latestMainsYear,
            useCustomSkill = settings.useCustomSkill,
            customSkillText = settings.customSkillText,
            decks = bundles
        )
    }
}
