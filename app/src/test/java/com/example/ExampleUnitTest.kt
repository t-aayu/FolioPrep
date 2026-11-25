package com.example

import com.example.data.local.McqQuestionEntity
import com.example.data.local.SubjectiveCardEntity
import com.example.data.local.serializeKeyPoints
import com.example.domain.QuestionMode
import com.example.domain.QuestionShareHelper
import com.example.domain.TextChunker
import com.example.domain.UpscPromptBuilder
import com.example.domain.UpscSubjects
import com.example.viewmodel.LibraryCategory
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
        assertTrue(promptBlankFallback.startsWith("# SKILL: UPSC CSE PRELIMS"))
        assertTrue(promptBlankFallback.contains(UpscPromptBuilder.DEFAULT_SKILL_PROMPT))
    }

    @Test
    fun mode_specific_skills_include_prelims_mains_and_integrated_both_sections() {
        val prelimsPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Ramsar wetlands conservation and Mangroves.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 7,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Environment",
            difficulty = com.example.domain.QuestionDifficulty.HARD
        )
        assertTrue(prelimsPrompt.startsWith("# SKILL: UPSC CSE PRELIMS"))
        assertTrue(prelimsPrompt.contains("Total MCQs required: 7"))
        assertTrue(prelimsPrompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Hard"))
        assertTrue(prelimsPrompt.contains("Only, All, Always, Generally, Exclusively, Necessarily, May, Can, Primarily"))
        assertTrue(prelimsPrompt.contains("Why other options are incorrect"))
        assertFalse(prelimsPrompt.contains("examiner", ignoreCase = true))
        assertTrue(UpscPromptBuilder.isGeneratedPrompt(prelimsPrompt))

        val mainsPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Cooperative federalism and GST Council dispute resolution.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.SUBJECTIVE,
            mcqCount = 0,
            mains10Count = 2,
            mains15Count = 1,
            subjectTag = "Polity and Governance",
            difficulty = com.example.domain.QuestionDifficulty.MEDIUM
        )
        assertTrue(mainsPrompt.startsWith("# SKILL: UPSC CSE MAINS"))
        assertTrue(mainsPrompt.contains("2 question(s) of 10 Marks / 150 Words and 1 question(s) of 15 Marks / 250 Words"))
        assertTrue(mainsPrompt.contains("Critically examine"))
        assertTrue(mainsPrompt.contains("GS Paper IV"))
        assertFalse(mainsPrompt.contains("examiner", ignoreCase = true))
        assertTrue(UpscPromptBuilder.isGeneratedPrompt(mainsPrompt))

        val bothPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Digital Public Infrastructure and AI governance.",
            latestPrelimsYear = 2027,
            latestMainsYear = 2026,
            questionMode = QuestionMode.BOTH,
            mcqCount = 4,
            mains10Count = 1,
            mains15Count = 1,
            subjectTag = "Sci-tech",
            difficulty = com.example.domain.QuestionDifficulty.EASY
        )
        assertTrue(bothPrompt.startsWith("# SKILL: UPSC CSE PRELIMS & MAINS"))
        assertTrue(bothPrompt.contains("PRELIMS MCQ STANDARDS (GS PAPER I — 4 MCQs)"))
        assertTrue(bothPrompt.contains("MAINS DESCRIPTIVE STANDARDS (GS PAPERS I–IV — 1×10M, 1×15M)"))
        assertTrue(bothPrompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Easy"))
        assertFalse(bothPrompt.contains("examiner", ignoreCase = true))
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

    @Test
    fun share_helper_formats_mains_flashcard_10_and_15_marker() {
        val card10 = SubjectiveCardEntity(
            deckId = 1L,
            question = "Examine the role of the Finance Commission in strengthening cooperative federalism.",
            marks = 10,
            intro = "Article 280 establishes the Finance Commission as a quasi-judicial body for vertical and horizontal devolution.",
            pointsSerialized = serializeKeyPoints(
                listOf(
                    "Balances fiscal capacity with equity criteria across states.",
                    "Encourages performance-linked grants for local bodies."
                )
            ),
            conclusion = "Institutionalizing permanent technical support can further deepen fiscal federalism.",
            orderIndex = 0
        )
        val formatted10 = QuestionShareHelper.formatSubjectiveFlashcardText(1, card10)
        assertTrue(formatted10.contains("10 Marks · 150 Words"))
        assertTrue(formatted10.contains("Examine the role of the Finance Commission"))
        assertTrue(formatted10.contains("1. Introduction:"))
        assertTrue(formatted10.contains("2. Core Arguments & Dimensions:"))
        assertTrue(formatted10.contains("1. Balances fiscal capacity with equity criteria across states."))
        assertTrue(formatted10.contains("2. Encourages performance-linked grants for local bodies."))
        assertTrue(formatted10.contains("3. Conclusion / Way Forward:"))

        val card15 = card10.copy(marks = 15)
        val formatted15 = QuestionShareHelper.formatSubjectiveFlashcardText(2, card15)
        assertTrue(formatted15.contains("Q2 (15 Marks · 250 Words)"))
    }

    @Test
    fun library_categories_include_prelims_mcqs_and_mains() {
        assertEquals(
            listOf("All", "Prelims - MCQs", "Mains"),
            LibraryCategory.entries.map { it.label }
        )
    }

    @Test
    fun mcq_question_types_contain_all_eight_requested_formats() {
        assertEquals(
            listOf(
                "Direct analytical & conceptual MCQs",
                "3-statement questions.",
                "4-statement questions.",
                "Nested 3 and 4 statement type (2026)",
                "Match the following 2 columns and 3 columns",
                "Assertion and reasoning, where appropriate.",
                "Correctly/incorrectly matched pairs.",
                "Multiple-item identification questions"
            ),
            com.example.domain.McqQuestionType.entries.map { it.label }
        )
    }

    @Test
    fun auto_rotate_mcq_type_never_repeats_last_generated_type() {
        com.example.domain.McqQuestionType.entries.forEach { lastType ->
            repeat(20) { seed ->
                val picked = UpscPromptBuilder.pickNextMcqTypeExcluding(
                    lastGeneratedLabel = lastType.label,
                    random = kotlin.random.Random(seed)
                )
                assertFalse(
                    "Picked ${picked.label} must differ from last ${lastType.label}",
                    picked == lastType
                )
            }
        }
        val blankPick = UpscPromptBuilder.pickNextMcqTypeExcluding("", kotlin.random.Random(1))
        assertTrue(com.example.domain.McqQuestionType.entries.contains(blankPick))
    }

    @Test
    fun nested_3_4_statement_2026_and_custom_mcq_type_inject_into_prompt() {
        val nestedPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Governor discretionary powers under Article 163.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 3,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Polity",
            mcqQuestionType = com.example.domain.McqQuestionType.NESTED_3_4_STATEMENT
        )
        assertTrue(nestedPrompt.contains("Nested 3 and 4 statement type (2026)"))
        assertTrue(nestedPrompt.contains("3 or 4 numbered primary statements"))
        assertTrue(nestedPrompt.contains("3 or 4 numbered inferences"))
        assertTrue(nestedPrompt.contains("How many of the above inferences are true?"))

        val customPrompt = UpscPromptBuilder.buildPrompt(
            sourceText = "Governor discretionary powers under Article 163.",
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            questionMode = QuestionMode.MCQ,
            mcqCount = 3,
            mains10Count = 0,
            mains15Count = 0,
            subjectTag = "Polity",
            mcqQuestionType = com.example.domain.McqQuestionType.THREE_STATEMENT,
            customMcqQuestionType = "Chronological ordering of Constitutional Amendments"
        )
        assertTrue(customPrompt.contains("Chronological ordering of Constitutional Amendments"))
        assertFalse(customPrompt.contains("REQUIRED MCQ QUESTION TYPE: 3-statement questions."))
    }

    @Test
    fun quick_ask_prompt_generates_single_mode_mcq_or_mains_with_default_count_and_json_schema() {
        val mcqPrompt = UpscPromptBuilder.buildQuickAskPrompt(
            keywords = "Article 356, Sarkaria Commission, President's Rule",
            questionType = com.example.domain.QuickAskQuestionType.MCQ,
            questionCount = 1,
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            difficulty = com.example.domain.QuestionDifficulty.HARD,
            mcqQuestionType = com.example.domain.McqQuestionType.THREE_STATEMENT
        )
        assertTrue(mcqPrompt.startsWith("# SKILL: UPSC CSE PRELIMS"))
        assertTrue(mcqPrompt.contains("Total MCQs required: 1"))
        assertTrue(mcqPrompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Hard"))
        assertTrue(mcqPrompt.contains("\"difficulty\": \"Hard\""))
        assertTrue(mcqPrompt.contains("TOPIC KEYWORDS: Article 356, Sarkaria Commission, President's Rule"))
        assertTrue(mcqPrompt.contains("```json"))
        assertTrue(mcqPrompt.contains("\"mcqs\":"))
        assertFalse(mcqPrompt.contains("\"subjective\":"))

        val mains10Prompt = UpscPromptBuilder.buildQuickAskPrompt(
            keywords = "Fiscal federalism, 16th Finance Commission",
            questionType = com.example.domain.QuickAskQuestionType.MAINS_10,
            questionCount = 2,
            latestPrelimsYear = 2026,
            latestMainsYear = 2026,
            difficulty = com.example.domain.QuestionDifficulty.EASY
        )
        assertTrue(mains10Prompt.startsWith("# SKILL: UPSC CSE MAINS"))
        assertTrue(mains10Prompt.contains("2 question(s) of 10 Marks / 150 Words and 0 question(s) of 15 Marks / 250 Words"))
        assertTrue(mains10Prompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Easy"))
        assertTrue(mains10Prompt.contains("TOPIC KEYWORDS: Fiscal federalism, 16th Finance Commission"))
        assertTrue(mains10Prompt.contains("\"subjective\":"))
        assertFalse(mains10Prompt.contains("\"mcqs\":"))

        val mains15Prompt = UpscPromptBuilder.buildQuickAskPrompt(
            keywords = "Ramsar wetlands, mangrove restoration",
            questionType = com.example.domain.QuickAskQuestionType.MAINS_15,
            questionCount = 1,
            latestPrelimsYear = 2026,
            latestMainsYear = 2026
        )
        assertTrue(mains15Prompt.startsWith("# SKILL: UPSC CSE MAINS"))
        assertTrue(mains15Prompt.contains("0 question(s) of 10 Marks / 150 Words and 1 question(s) of 15 Marks / 250 Words"))
        assertTrue(mains15Prompt.contains("TARGET HARDNESS / DIFFICULTY LEVEL: Medium"))
        assertTrue(mains15Prompt.contains("TOPIC KEYWORDS: Ramsar wetlands, mangrove restoration"))
        assertTrue(mains15Prompt.contains("\"subjective\":"))
        assertFalse(mains15Prompt.contains("\"mcqs\":"))
    }

    @Test
    fun library_pagination_slices_5_for_subheadings_and_10_for_search_with_prev_next() {
        val decks = (1..13).map { "Deck $it" }

        assertEquals(5, com.example.domain.LibraryPagination.pageSizeFor(""))
        assertEquals(5, com.example.domain.LibraryPagination.pageSizeFor("   "))
        assertEquals(10, com.example.domain.LibraryPagination.pageSizeFor("Polity"))

        // Browse mode: 5 per page
        val browsePage0 = com.example.domain.LibraryPagination.slice(decks, requestedPage = 0, pageSize = 5)
        assertEquals(listOf("Deck 1", "Deck 2", "Deck 3", "Deck 4", "Deck 5"), browsePage0.items)
        assertEquals(1, browsePage0.startItemNumber)
        assertEquals(5, browsePage0.endItemNumber)
        assertEquals(3, browsePage0.totalPages)
        assertFalse(browsePage0.hasPrevious)
        assertTrue(browsePage0.hasNext)
        assertEquals(5, browsePage0.nextCount)

        val browsePage1 = com.example.domain.LibraryPagination.slice(decks, requestedPage = 1, pageSize = 5)
        assertEquals(listOf("Deck 6", "Deck 7", "Deck 8", "Deck 9", "Deck 10"), browsePage1.items)
        assertEquals(6, browsePage1.startItemNumber)
        assertEquals(10, browsePage1.endItemNumber)
        assertTrue(browsePage1.hasPrevious)
        assertEquals(5, browsePage1.previousCount)
        assertTrue(browsePage1.hasNext)
        assertEquals(3, browsePage1.nextCount)

        val browsePage2 = com.example.domain.LibraryPagination.slice(decks, requestedPage = 2, pageSize = 5)
        assertEquals(listOf("Deck 11", "Deck 12", "Deck 13"), browsePage2.items)
        assertTrue(browsePage2.hasPrevious)
        assertEquals(5, browsePage2.previousCount)
        assertFalse(browsePage2.hasNext)
        assertEquals(0, browsePage2.nextCount)

        // Out-of-bounds clamping (e.g., after deleting last item on final page)
        val clamped = com.example.domain.LibraryPagination.slice(decks.take(5), requestedPage = 2, pageSize = 5)
        assertEquals(0, clamped.pageIndex)
        assertFalse(clamped.isPaginated)

        // Search mode: 10 per page
        val searchItems = (1..24).map { "Result $it" }
        val searchPage0 = com.example.domain.LibraryPagination.slice(searchItems, requestedPage = 0, pageSize = 10)
        assertEquals(10, searchPage0.items.size)
        assertEquals("Result 1", searchPage0.items.first())
        assertEquals("Result 10", searchPage0.items.last())
        assertFalse(searchPage0.hasPrevious)
        assertTrue(searchPage0.hasNext)
        assertEquals(10, searchPage0.nextCount)

        val searchPage1 = com.example.domain.LibraryPagination.slice(searchItems, requestedPage = 1, pageSize = 10)
        assertEquals(10, searchPage1.items.size)
        assertEquals("Result 11", searchPage1.items.first())
        assertEquals("Result 20", searchPage1.items.last())
        assertTrue(searchPage1.hasPrevious)
        assertEquals(10, searchPage1.previousCount)
        assertTrue(searchPage1.hasNext)
        assertEquals(4, searchPage1.nextCount)
    }

    @Test
    fun deck_list_sorts_by_latest_revised_and_unrevised_by_creation_time() {
        fun sampleDeck(id: Long, createdAt: Long, lastPracticedAt: Long? = null) =
            com.example.data.local.DeckSummary(
                id = id,
                title = "Deck $id",
                examScope = "UPSC CSE",
                prelimsYear = 2026,
                mainsYear = 2026,
                tagsSerialized = "Polity",
                sourceType = "TEXT",
                createdAt = createdAt,
                lastPracticedAt = lastPracticedAt,
                mcqCount = 1,
                subjectiveCount = 0,
                masteredSubjectiveCount = 0,
                bestAccuracyPercent = null
            )

        val unsorted = listOf(
            sampleDeck(id = 1L, createdAt = 1_000L, lastPracticedAt = 9_999L), // revised latest -> 1st
            sampleDeck(id = 3L, createdAt = 6_000L, lastPracticedAt = null),   // unrevised newer -> 2nd
            sampleDeck(id = 2L, createdAt = 2_000L, lastPracticedAt = 5_000L), // revised earlier -> 3rd
            sampleDeck(id = 4L, createdAt = 3_000L, lastPracticedAt = null)    // unrevised older -> 4th
        )

        val sorted = unsorted.sortedWith(
            compareByDescending<com.example.data.local.DeckSummary> { it.lastPracticedAt ?: it.createdAt }
                .thenByDescending { it.createdAt }
                .thenByDescending { it.id }
        )

        assertEquals(listOf(1L, 3L, 2L, 4L), sorted.map { it.id })
    }
}


