package com.example

import com.example.data.local.McqQuestionEntity
import com.example.domain.QuestionMode
import com.example.domain.QuestionShareHelper
import com.example.domain.TextChunker
import com.example.domain.UpscPromptBuilder
import com.example.domain.UpscSubjects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun prompt_includes_upsc_years_fenced_json_and_syllabus_correlation() {
        val prompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Supreme Court ruling on Article 21 and forest conservation.",
            latestPrelimsYear = 2027,
            latestMainsYear = 2026,
            questionMode = QuestionMode.BOTH,
            mcqCount = 5,
            mains10Count = 1,
            mains15Count = 1,
            subjectTag = "Environment"
        )
        assertTrue(prompt.contains("Question Setter"))
        assertTrue(prompt.contains("university professor"))
        assertFalse(prompt.contains("examiner", ignoreCase = true))
        assertTrue(prompt.contains("UPSC CSE Prelims 2027"))
        assertTrue(prompt.contains("UPSC CSE Mains 2026"))
        assertTrue(prompt.contains("```json"))
        assertTrue(prompt.contains("  \"title\": \"Concise UPSC Topic Title\""))
        assertFalse(prompt.contains("single line", ignoreCase = true))
        assertTrue(prompt.contains("SYLLABUS & PYQ CORRELATION"))
    }

    @Test
    fun custom_skill_overrides_default_persona_when_enabled() {
        val customPersona = "Act as a senior professor of Constitutional Law and Public Policy at a central university."
        val promptWithCustom = UpscPromptBuilder.buildPrompt(
            sourceText = "Finance Commission devolution formula.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 5,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Polity",
            useCustomSkill = true,
            customSkillText = customPersona
        )
        assertTrue(promptWithCustom.startsWith(customPersona))
        assertFalse(promptWithCustom.contains(UpscPromptBuilder.DEFAULT_SKILL_PROMPT))

        val promptBlankFallback = UpscPromptBuilder.buildPrompt(
            sourceText = "Finance Commission devolution formula.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 5,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Polity",
            useCustomSkill = true,
            customSkillText = "   "
        )
        assertTrue(promptBlankFallback.startsWith(UpscPromptBuilder.DEFAULT_SKILL_PROMPT))
    }

    @Test
    fun subject_tags_match_question_mode() {
        assertEquals(
            listOf(
                "Polity",
                "Economy",
                "Environment",
                "History",
                "Geography",
                "Sci-Tech",
                "IR",
                "Government Schemes",
                "Current Affairs"
            ),
            UpscSubjects.tagsForMode(QuestionMode.MCQ)
        )
        assertEquals(
            listOf(
                "Geography",
                "Indian History",
                "World History",
                "Indian Society",
                "Polity and Governance",
                "IR",
                "Social Justice",
                "Economy",
                "Internal Security",
                "Sci-Tech",
                "Environment Biodiversity",
                "Disaster Management",
                "Ethics integrity and aptitude"
            ),
            UpscSubjects.tagsForMode(QuestionMode.SUBJECTIVE)
        )
        assertEquals(
            listOf(
                "History",
                "Geography",
                "Polity",
                "IR",
                "Economy",
                "Sci-tech",
                "Environment"
            ),
            UpscSubjects.tagsForMode(QuestionMode.BOTH)
        )
    }

    @Test
    fun chunker_splits_long_article_safely() {
        val longText = List(40) { idx ->
            "Paragraph $idx: " + List(80) { "constitution" }.joinToString(" ")
        }.joinToString("\n\n")
        val chunks = TextChunker.chunkText(longText)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.wordCount <= TextChunker.MAX_WORDS_PER_CHUNK })
    }

    @Test
    fun share_helper_formats_question_and_all_four_options() {
        val mcq = McqQuestionEntity(
            deckId = 1L,
            question = "Consider the following statements:\n1. Statement A.\n2. Statement B.",
            optionA = "1 only",
            optionB = "2 only",
            optionC = "Both 1 and 2",
            optionD = "Neither 1 nor 2",
            correctIndex = 2,
            explanation = "Both are correct.",
            orderIndex = 0
        )
        val formatted = QuestionShareHelper.formatQuestionWithOptions(3, mcq)
        assertTrue(formatted.startsWith("Q3. Consider the following statements:\n1. Statement A.\n2. Statement B."))
        assertTrue(formatted.contains("A. 1 only"))
        assertTrue(formatted.contains("B. 2 only"))
        assertTrue(formatted.contains("C. Both 1 and 2"))
        assertTrue(formatted.contains("D. Neither 1 nor 2"))
    }

    @Test
    fun prompt_defaults_to_medium_difficulty_and_requests_difficulty_field_in_json() {
        val defaultPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Monetary Policy Committee inflation targeting.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 5,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Economy"
        )
        assertTrue(defaultPrompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Medium"))
        assertTrue(defaultPrompt.contains("\"difficulty\": \"Medium\""))
        assertTrue(defaultPrompt.contains("include \"difficulty\" set to \"Easy\", \"Medium\", or \"Hard\""))

        val hardPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Monetary Policy Committee inflation targeting.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 5,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Economy",
            difficulty = com.example.domain.QuestionDifficulty.HARD
        )
        assertTrue(hardPrompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Hard"))
        assertTrue(hardPrompt.contains("\"difficulty\": \"Hard\""))
    }

    @Test
    fun difficulty_normalization_handles_variants_and_defaults_to_medium() {
        assertEquals(
            com.example.domain.QuestionDifficulty.EASY,
            com.example.domain.QuestionDifficulty.fromRaw("easy")
        )
        assertEquals(
            com.example.domain.QuestionDifficulty.EASY,
            com.example.domain.QuestionDifficulty.fromRaw("Basic / Introductory")
        )
        assertEquals(
            com.example.domain.QuestionDifficulty.HARD,
            com.example.domain.QuestionDifficulty.fromRaw("HARD")
        )
        assertEquals(
            com.example.domain.QuestionDifficulty.HARD,
            com.example.domain.QuestionDifficulty.fromRaw("Tough / Analytical")
        )
        assertEquals(
            com.example.domain.QuestionDifficulty.MEDIUM,
            com.example.domain.QuestionDifficulty.fromRaw("Moderate")
        )
        assertEquals(
            com.example.domain.QuestionDifficulty.MEDIUM,
            com.example.domain.QuestionDifficulty.fromRaw(null)
        )
        assertEquals(
            "Documents/FolioPrep/folioprep_backup.json",
            com.example.data.LocalBackupManager.defaultBackupDisplayPath()
        )
    }

    @Test
    fun share_helper_formats_explanation_with_correct_answer() {
        val mcq = McqQuestionEntity(
            deckId = 1L,
            question = "Consider the following statements about Finance Commission.",
            optionA = "1 only",
            optionB = "2 only",
            optionC = "Both 1 and 2",
            optionD = "Neither 1 nor 2",
            correctIndex = 1,
            explanation = "Article 280 constitutes the Finance Commission.\nStatement 2 accurately describes its mandate.",
            orderIndex = 0
        )
        val formatted = QuestionShareHelper.formatExplanationText(4, mcq)
        assertTrue(formatted.contains("Q4 Explanation"))
        assertTrue(formatted.contains("Correct Answer: B. 2 only"))
        assertTrue(formatted.contains("Article 280 constitutes the Finance Commission."))
        assertTrue(formatted.contains("Statement 2 accurately describes its mandate."))
    }
}
