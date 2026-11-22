package com.example.data

import com.example.data.local.DeckDetailBundle
import com.example.data.local.DeckSummary
import com.example.data.local.McqQuestionEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.StudyDao
import com.example.data.local.StudyDeckEntity
import com.example.data.local.SubjectiveCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StudyRepository(
    private val studyDao: StudyDao,
    private val examYearPreferences: ExamYearPreferences
) {
    val examYearConfig: Flow<ExamYearConfig> = examYearPreferences.examYearConfigFlow

    val appSettings: Flow<AppSettingsConfig> = examYearPreferences.appSettingsFlow

    val deckSummaries: Flow<List<DeckSummary>> = studyDao.observeDeckSummaries()

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

    suspend fun updatePrelimsYear(year: Int) = examYearPreferences.setPrelimsYear(year)

    suspend fun updateMainsYear(year: Int) = examYearPreferences.setMainsYear(year)

    suspend fun updateBothExamYears(prelimsYear: Int, mainsYear: Int) =
        examYearPreferences.setBothYears(prelimsYear, mainsYear)

    suspend fun updateThemeMode(mode: AppThemeMode) = examYearPreferences.setThemeMode(mode)

    suspend fun updateUseCustomSkill(enabled: Boolean) = examYearPreferences.setUseCustomSkill(enabled)

    suspend fun updateCustomSkillText(skillText: String) = examYearPreferences.setCustomSkillText(skillText)

    suspend fun saveDeck(
        deck: StudyDeckEntity,
        mcqs: List<McqQuestionEntity>,
        subjectiveCards: List<SubjectiveCardEntity>
    ): Long = studyDao.saveFullDeck(deck, mcqs, subjectiveCards)

    suspend fun deleteDeck(deckId: Long) = studyDao.deleteDeckById(deckId)

    suspend fun toggleSubjectiveMastery(cardId: Long, isMastered: Boolean, deckId: Long) {
        studyDao.updateSubjectiveMastery(cardId, isMastered)
        studyDao.updateDeckLastPracticed(deckId, System.currentTimeMillis())
    }

    suspend fun recordQuizAttempt(attempt: QuizAttemptEntity) {
        studyDao.insertAttempt(attempt)
        studyDao.updateDeckLastPracticed(attempt.deckId, attempt.completedAt)
    }
}
