package com.example

import com.example.domain.QuestionMode
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
}
