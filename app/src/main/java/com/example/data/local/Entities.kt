package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "study_decks")
data class StudyDeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "exam_scope") val examScope: String = "UPSC CSE",
    @ColumnInfo(name = "prelims_year") val prelimsYear: Int = 2026,
    @ColumnInfo(name = "mains_year") val mainsYear: Int = 2026,
    @ColumnInfo(name = "tags_serialized") val tagsSerialized: String = "",
    @ColumnInfo(name = "source_type") val sourceType: String = "TEXT",
    @ColumnInfo(name = "source_snippet") val sourceSnippet: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_practiced_at") val lastPracticedAt: Long? = null
) {
    fun tagList(): List<String> = tagsSerialized
        .split("|")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

@Entity(
    tableName = "mcq_questions",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deck_id")]
)
data class McqQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    val question: String,
    @ColumnInfo(name = "option_a") val optionA: String,
    @ColumnInfo(name = "option_b") val optionB: String,
    @ColumnInfo(name = "option_c") val optionC: String,
    @ColumnInfo(name = "option_d") val optionD: String,
    @ColumnInfo(name = "correct_index") val correctIndex: Int,
    val explanation: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
    @ColumnInfo(name = "difficulty") val difficulty: String = "Medium"
) {
    fun optionsList(): List<String> = listOf(optionA, optionB, optionC, optionD)
}

@Entity(
    tableName = "subjective_cards",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deck_id")]
)
data class SubjectiveCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    val question: String,
    val marks: Int = 10,
    val intro: String,
    @ColumnInfo(name = "points_serialized") val pointsSerialized: String,
    val conclusion: String,
    @ColumnInfo(name = "is_mastered") val isMastered: Boolean = false,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0
) {
    fun keyPointsList(): List<String> = pointsSerialized
        .split("\n|||\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deck_id")]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    val mode: String, // "PRACTICE" or "TIMED_EXAM"
    @ColumnInfo(name = "total_questions") val totalQuestions: Int,
    @ColumnInfo(name = "correct_count") val correctCount: Int,
    @ColumnInfo(name = "wrong_count") val wrongCount: Int,
    @ColumnInfo(name = "skipped_count") val skippedCount: Int,
    @ColumnInfo(name = "negative_factor") val negativeFactor: Double,
    @ColumnInfo(name = "net_score") val netScore: Double,
    @ColumnInfo(name = "max_score") val maxScore: Double,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "completed_at") val completedAt: Long = System.currentTimeMillis()
)

data class DeckSummary(
    val id: Long,
    val title: String,
    @ColumnInfo(name = "exam_scope") val examScope: String,
    @ColumnInfo(name = "prelims_year") val prelimsYear: Int,
    @ColumnInfo(name = "mains_year") val mainsYear: Int,
    @ColumnInfo(name = "tags_serialized") val tagsSerialized: String,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_practiced_at") val lastPracticedAt: Long?,
    @ColumnInfo(name = "mcq_count") val mcqCount: Int,
    @ColumnInfo(name = "subjective_count") val subjectiveCount: Int,
    @ColumnInfo(name = "mastered_subjective_count") val masteredSubjectiveCount: Int,
    @ColumnInfo(name = "best_accuracy_percent") val bestAccuracyPercent: Int?
) {
    fun tagList(): List<String> = tagsSerialized
        .split("|")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

data class DeckDetailBundle(
    val deck: StudyDeckEntity,
    val mcqs: List<McqQuestionEntity>,
    val subjectiveCards: List<SubjectiveCardEntity>,
    val attempts: List<QuizAttemptEntity>
)

fun serializeTags(tags: List<String>): String =
    tags.map { it.trim().replace("|", "") }.filter { it.isNotEmpty() }.distinct().joinToString("|")

fun serializeKeyPoints(points: List<String>): String =
    points.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n|||\n")
