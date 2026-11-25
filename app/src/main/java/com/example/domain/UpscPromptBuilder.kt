package com.example.domain

enum class QuestionMode(val label: String) {
    MCQ("Prelims MCQ"),
    SUBJECTIVE("Mains 10/15M"),
    BOTH("Both")
}

enum class QuickAskQuestionType(val label: String, val helper: String) {
    MCQ("MCQ", "Prelims MCQs"),
    MAINS_10("Mains 10M", "10 Marks · 150 words"),
    MAINS_15("Mains 15M", "15 Marks · 250 words")
}

enum class QuestionDifficulty(
    val label: String,
    val jsonValue: String,
    val description: String
) {
    EASY("Easy", "Easy", "Foundational NCERT & direct conceptual clarity"),
    MEDIUM("Medium", "Medium", "Standard UPSC CSE analytical & multi-statement level"),
    HARD("Hard", "Hard", "Elimination-resistant high-order UPSC CSE difficulty");

    companion object {
        fun fromRaw(raw: String?): QuestionDifficulty {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when {
                normalized.startsWith("easy") || normalized.startsWith("basic") || normalized == "low" -> EASY
                normalized.startsWith("hard") || normalized.startsWith("tough") || normalized.startsWith("difficult") || normalized == "high" -> HARD
                else -> MEDIUM
            }
        }
    }
}

enum class McqQuestionType(
    val label: String,
    val shortDescription: String,
    val promptInstruction: String
) {
    DIRECT_ANALYTICAL(
        label = "Direct analytical & conceptual MCQs",
        shortDescription = "Conceptual, principle & application-based MCQs",
        promptInstruction = "Frame direct analytical and conceptual UPSC Prelims MCQs testing fundamental principles, institutional/economic mechanisms, exceptions, and application—without multi-statement counting scaffolding."
    ),
    THREE_STATEMENT(
        label = "3-statement questions.",
        shortDescription = "Three numbered statements evaluated independently",
        promptInstruction = "Frame 3-statement UPSC Prelims MCQs: present exactly 3 numbered statements (1., 2., 3.) for independent evaluation, concluding with either 'Which of the statements given above is/are correct?' or 'How many of the statements given above are correct?'."
    ),
    FOUR_STATEMENT(
        label = "4-statement questions.",
        shortDescription = "Four numbered statements with counting or combination options",
        promptInstruction = "Frame 4-statement UPSC Prelims MCQs: present exactly 4 numbered statements (1., 2., 3., 4.) for independent evaluation, concluding with 'How many of the statements given above are correct?' or 'Which of the statements given above is/are correct?'."
    ),
    NESTED_3_4_STATEMENT(
        label = "Nested 3 and 4 statement type (2026)",
        shortDescription = "3/4 statements followed by 3/4 derived inferences to evaluate",
        promptInstruction = "Frame Nested 3- and 4-statement type (2026) UPSC Prelims MCQs: first present 3 or 4 numbered primary statements ('Statements:\n1. ...\n2. ...\n3. ...'), then present 3 or 4 numbered inferences ('Inferences:\nI. ...\nII. ...\nIII. ...') where each inference is based on one or more of the statements given above, and conclude by asking which or how many of the above inferences are true."
    ),
    MATCH_2_3_COLUMNS(
        label = "Match the following 2 columns and 3 columns",
        shortDescription = "2-column or 3-column matching matrix questions",
        promptInstruction = "Frame Match-the-Following UPSC Prelims MCQs using either 2 columns (List I – List II) or 3 columns (Column I – Column II – Column III) with cleanly aligned rows in \"q\" and matching code combinations in \"opts\"."
    ),
    ASSERTION_REASONING(
        label = "Assertion and reasoning, where appropriate.",
        shortDescription = "Statement-I & Statement-II causal explanation format",
        promptInstruction = "Frame UPSC Statement-I / Statement-II (Assertion and Reasoning) MCQs testing causal or conceptual relationships, using standard UPSC options: Both Statement-I and Statement-II are correct and Statement-II is the correct explanation for Statement-I; Both are correct and Statement-II is not the correct explanation; Statement-I is correct but Statement-II is incorrect; Statement-I is incorrect but Statement-II is correct."
    ),
    CORRECT_INCORRECT_PAIRS(
        label = "Correctly/incorrectly matched pairs.",
        shortDescription = "Evaluate 3 or 4 pairs for correct or incorrect matches",
        promptInstruction = "Frame Correctly / Incorrectly Matched Pairs UPSC Prelims MCQs presenting 3 or 4 pairs (e.g., Pair 1: Item — Attribute) and asking how many or which of the pairs given above are correctly (or incorrectly) matched."
    ),
    MULTIPLE_ITEM_ID(
        label = "Multiple-item identification questions",
        shortDescription = "Identify which or how many listed items fit a criterion",
        promptInstruction = "Frame Multiple-Item Identification UPSC Prelims MCQs listing 3 to 5 specific items (species, bodies, sites, treaties, rivers, instruments, etc.) and asking 'How many of the above...' or 'Which of the above...' satisfy a precise UPSC criterion."
    );
}

