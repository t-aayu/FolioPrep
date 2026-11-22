package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    @Query(
        """
        SELECT 
            d.id,
            d.title,
            d.exam_scope,
            d.prelims_year,
            d.mains_year,
            d.tags_serialized,
            d.source_type,
            d.created_at,
            d.last_practiced_at,
            (SELECT COUNT(*) FROM mcq_questions m WHERE m.deck_id = d.id) AS mcq_count,
            (SELECT COUNT(*) FROM subjective_cards s WHERE s.deck_id = d.id) AS subjective_count,
            (SELECT COUNT(*) FROM subjective_cards s WHERE s.deck_id = d.id AND s.is_mastered = 1) AS mastered_subjective_count,
            (SELECT MAX(CAST((a.correct_count * 100.0) / CASE WHEN a.total_questions > 0 THEN a.total_questions ELSE 1 END AS INTEGER))
             FROM quiz_attempts a WHERE a.deck_id = d.id) AS best_accuracy_percent
        FROM study_decks d
        ORDER BY COALESCE(d.last_practiced_at, d.created_at) DESC
        """
    )
    fun observeDeckSummaries(): Flow<List<DeckSummary>>

    @Query("SELECT * FROM study_decks WHERE id = :deckId")
    fun observeDeck(deckId: Long): Flow<StudyDeckEntity?>

    @Query("SELECT * FROM mcq_questions WHERE deck_id = :deckId ORDER BY order_index ASC, id ASC")
    fun observeMcqsForDeck(deckId: Long): Flow<List<McqQuestionEntity>>

    @Query("SELECT * FROM subjective_cards WHERE deck_id = :deckId ORDER BY order_index ASC, id ASC")
    fun observeSubjectiveForDeck(deckId: Long): Flow<List<SubjectiveCardEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE deck_id = :deckId ORDER BY completed_at DESC")
    fun observeAttemptsForDeck(deckId: Long): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: StudyDeckEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMcqs(mcqs: List<McqQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjectiveCards(cards: List<SubjectiveCardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity): Long

    @Update
    suspend fun updateDeck(deck: StudyDeckEntity)

    @Query("UPDATE study_decks SET last_practiced_at = :timestamp WHERE id = :deckId")
    suspend fun updateDeckLastPracticed(deckId: Long, timestamp: Long)

    @Query("UPDATE subjective_cards SET is_mastered = :isMastered WHERE id = :cardId")
    suspend fun updateSubjectiveMastery(cardId: Long, isMastered: Boolean)

    @Query("DELETE FROM study_decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)

    @Transaction
    suspend fun saveFullDeck(
        deck: StudyDeckEntity,
        mcqs: List<McqQuestionEntity>,
        subjectiveCards: List<SubjectiveCardEntity>
    ): Long {
        val deckId = insertDeck(deck)
        if (mcqs.isNotEmpty()) {
            insertMcqs(mcqs.mapIndexed { idx, item -> item.copy(id = 0L, deckId = deckId, orderIndex = idx) })
        }
        if (subjectiveCards.isNotEmpty()) {
            insertSubjectiveCards(
                subjectiveCards.mapIndexed { idx, item -> item.copy(id = 0L, deckId = deckId, orderIndex = idx) }
            )
        }
        return deckId
    }
}
