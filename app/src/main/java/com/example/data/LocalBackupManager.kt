package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.data.local.DeckDetailBundle
import com.example.data.local.McqQuestionEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.StudyDeckEntity
import com.example.data.local.SubjectiveCardEntity
import com.example.domain.QuestionDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupSnapshot(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val latestPrelimsYear: Int = ExamYearConfig.DEFAULT_EXAM_YEAR,
    val latestMainsYear: Int = ExamYearConfig.DEFAULT_EXAM_YEAR,
    val useCustomSkill: Boolean = false,
    val customSkillText: String = "",
    val decks: List<DeckDetailBundle> = emptyList()
)

data class BackupWriteResult(
    val locationLabel: String,
    val deckCount: Int,
    val mcqCount: Int,
    val subjectiveCount: Int,
    val timestamp: Long
)

data class BackupRestoreResult(
    val restoredDecks: Int,
    val skippedDuplicateDecks: Int,
    val restoredMcqs: Int,
    val restoredSubjective: Int
)

object LocalBackupManager {

    const val BACKUP_FILE_NAME = "folioprep_backup.json"
    const val BACKUP_SUBFOLDER = "FolioPrep"

    fun defaultBackupDisplayPath(): String = "Documents/$BACKUP_SUBFOLDER/$BACKUP_FILE_NAME"

    fun serializeSnapshot(snapshot: BackupSnapshot): String {
        val root = JSONObject().apply {
            put("app", "FolioPrep")
            put("schemaVersion", snapshot.schemaVersion)
            put("exportedAt", snapshot.exportedAt)
            put("latestPrelimsYear", snapshot.latestPrelimsYear)
            put("latestMainsYear", snapshot.latestMainsYear)
            put("useCustomSkill", snapshot.useCustomSkill)
            put("customSkillText", snapshot.customSkillText)

            val decksArr = JSONArray()
            snapshot.decks.forEach { bundle ->
                val d = bundle.deck
                val deckObj = JSONObject().apply {
                    put("title", d.title)
                    put("examScope", d.examScope)
                    put("prelimsYear", d.prelimsYear)
                    put("mainsYear", d.mainsYear)
                    put("tagsSerialized", d.tagsSerialized)
                    put("sourceType", d.sourceType)
                    put("sourceSnippet", d.sourceSnippet)
                    put("createdAt", d.createdAt)
                    if (d.lastPracticedAt != null) put("lastPracticedAt", d.lastPracticedAt)

                    val mcqsArr = JSONArray()
                    bundle.mcqs.forEach { m ->
                        mcqsArr.put(
                            JSONObject().apply {
                                put("question", m.question)
                                put("optionA", m.optionA)
                                put("optionB", m.optionB)
                                put("optionC", m.optionC)
                                put("optionD", m.optionD)
                                put("correctIndex", m.correctIndex)
                                put("explanation", m.explanation)
                                put("orderIndex", m.orderIndex)
                                put("difficulty", QuestionDifficulty.fromRaw(m.difficulty).label)
                            }
                        )
                    }
                    put("mcqs", mcqsArr)

                    val subjArr = JSONArray()
                    bundle.subjectiveCards.forEach { s ->
                        subjArr.put(
                            JSONObject().apply {
                                put("question", s.question)
                                put("marks", s.marks)
                                put("intro", s.intro)
                                put("pointsSerialized", s.pointsSerialized)
                                put("conclusion", s.conclusion)
                                put("isMastered", s.isMastered)
                                put("orderIndex", s.orderIndex)
                            }
                        )
                    }
                    put("subjectiveCards", subjArr)

                    val attemptsArr = JSONArray()
                    bundle.attempts.forEach { a ->
                        attemptsArr.put(
                            JSONObject().apply {
                                put("mode", a.mode)
                                put("totalQuestions", a.totalQuestions)
                                put("correctCount", a.correctCount)
                                put("wrongCount", a.wrongCount)
                                put("skippedCount", a.skippedCount)
                                put("negativeFactor", a.negativeFactor)
                                put("netScore", a.netScore)
                                put("maxScore", a.maxScore)
                                put("durationSeconds", a.durationSeconds)
                                put("completedAt", a.completedAt)
                            }
                        )
                    }
                    put("attempts", attemptsArr)
                }
                decksArr.put(deckObj)
            }
            put("decks", decksArr)
        }
        return root.toString(2)
    }