sealed class McqTypeSelection {
    data object AutoRotate : McqTypeSelection()
    data class Preset(val type: McqQuestionType) : McqTypeSelection()
    data object Custom : McqTypeSelection()
}

object UpscSubjects {
    val mcqTags: List<String> = listOf(
        "Polity",
        "Economy",
        "Environment",
        "History",
        "Geography",
        "Sci-Tech",
        "IR",
        "Government Schemes",
        "Current Affairs"
    )

    val mainsTags: List<String> = listOf(
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
    )

    val bothTags: List<String> = listOf(
        "History",
        "Geography",
        "Polity",
        "IR",
        "Economy",
        "Sci-tech",
        "Environment"
    )

    fun tagsForMode(mode: QuestionMode): List<String> = when (mode) {
        QuestionMode.MCQ -> mcqTags
        QuestionMode.SUBJECTIVE -> mainsTags
        QuestionMode.BOTH -> bothTags
    }

    val allTags: List<String> = (mcqTags + mainsTags + bothTags).distinctBy { it.lowercase() }
}

object UpscPromptBuilder {

    const val DEFAULT_SKILL_PROMPT: String =
        "Act strictly as a senior UPSC Civil Services Examination (CSE) Question Setter — an experienced university professor and subject specialist who drafts official UPSC CSE Prelims (GS Paper I) and Mains (GS Papers I–IV) question papers."

    fun isGeneratedPrompt(raw: String, customSkillText: String = ""): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        val custom = customSkillText.trim()
        return trimmed.startsWith("# SKILL: UPSC CSE", ignoreCase = true) ||
            trimmed.startsWith("Act strictly as a senior UPSC", ignoreCase = true) ||
            trimmed.startsWith(DEFAULT_SKILL_PROMPT, ignoreCase = true) ||
            (custom.isNotEmpty() && trimmed.startsWith(custom, ignoreCase = true))
    }

    fun pickNextMcqTypeExcluding(
        lastGeneratedLabel: String?,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): McqQuestionType {
        val last = lastGeneratedLabel?.trim().orEmpty()
        val candidates = McqQuestionType.entries.filterNot {
            it.label.equals(last, ignoreCase = true)
        }.ifEmpty { McqQuestionType.entries }
        return candidates[random.nextInt(candidates.size)]
    }

    fun buildQuickAskPrompt(
        keywords: String,
        questionType: QuickAskQuestionType,
        questionCount: Int,
        latestPrelimsYear: Int,
        latestMainsYear: Int,
        mcqQuestionType: McqQuestionType? = null,
        useCustomSkill: Boolean = false,
        customSkillText: String = ""
    ): String {
        val safeCount = questionCount.coerceAtLeast(1)
        val cleanKeywords = keywords.trim()
        val topicAnchor = if (cleanKeywords.isNotEmpty()) {
            "TOPIC KEYWORDS: $cleanKeywords"
        } else {
            ""
        }

        return when (questionType) {
            QuickAskQuestionType.MCQ -> buildPrompt(
                sourceText = topicAnchor,
                latestPrelimsYear = latestPrelimsYear,
                latestMainsYear = latestMainsYear,
                questionMode = QuestionMode.MCQ,
                mcqCount = safeCount,
                mains10Count = 0,
                mains15Count = 0,
                subjectTag = null,
                difficulty = QuestionDifficulty.MEDIUM,
                mcqQuestionType = mcqQuestionType,
                customMcqQuestionType = "",
                useCustomSkill = useCustomSkill,
                customSkillText = customSkillText
            )

            QuickAskQuestionType.MAINS_10 -> buildPrompt(
                sourceText = topicAnchor,
                latestPrelimsYear = latestPrelimsYear,
                latestMainsYear = latestMainsYear,
                questionMode = QuestionMode.SUBJECTIVE,
                mcqCount = 0,
                mains10Count = safeCount,
                mains15Count = 0,
                subjectTag = null,
                difficulty = QuestionDifficulty.MEDIUM,
                mcqQuestionType = null,
                customMcqQuestionType = "",
                useCustomSkill = useCustomSkill,
                customSkillText = customSkillText
            )

            QuickAskQuestionType.MAINS_15 -> buildPrompt(
                sourceText = topicAnchor,
                latestPrelimsYear = latestPrelimsYear,
                latestMainsYear = latestMainsYear,
                questionMode = QuestionMode.SUBJECTIVE,
                mcqCount = 0,
                mains10Count = 0,
                mains15Count = safeCount,
                subjectTag = null,
                difficulty = QuestionDifficulty.MEDIUM,
                mcqQuestionType = null,
                customMcqQuestionType = "",
                useCustomSkill = useCustomSkill,
                customSkillText = customSkillText
            )
        }
    }

    fun buildPrompt(
        sourceText: String,
        latestPrelimsYear: Int,
        latestMainsYear: Int,
        questionMode: QuestionMode,
        mcqCount: Int,
        mains10Count: Int,
        mains15Count: Int,
        subjectTag: String?,
        difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
        mcqQuestionType: McqQuestionType? = null,
        customMcqQuestionType: String = "",
        useCustomSkill: Boolean = false,
        customSkillText: String = ""
    ): String {
        val includeMcq = (questionMode == QuestionMode.MCQ || questionMode == QuestionMode.BOTH) && mcqCount > 0
        val includeMains10 = (questionMode == QuestionMode.SUBJECTIVE || questionMode == QuestionMode.BOTH) && mains10Count > 0
        val includeMains15 = (questionMode == QuestionMode.SUBJECTIVE || questionMode == QuestionMode.BOTH) && mains15Count > 0
        val includeSubjective = includeMains10 || includeMains15

        val taskParts = mutableListOf<String>()
        if (includeMcq) {
            taskParts.add("$mcqCount UPSC Prelims MCQ(s)")
        }
        if (includeMains10) {
            taskParts.add("$mains10Count UPSC Mains 10-mark (150 words) subjective question(s)")
        }
        if (includeMains15) {
            taskParts.add("$mains15Count UPSC Mains 15-mark (250 words) subjective question(s)")
        }

        val taskSummary = if (taskParts.isEmpty()) {
            "0 questions (please set MCQ, 10-mark, or 15-mark count above 0 before sharing)"
        } else {
            taskParts.joinToString(" and ")
        }

        val subjectLine = if (!subjectTag.isNullOrBlank()) {
            "SUBJECT / PAPER FOCUS: $subjectTag (UPSC CSE)."
        } else {
            "SUBJECT / PAPER FOCUS: Automatically infer the most appropriate UPSC CSE GS paper, subject(s), and syllabus tags from the supplied source material."
        }

        val mcqTypeRule = buildMcqTypeRule(
            mcqQuestionType = mcqQuestionType,
            customMcqQuestionType = customMcqQuestionType
        )

        val schemaBlock = buildSchemaBlock(
            includeMcq = includeMcq,
            includeSubjective = includeSubjective,
            difficulty = difficulty,
            mcqQuestionType = mcqQuestionType,
            customMcqQuestionType = customMcqQuestionType
        )
        val cleanSource = sourceText.trim().ifEmpty { "[Paste text, import an article link, or scan a photo above]" }
        val customSkillActive = useCustomSkill && customSkillText.isNotBlank()

        if (customSkillActive) {
            return buildCustomSkillPrompt(
                customSkill = customSkillText.trim(),
                taskSummary = taskSummary,
                subjectLine = subjectLine,
                latestPrelimsYear = latestPrelimsYear,
                latestMainsYear = latestMainsYear,
                includeMcq = includeMcq,
                includeSubjective = includeSubjective,
                mains10Count = mains10Count,
                mains15Count = mains15Count,
                difficulty = difficulty,
                mcqTypeRule = mcqTypeRule,
                schemaBlock = schemaBlock,
                cleanSource = cleanSource
            )
        }

        return when (questionMode) {
            QuestionMode.MCQ -> buildPrelimsSkillPrompt(
                taskSummary = taskSummary,
                subjectLine = subjectLine,
                latestPrelimsYear = latestPrelimsYear,
                mcqCount = mcqCount,
                difficulty = difficulty,
                mcqTypeRule = mcqTypeRule,
                schemaBlock = schemaBlock,
                cleanSource = cleanSource
            )

            QuestionMode.SUBJECTIVE -> buildMainsSkillPrompt(
                taskSummary = taskSummary,
                subjectLine = subjectLine,
                latestMainsYear = latestMainsYear,
                mains10Count = mains10Count,
                mains15Count = mains15Count,
                difficulty = difficulty,
                schemaBlock = schemaBlock,
                cleanSource = cleanSource
            )

            QuestionMode.BOTH -> buildIntegratedBothSkillPrompt(
                taskSummary = taskSummary,
                subjectLine = subjectLine,
                latestPrelimsYear = latestPrelimsYear,
                latestMainsYear = latestMainsYear,
                includeMcq = includeMcq,
                includeSubjective = includeSubjective,
                mcqCount = mcqCount,
                mains10Count = mains10Count,
                mains15Count = mains15Count,
                difficulty = difficulty,
                mcqTypeRule = mcqTypeRule,
                schemaBlock = schemaBlock,
                cleanSource = cleanSource
            )
        }
    }

    private fun buildMcqTypeRule(
        mcqQuestionType: McqQuestionType?,
        customMcqQuestionType: String
    ): String? {
        val custom = customMcqQuestionType.trim()
        if (custom.isNotEmpty()) {
            return "REQUIRED MCQ QUESTION TYPE: Strictly frame the generated Prelims MCQs in this requested style/format: $custom."
        }
        if (mcqQuestionType != null) {
            return "REQUIRED MCQ QUESTION TYPE: ${mcqQuestionType.label} — ${mcqQuestionType.promptInstruction}"
        }
        return null
    }

    private fun difficultyGuidanceForPrelims(difficulty: QuestionDifficulty): String = when (difficulty) {
        QuestionDifficulty.EASY ->
            "Calibrate primarily to Easy UPSC Prelims level: test foundational NCERT principles, direct constitutional/statutory basics, and clear conceptual accuracy without trivial giveaways."
        QuestionDifficulty.MEDIUM ->
            "Calibrate primarily to Medium (Standard UPSC CSE Prelims) level: balance conceptual depth, multi-statement independent evaluation, and realistic UPSC distractors."
        QuestionDifficulty.HARD ->
            "Calibrate primarily to Hard (Elimination-Resistant UPSC CSE Prelims) level: require careful independent verification of every statement using subtle conceptual distinctions, exceptions, and nuanced qualifiers—never obscure trivia."
    }

    private fun difficultyGuidanceForMains(difficulty: QuestionDifficulty): String = when (difficulty) {
        QuestionDifficulty.EASY ->
            "Calibrate primarily to Easy UPSC Mains level: clear, direct analytical demands grounded in core static syllabus concepts and straightforward contemporary linkages."
        QuestionDifficulty.MEDIUM ->
            "Calibrate primarily to Medium (Standard UPSC CSE Mains) level: balanced multi-dimensional evaluation linking static syllabus foundations with contemporary policy and institutional issues."
        QuestionDifficulty.HARD ->
            "Calibrate primarily to Hard (High-Order UPSC CSE Mains) level: deep critical evaluation, subtle constitutional/institutional trade-offs, and natural interdisciplinary synthesis across GS papers."
    }

    private fun buildPrelimsSkillPrompt(
        taskSummary: String,
        subjectLine: String,
        latestPrelimsYear: Int,
        mcqCount: Int,
        difficulty: QuestionDifficulty,
        mcqTypeRule: String?,
        schemaBlock: String,
        cleanSource: String
    ): String = buildString {
        appendLine("# SKILL: UPSC CSE PRELIMS – EXPERT QUESTION PAPER SETTER")
        appendLine()
        appendLine("## 1. ROLE AND PERSONA")
        appendLine(DEFAULT_SKILL_PROMPT)
        appendLine("Draft original questions that closely resemble the intellectual depth, framing, conceptual rigour, and difficulty of actual UPSC Civil Services Preliminary Examination (GS Paper I) papers. Think like an official UPSC Question Setter—never like a coaching institute, textbook author, or ordinary quiz generator.")
        appendLine("Every question must test conceptual understanding over rote memorisation, analytical & logical reasoning, subtle conceptual distinctions, interdisciplinary awareness, static-current application, exceptions/qualifications, and elimination skills.")
        appendLine()
        appendLine("## 2. ASSIGNMENT & DIFFICULTY CALIBRATION")
        appendLine("- GENERATE EXACTLY: $taskSummary (Total MCQs required: $mcqCount).")
        appendLine("- $subjectLine")
        if (!mcqTypeRule.isNullOrBlank()) {
            appendLine("- $mcqTypeRule")
        }
        appendLine("- EXAM PATTERN CALIBRATION: Strictly emulate official UPSC CSE Prelims $latestPrelimsYear trends AND all previous UPSC CSE Prelims years up to $latestPrelimsYear.")
        appendLine("- TARGET HARDNESS / DIFFICULTY LEVEL: ${difficulty.label} — ${difficulty.description}.")
        appendLine("- ${difficultyGuidanceForPrelims(difficulty)}")
        appendLine("- Difficulty must arise from conceptual complexity, subtle distinctions, and analytical reasoning rather than obscure or irrelevant trivia.")
        appendLine()
        appendLine("## 3. PRELIMS QUESTION PATTERNS & SUBJECT EXPERTISE (GS PAPER I)")
        appendLine("- GS Paper I Domains: History & Culture (chronology, movements, heritage), Indian Polity & Governance (constitutional/statutory/quasi-judicial distinctions, powers & limitations), Geography (spatial & causal processes, map-based reasoning), Indian Economy (mechanisms, policy implications, indicator relationships), Environment & Ecology (ecosystems, species-habitat, conservation designations, conventions), Science & Technology (scientific principles & applications), and IR & Global Initiatives (mandates, groupings, India's interests).")
        appendLine("- Supported UPSC Question Formats:")
        appendLine("  1. Direct analytical & conceptual MCQs.")
        appendLine("  2. 3-statement and 4-statement questions (including counting 'How many of the statements given above are correct?').")
        appendLine("  3. Nested 3 and 4 statement type (2026): 3/4 primary statements followed by 3/4 derived inferences based on one or more of the statements, asking which or how many of the inferences are true.")
        appendLine("  4. Match-the-following (2 columns and 3 columns) and correctly/incorrectly matched pairs.")
        appendLine("  5. Statement-I / Statement-II assertion and reasoning, and multiple-item identification questions.")
        appendLine("- Qualifiers & Distractor Design: Use authentic qualifiers carefully (Only, All, Always, Generally, Exclusively, Necessarily, May, Can, Primarily). Construct plausible, challenging distractors via one subtle factual/conceptual error, confusing closely related provisions/institutions/economic concepts, incorrect causal links, interchanged habitats/locations, or confusing what an institution can do vs. what it is constitutionally empowered to do. Never use absurd or artificially obvious distractors.")
        appendLine()
        appendLine("## 4. SYLLABUS & PYQ CORRELATION, ACCURACY & EXPLANATIONS")
        appendLine("- SYLLABUS & PYQ CORRELATION: Treat the source below as a current-affairs or conceptual anchor. Identify the central issue, extract underlying static UPSC concepts, and freely integrate verified static syllabus knowledge and interdisciplinary links. Do NOT restrict questions to sentences explicitly in the source, and do NOT invent facts or fabricate PYQs.")
        appendLine("- STRICT ACCURACY: Accuracy takes precedence over difficulty. Verify constitutional articles, statutory provisions, scientific/ecological facts, and economic principles; never manufacture certainty or allow outdated facts.")
        appendLine("- QUALITY CONTROL: Before finalising, internally verify each question has exactly one verifiable correct answer, unambiguous statements, plausible distractors, no repetitive templates, and authentic UPSC GS Paper I relevance.")
        appendLine("- DETAILED EXPLANATION IN \"exp\": For every MCQ, structure \"exp\" clearly with \\n line breaks covering: (1) independent analysis of each statement/inference (citing relevant constitutional articles, statutes, scientific/economic mechanisms, and exceptions), (2) brief elimination logic explaining why other options are incorrect, and (3) UPSC Relevance (GS Paper I syllabus area & underlying concept tested).")
        appendLine()
        appendLine("## 5. OUTPUT FORMAT (STRICT JSON FOR FOLIOPREP)")
        appendLine("- For every MCQ: provide exactly 4 options in \"opts\", set \"ans\" to the 0-based index (0, 1, 2, or 3) of the single correct option, and include \"difficulty\" set to \"Easy\", \"Medium\", or \"Hard\" reflecting the question's actual UPSC CSE hardness level (calibrated primarily around ${difficulty.label}).")
        appendOutputSchemaAndSource(schemaBlock, cleanSource)
    }

    private fun buildMainsSkillPrompt(
        taskSummary: String,
        subjectLine: String,
        latestMainsYear: Int,
        mains10Count: Int,
        mains15Count: Int,
        difficulty: QuestionDifficulty,
        schemaBlock: String,
        cleanSource: String
    ): String = buildString {
        appendLine("# SKILL: UPSC CSE MAINS – EXPERT QUESTION PAPER SETTER")
        appendLine()
        appendLine("## 1. ROLE AND PERSONA")
        appendLine(DEFAULT_SKILL_PROMPT)
        appendLine("You possess deep expertise across UPSC CSE Mains GS Papers I, II, III, IV, Essay, and Optional trends. Generate high-quality descriptive questions that match the language, intellectual depth, analytical rigour, and examination orientation of actual UPSC CSE Mains papers—never generic coaching-institute or newspaper-headline questions.")
        appendLine()
        appendLine("## 2. ASSIGNMENT & DIFFICULTY CALIBRATION")
        appendLine("- GENERATE EXACTLY: $taskSummary ($mains10Count question(s) of 10 Marks / 150 Words and $mains15Count question(s) of 15 Marks / 250 Words).")
        appendLine("- $subjectLine")
        appendLine("- EXAM PATTERN CALIBRATION: Calibrate strictly against official UPSC CSE Mains $latestMainsYear PYQ trends AND all previous UPSC CSE Mains years up to $latestMainsYear.")
        appendLine("- TARGET HARDNESS / DIFFICULTY LEVEL: ${difficulty.label} — ${difficulty.description}.")
        appendLine("- ${difficultyGuidanceForMains(difficulty)}")
        appendLine("- Marks & Word-Limit Depth: 10-mark (150 words) questions must test focused analytical understanding; 15-mark (250 words) questions must demand deeper critical evaluation, multiple dimensions, and well-developed argumentation.")
        appendLine()
        appendLine("## 3. GS PAPER MAPPING, DIRECTIVES & QUESTION FORMULATION")
        appendLine("- Paper Identification (do not force unnatural mappings): GS Paper I (Heritage & Culture, Modern/World History, Indian Society, Geography & Resources); GS Paper II (Constitution, Federalism, Parliament, Judiciary, Governance, Welfare & Social Justice, IR); GS Paper III (Economy, Budgeting, Agriculture, Infrastructure, Sci-Tech, Environment & Biodiversity, Disaster Management, Internal Security); GS Paper IV (Ethics, Human Values, Probity, Emotional Intelligence, Ethical Dilemmas & Administrative Case Studies).")
        appendLine("- Authentic UPSC Directive Words: Select the directive matching the intellectual demand—Discuss, Examine, Critically examine, Analyse, Critically analyse, Evaluate, Elucidate, Explain, Comment, Substantiate, Illustrate, Assess, To what extent, In what ways, How far, Do you agree?")
        appendLine("- Question Quality & Multidimensional Framing: Every question must have a clear central demand, be answerable within its prescribed word limit, use formal concise UPSC wording, and invite balanced multidimensional reasoning (historical evolution, constitutional/institutional provisions, socio-economic & administrative implications, Centre-State relations, regional disparities, environmental sustainability, technology, ethics, reforms/way forward). Avoid purely definitional, overly broad, vague, multi-unrelated-part, or source-copying questions.")
        appendLine("- Interdisciplinary & GS IV Ethics/Case Studies: Where natural and syllabus-relevant, weave interdisciplinary links (e.g., GS I+II, GS II+III, GS III+IV). When the source relates to ethics, governance, or institutional integrity, frame either theoretical ethics questions or realistic administrative case studies (with clear stakeholders, ethical dilemmas, administrative constraints, and practical decision-making demands).")
        appendLine()
        appendLine("## 4. SYLLABUS & PYQ CORRELATION, QUALITY CONTROL & ANSWER BLUEPRINT")
        appendLine("- SYLLABUS & PYQ CORRELATION: Treat the source below as a conceptual or current-affairs anchor. Extract the underlying static syllabus concept and broader constitutional, institutional, economic, or strategic significance. Never fabricate UPSC PYQs or invent unsupported facts.")
        appendLine("- INTERNAL QUALITY CONTROL: Reject and reformulate any question that is purely factual, repetitive, vague, lacks an appropriate directive, or merely summarises the source.")
        appendLine("- STRUCTURED MODEL ANSWER BLUEPRINT (FOR FOLIOPREP FLASHCARDS): For every subjective question, populate \"intro\" with a crisp contextual introduction addressing the core demand, \"points\" with distinct multi-dimensional analytical arguments (citing relevant constitutional articles, committees, policies, mechanisms, or examples), and \"conclusion\" with a balanced synthesis / way forward.")
        appendLine()
        appendLine("## 5. OUTPUT FORMAT (STRICT JSON FOR FOLIOPREP)")
        appendLine("- For every subjective item: set \"marks\" to 10 for 10-mark questions ($mains10Count) and 15 for 15-mark questions ($mains15Count), and clearly indicate '(10 Marks, 150 Words)' or '(15 Marks, 250 Words)' at the end of \"q\".")
        appendOutputSchemaAndSource(schemaBlock, cleanSource)
    }

    private fun buildIntegratedBothSkillPrompt(
        taskSummary: String,
        subjectLine: String,
        latestPrelimsYear: Int,
        latestMainsYear: Int,
        includeMcq: Boolean,
        includeSubjective: Boolean,
        mcqCount: Int,
        mains10Count: Int,
        mains15Count: Int,
        difficulty: QuestionDifficulty,
        mcqTypeRule: String?,
        schemaBlock: String,
        cleanSource: String
    ): String = buildString {
        appendLine("# SKILL: UPSC CSE PRELIMS & MAINS – INTEGRATED EXPERT QUESTION PAPER SETTER")
        appendLine()
        appendLine("## 1. ROLE AND PERSONA")
        appendLine(DEFAULT_SKILL_PROMPT)
        appendLine("Draft an integrated set of authentic UPSC CSE Prelims (GS Paper I) MCQs and UPSC CSE Mains (GS Papers I–IV) descriptive questions that reflect the intellectual depth, analytical rigour, framing, and precision of official UPSC question papers—never coaching-institute gimmicks or generic trivia.")
        appendLine()
        appendLine("## 2. ASSIGNMENT & DIFFICULTY CALIBRATION")
        appendLine("- GENERATE EXACTLY: $taskSummary.")
        appendLine("- $subjectLine")
        if (includeMcq && !mcqTypeRule.isNullOrBlank()) {
            appendLine("- $mcqTypeRule")
        }
        appendLine("- EXAM PATTERN CALIBRATION: Strictly emulate official UPSC CSE Prelims $latestPrelimsYear (and all previous years) for MCQs, AND official UPSC CSE Mains $latestMainsYear (and all previous years) for descriptive questions.")
        appendLine("- TARGET HARDNESS / DIFFICULTY LEVEL: ${difficulty.label} — ${difficulty.description}.")
        appendLine("- Prelims Calibration: ${difficultyGuidanceForPrelims(difficulty)}")
        appendLine("- Mains Calibration: ${difficultyGuidanceForMains(difficulty)}")
        appendLine()
        if (includeMcq) {
            appendLine("## 3. PRELIMS MCQ STANDARDS (GS PAPER I — $mcqCount MCQs)")
            appendLine("- Test conceptual depth, subtle distinctions, exceptions, static-current application, and analytical elimination across History & Culture, Polity & Governance, Geography, Economy, Environment & Ecology, Sci-Tech, and IR.")
            appendLine("- Supported MCQ styles include direct analytical MCQs, 3/4-statement questions, Nested 3 and 4 statement type (2026 — 3/4 statements followed by 3/4 derived inferences), 2/3-column match-the-following, assertion-reasoning, matched pairs, and multiple-item identification.")
            appendLine("- Use qualifiers carefully (Only, All, Always, Generally, Exclusively, Necessarily, May, Can, Primarily) and construct intelligent, plausible distractors (subtle factual/conceptual errors, closely related provisions/institutions, interchanged habitats/locations, incorrect causality). Never use absurd distractors.")
            appendLine("- In each MCQ's \"exp\", provide statement-by-statement verification (with articles/statutes/mechanisms/exceptions), brief elimination logic, and UPSC syllabus relevance.")
            appendLine()
        }
        if (includeSubjective) {
            appendLine("## ${if (includeMcq) "4" else "3"}. MAINS DESCRIPTIVE STANDARDS (GS PAPERS I–IV — ${mains10Count}×10M, ${mains15Count}×15M)")
            appendLine("- Map naturally to GS Paper I, II, III, or IV (including interdisciplinary links and GS IV ethics/administrative case studies where genuinely relevant).")
            appendLine("- Use authentic UPSC directives (Discuss, Examine, Critically examine, Analyse, Critically analyse, Evaluate, Elucidate, Comment, Substantiate, Assess, To what extent, How far).")
            appendLine("- Calibrate 10-mark (150 words) questions for focused analysis and 15-mark (250 words) questions for deeper multidimensional evaluation (constitutional, institutional, socio-economic, environmental, technological, ethical, and reform dimensions).")
            appendLine("- Populate each flashcard blueprint with a crisp \"intro\", multi-dimensional \"points\" (facts/articles/policies/examples), and a balanced \"conclusion\" / way forward.")
            appendLine()
        }
        appendLine("## ${if (includeMcq && includeSubjective) "5" else "4"}. SYLLABUS & PYQ CORRELATION, ACCURACY & QUALITY CONTROL")
        appendLine("- SYLLABUS & PYQ CORRELATION: Treat the source text below as a current-affairs or conceptual anchor. Freely integrate verified static UPSC CSE syllabus knowledge and interdisciplinary links without restricting questions to sentences in the source, inventing unsupported facts, or fabricating PYQs.")
        appendLine("- STRICT ACCURACY & QUALITY CONTROL: Accuracy takes precedence over difficulty. Independently verify every constitutional provision, statute, scientific fact, and economic mechanism; ensure zero ambiguity, zero repetition across Prelims & Mains items, and authentic UPSC standard.")
        appendLine()
        appendLine("## ${if (includeMcq && includeSubjective) "6" else "5"}. OUTPUT FORMAT (STRICT JSON FOR FOLIOPREP)")
        if (includeMcq) {
            appendLine("- For every MCQ: provide exactly 4 options in \"opts\", set \"ans\" to the 0-based index (0, 1, 2, or 3) of the single correct option, and include \"difficulty\" set to \"Easy\", \"Medium\", or \"Hard\" reflecting the question's actual UPSC CSE hardness level (calibrated primarily around ${difficulty.label}).")
        }
        if (includeSubjective) {
            appendLine("- For every subjective item: set \"marks\" to 10 for 10-mark questions ($mains10Count) and 15 for 15-mark questions ($mains15Count), and indicate '(10 Marks, 150 Words)' or '(15 Marks, 250 Words)' at the end of \"q\".")
        }
        appendOutputSchemaAndSource(schemaBlock, cleanSource)
    }

    private fun buildCustomSkillPrompt(
        customSkill: String,
        taskSummary: String,
        subjectLine: String,
        latestPrelimsYear: Int,
        latestMainsYear: Int,
        includeMcq: Boolean,
        includeSubjective: Boolean,
        mains10Count: Int,
        mains15Count: Int,
        difficulty: QuestionDifficulty,
        mcqTypeRule: String?,
        schemaBlock: String,
        cleanSource: String
    ): String = buildString {
        appendLine(customSkill)
        appendLine()
        appendLine("Using the source text below as a topic/current-affairs anchor, generate $taskSummary aligned with the UPSC CSE syllabus and Previous Year Question (PYQ) trends.")
        appendLine(subjectLine)
        if (includeMcq && !mcqTypeRule.isNullOrBlank()) {
            appendLine(mcqTypeRule)
        }
        appendLine("TARGET HARDNESS / DIFFICULTY LEVEL: ${difficulty.label} — ${difficulty.description}.")
        appendLine()
        appendLine("EXAM PATTERN & SYLLABUS RULES:")
        if (includeMcq) {
            appendLine("- PRELIMS PATTERN CALIBRATION (Up to $latestPrelimsYear): Align with UPSC CSE Prelims $latestPrelimsYear and previous years, following the requested MCQ question style with rigorous explanations.")
        }
        if (includeSubjective) {
            appendLine("- MAINS PATTERN CALIBRATION (Up to $latestMainsYear): Align with UPSC CSE Mains $latestMainsYear and previous years using authentic UPSC directive words, calibrated for 10-mark (150 words) and 15-mark (250 words) depth.")
        }
        appendLine("- SYLLABUS & PYQ CORRELATION: Treat the source as an anchor and freely integrate verified correlated static UPSC CSE syllabus knowledge without fabricating facts or PYQs.")
        if (includeMcq) {
            appendLine("- For every MCQ: provide exactly 4 options in \"opts\", set \"ans\" to the 0-based index (0, 1, 2, or 3) of the single correct option, and include \"difficulty\" set to \"Easy\", \"Medium\", or \"Hard\" reflecting the question's actual UPSC CSE hardness level (calibrated primarily around ${difficulty.label}).")
        }
        if (includeSubjective) {
            appendLine("- For every subjective item: set \"marks\" to 10 for 10-mark questions ($mains10Count) and 15 for 15-mark questions ($mains15Count).")
        }
        appendOutputSchemaAndSource(schemaBlock, cleanSource)
    }

    private fun StringBuilder.appendOutputSchemaAndSource(
        schemaBlock: String,
        cleanSource: String
    ) {
        appendLine("- OUTPUT FORMAT: Return ONLY a single fenced JSON code snippet block (```json ... ```) formatted cleanly with proper spacing and indentation, with no greetings, commentary, or extra text outside the code block so it is easy to read and copy in one tap.")
        appendLine("- Use appropriate \\n line breaks inside multi-statement UPSC question stems (e.g., separating numbered statements 1., 2., 3. and the concluding question) and explanations for clear readability.")
        appendLine("- Follow this exact JSON schema inside the ```json code snippet:")
        appendLine("```json")
        appendLine(schemaBlock)
        appendLine("```")
        appendLine()
        appendLine("SOURCE TEXT:")
        append(cleanSource)
    }

    private fun buildSchemaBlock(
        includeMcq: Boolean,
        includeSubjective: Boolean,
        difficulty: QuestionDifficulty,
        mcqQuestionType: McqQuestionType? = null,
        customMcqQuestionType: String = ""
    ): String {
        val sampleMcqQuestion = if (
            customMcqQuestionType.isBlank() &&
            mcqQuestionType == McqQuestionType.NESTED_3_4_STATEMENT
        ) {
            "Statements:\\n1. First primary statement here.\\n2. Second primary statement here.\\n3. Third primary statement here.\\n\\nInferences:\\nI. First inference derived from statement(s) above.\\nII. Second inference derived from statement(s) above.\\nIII. Third inference derived from statement(s) above.\\n\\nHow many of the above inferences are true?"
        } else {
            "Consider the following statements:\\n1. First statement here.\\n2. Second statement here.\\n\\nHow many of the statements given above are correct?"
        }

        return when {
            includeMcq && includeSubjective -> """
{
  "title": "Concise UPSC Topic Title",
  "tags": [
    "Polity"
  ],
  "mcqs": [
    {
      "q": "$sampleMcqQuestion",
      "opts": [
        "Only one",
        "Only two",
        "All three",
        "None"
      ],
      "ans": 0,
      "difficulty": "${difficulty.jsonValue}",
      "exp": "Statement/Inference 1 is correct because ...\nStatement/Inference 2 is incorrect because ...\n\nWhy other options are incorrect: ...\nUPSC Relevance: GS Paper I - ..."
    }
  ],
  "subjective": [
    {
      "q": "Mains question with authentic UPSC directive (10 Marks, 150 Words)",
      "marks": 10,
      "intro": "2-3 sentence contextual introduction addressing the core demand",
      "points": [
        "Key dimension 1 with constitutional/policy anchor",
        "Key dimension 2 with analytical reasoning",
        "Key dimension 3 with challenges/implications",
        "Key dimension 4 with institutional/reform angle"
      ],
      "conclusion": "Balanced way forward / constitutional synthesis"
    }
  ]
}
            """.trimIndent()

            includeSubjective -> """
{
  "title": "Concise UPSC Topic Title",
  "tags": [
    "Environment"
  ],
  "subjective": [
    {
      "q": "Mains question with authentic UPSC directive (10 Marks, 150 Words)",
      "marks": 10,
      "intro": "2-3 sentence contextual introduction addressing the core demand",
      "points": [
        "Key dimension 1 with constitutional/policy anchor",
        "Key dimension 2 with analytical reasoning",
        "Key dimension 3 with challenges/implications",
        "Key dimension 4 with institutional/reform angle"
      ],
      "conclusion": "Balanced way forward / constitutional synthesis"
    }
  ]
}
            """.trimIndent()

            else -> """
{
  "title": "Concise UPSC Topic Title",
  "tags": [
    "Polity"
  ],
  "mcqs": [
    {
      "q": "$sampleMcqQuestion",
      "opts": [
        "Only one",
        "Only two",
        "All three",
        "None"
      ],
      "ans": 0,
      "difficulty": "${difficulty.jsonValue}",
      "exp": "Statement/Inference 1 is correct because ...\nStatement/Inference 2 is incorrect because ...\n\nWhy other options are incorrect: ...\nUPSC Relevance: GS Paper I - ..."
    }
  ]
}
            """.trimIndent()
        }
    }
}