    fun parseSnapshot(rawJson: String): Result<BackupSnapshot> = runCatching {
        val root = JSONObject(rawJson.trim())
        val decksArr = root.optJSONArray("decks")
            ?: throw IllegalArgumentException("Selected file is not a valid FolioPrep backup (missing 'decks').")

        val bundles = buildList {
            for (i in 0 until decksArr.length()) {
                val deckObj = decksArr.optJSONObject(i) ?: continue
                val title = deckObj.optString("title", "").trim()
                if (title.isEmpty()) continue

                val lastPracticed = if (deckObj.has("lastPracticedAt") && !deckObj.isNull("lastPracticedAt")) {
                    deckObj.optLong("lastPracticedAt")
                } else null

                val deckEntity = StudyDeckEntity(
                    id = 0L,
                    title = title,
                    examScope = deckObj.optString("examScope", "UPSC CSE"),
                    prelimsYear = deckObj.optInt("prelimsYear", ExamYearConfig.DEFAULT_EXAM_YEAR),
                    mainsYear = deckObj.optInt("mainsYear", ExamYearConfig.DEFAULT_EXAM_YEAR),
                    tagsSerialized = deckObj.optString("tagsSerialized", ""),
                    sourceType = deckObj.optString("sourceType", "TEXT"),
                    sourceSnippet = deckObj.optString("sourceSnippet", ""),
                    createdAt = deckObj.optLong("createdAt", System.currentTimeMillis()),
                    lastPracticedAt = lastPracticed
                )

                val mcqs = buildList {
                    val arr = deckObj.optJSONArray("mcqs")
                    if (arr != null) {
                        for (mIdx in 0 until arr.length()) {
                            val obj = arr.optJSONObject(mIdx) ?: continue
                            add(
                                McqQuestionEntity(
                                    id = 0L,
                                    deckId = 0L,
                                    question = obj.optString("question", ""),
                                    optionA = obj.optString("optionA", ""),
                                    optionB = obj.optString("optionB", ""),
                                    optionC = obj.optString("optionC", ""),
                                    optionD = obj.optString("optionD", ""),
                                    correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3),
                                    explanation = obj.optString("explanation", ""),
                                    orderIndex = obj.optInt("orderIndex", mIdx),
                                    difficulty = QuestionDifficulty.fromRaw(obj.optString("difficulty", "Medium")).label
                                )
                            )
                        }
                    }
                }

                val subjective = buildList {
                    val arr = deckObj.optJSONArray("subjectiveCards")
                    if (arr != null) {
                        for (sIdx in 0 until arr.length()) {
                            val obj = arr.optJSONObject(sIdx) ?: continue
                            add(
                                SubjectiveCardEntity(
                                    id = 0L,
                                    deckId = 0L,
                                    question = obj.optString("question", ""),
                                    marks = obj.optInt("marks", 10),
                                    intro = obj.optString("intro", ""),
                                    pointsSerialized = obj.optString("pointsSerialized", ""),
                                    conclusion = obj.optString("conclusion", ""),
                                    isMastered = obj.optBoolean("isMastered", false),
                                    orderIndex = obj.optInt("orderIndex", sIdx)
                                )
                            )
                        }
                    }
                }

                val attempts = buildList {
                    val arr = deckObj.optJSONArray("attempts")
                    if (arr != null) {
                        for (aIdx in 0 until arr.length()) {
                            val obj = arr.optJSONObject(aIdx) ?: continue
                            add(
                                QuizAttemptEntity(
                                    id = 0L,
                                    deckId = 0L,
                                    mode = obj.optString("mode", "PRACTICE"),
                                    totalQuestions = obj.optInt("totalQuestions", 0),
                                    correctCount = obj.optInt("correctCount", 0),
                                    wrongCount = obj.optInt("wrongCount", 0),
                                    skippedCount = obj.optInt("skippedCount", 0),
                                    negativeFactor = obj.optDouble("negativeFactor", 0.0),
                                    netScore = obj.optDouble("netScore", 0.0),
                                    maxScore = obj.optDouble("maxScore", 0.0),
                                    durationSeconds = obj.optInt("durationSeconds", 0),
                                    completedAt = obj.optLong("completedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }

                add(
                    DeckDetailBundle(
                        deck = deckEntity,
                        mcqs = mcqs,
                        subjectiveCards = subjective,
                        attempts = attempts
                    )
                )
            }
        }

        BackupSnapshot(
            schemaVersion = root.optInt("schemaVersion", 1),
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            latestPrelimsYear = root.optInt("latestPrelimsYear", ExamYearConfig.DEFAULT_EXAM_YEAR),
            latestMainsYear = root.optInt("latestMainsYear", ExamYearConfig.DEFAULT_EXAM_YEAR),
            useCustomSkill = root.optBoolean("useCustomSkill", false),
            customSkillText = root.optString("customSkillText", ""),
            decks = bundles
        )
    }

    suspend fun writeBackup(
        context: Context,
        snapshot: BackupSnapshot,
        customTreeUriString: String
    ): Result<BackupWriteResult> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = serializeSnapshot(snapshot)
            val totalMcqs = snapshot.decks.sumOf { it.mcqs.size }
            val totalSubjective = snapshot.decks.sumOf { it.subjectiveCards.size }

            val location = if (customTreeUriString.isNotBlank()) {
                val treeUri = Uri.parse(customTreeUriString)
                val wroteToTree = runCatching {
                    writeToSafTree(context, treeUri, payload)
                }.isSuccess
                if (wroteToTree) {
                    "Selected Folder/$BACKUP_FILE_NAME"
                } else {
                    writeToSharedDocumentsFolder(context, payload)
                }
            } else {
                writeToSharedDocumentsFolder(context, payload)
            }

            BackupWriteResult(
                locationLabel = location,
                deckCount = snapshot.decks.size,
                mcqCount = totalMcqs,
                subjectiveCount = totalSubjective,
                timestamp = snapshot.exportedAt
            )
        }
    }

    suspend fun readBackupFromUri(
        context: Context,
        uri: Uri
    ): Result<BackupSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: throw IllegalStateException("Unable to read the selected backup file.")
            parseSnapshot(raw).getOrThrow()
        }
    }

    fun buildInitialDocumentsFolderUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2F$BACKUP_SUBFOLDER")
            }.getOrNull()
        } else null
    }

    private fun writeToSafTree(context: Context, treeUri: Uri, jsonContent: String) {
        val resolver = context.contentResolver
        val parentDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        var existingFileUri: Uri? = null
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                if (name == BACKUP_FILE_NAME && idIdx >= 0) {
                    val docId = cursor.getString(idIdx)
                    existingFileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    break
                }
            }
        }

        val targetUri = existingFileUri ?: run {
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
            DocumentsContract.createDocument(
                resolver,
                parentDocUri,
                "application/json",
                BACKUP_FILE_NAME
            ) ?: throw IllegalStateException("Could not create backup file in selected folder.")
        }

        resolver.openOutputStream(targetUri, "wt")?.use { out ->
            out.write(jsonContent.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: throw IllegalStateException("Could not open backup output stream.")
    }

    private fun writeToSharedDocumentsFolder(context: Context, jsonContent: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri("external")
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_SUBFOLDER/"

            var existingUri: Uri? = null
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(BACKUP_FILE_NAME, relativePath)
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    existingUri = ContentUris.withAppendedId(collection, id)
                }
            }

            val targetUri = existingUri ?: run {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                resolver.insert(collection, values)
            }

            if (targetUri != null) {
                val wrote = runCatching {
                    resolver.openOutputStream(targetUri, "wt")?.use { out ->
                        out.write(jsonContent.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }.isSuccess
                if (wrote) {
                    return defaultBackupDisplayPath()
                }
            }
        }

        // Fallback for Android 7-9 or if MediaStore insert was blocked
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            BACKUP_SUBFOLDER
        ).apply { mkdirs() }
        val file = File(dir, BACKUP_FILE_NAME)
        file.writeText(jsonContent, Charsets.UTF_8)
        return "App Documents/$BACKUP_SUBFOLDER/$BACKUP_FILE_NAME"
    }
}
