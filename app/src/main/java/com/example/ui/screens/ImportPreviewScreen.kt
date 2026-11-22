package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DraftMcq
import com.example.domain.DraftSubjective
import com.example.domain.ParsedStudyPayload
import com.example.domain.QuestionDifficulty
import com.example.domain.UpscSubjects
import com.example.ui.components.DifficultyBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportPreviewScreen(
    draft: ParsedStudyPayload?,
    onBack: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onUpdateMcq: (DraftMcq) -> Unit,
    onDeleteMcq: (String) -> Unit,
    onUpdateSubjective: (DraftSubjective) -> Unit,
    onDeleteSubjective: (String) -> Unit,
    onSaveDeck: () -> Unit
) {
    var customTagInput by remember { mutableStateOf("") }
    var editingMcq by remember { mutableStateOf<DraftMcq?>(null) }
    var editingSubjective by remember { mutableStateOf<DraftSubjective?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Verify & Save Deck",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (draft != null) {
                Surface(
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onSaveDeck,
                            enabled = draft.totalCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .height(52.dp)
                                .testTag("save_imported_deck_button")
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Offline Deck (${draft.mcqs.size} MCQs · ${draft.subjective.size} Mains)")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (draft == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No imported draft available.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Scaffold
        }

        val inferredMode = when {
            draft.mcqs.isNotEmpty() && draft.subjective.isNotEmpty() -> com.example.domain.QuestionMode.BOTH
            draft.subjective.isNotEmpty() -> com.example.domain.QuestionMode.SUBJECTIVE
            else -> com.example.domain.QuestionMode.MCQ
        }
        val allSelectableTags = remember(draft.tags, inferredMode) {
            (UpscSubjects.tagsForMode(inferredMode) + draft.tags).distinctBy { it.lowercase() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "deck_metadata") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.title,
                                onValueChange = onUpdateTitle,
                                label = { Text("Deck Title") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preview_deck_title_input")
                            )

                            Text(
                                text = "UPSC Subject Tags",
                                style = MaterialTheme.typography.labelLarge
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allSelectableTags.forEach { tag ->
                                    val selected = draft.tags.any { it.equals(tag, ignoreCase = true) }
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onToggleTag(tag) },
                                        label = { Text(tag) },
                                        leadingIcon = if (selected) {
                                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customTagInput,
                                    onValueChange = { customTagInput = it },
                                    placeholder = { Text("Add custom tag (e.g. GS-II, Optional)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_tag_input")
                                )
                                IconButton(
                                    onClick = {
                                        onAddCustomTag(customTagInput)
                                        customTagInput = ""
                                    },
                                    modifier = Modifier.testTag("add_custom_tag_button")
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Add custom tag")
                                }
                            }
                        }
                    }
                }

                if (draft.warnings.isNotEmpty()) {
                    item(key = "warnings") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Parser Notes",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                draft.warnings.forEach { w ->
                                    Text(
                                        text = "• $w",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (draft.mcqs.isNotEmpty()) {
                    item(key = "mcq_header") {
                        Text(
                            text = "Prelims MCQs (${draft.mcqs.size})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    itemsIndexed(items = draft.mcqs, key = { _, item -> item.tempId }) { idx, mcq ->
                        DraftMcqPreviewCard(
                            index = idx + 1,
                            mcq = mcq,
                            onEdit = { editingMcq = mcq },
                            onDelete = { onDeleteMcq(mcq.tempId) }
                        )
                    }
                }

                if (draft.subjective.isNotEmpty()) {
                    item(key = "subj_header") {
                        Text(
                            text = "Mains 10/15-Mark Flashcards (${draft.subjective.size})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    itemsIndexed(items = draft.subjective, key = { _, item -> item.tempId }) { idx, card ->
                        DraftSubjectivePreviewCard(
                            index = idx + 1,
                            card = card,
                            onEdit = { editingSubjective = card },
                            onDelete = { onDeleteSubjective(card.tempId) }
                        )
                    }
                }
            }
        }
    }

    editingMcq?.let { target ->
        EditMcqDialog(
            mcq = target,
            onDismiss = { editingMcq = null },
            onSave = { updated ->
                onUpdateMcq(updated)
                editingMcq = null
            }
        )
    }

    editingSubjective?.let { target ->
        EditSubjectiveDialog(
            card = target,
            onDismiss = { editingSubjective = null },
            onSave = { updated ->
                onUpdateSubjective(updated)
                editingSubjective = null
            }
        )
    }
}

@Composable
private fun DraftMcqPreviewCard(
    index: Int,
    mcq: DraftMcq,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MCQ #$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DifficultyBadge(difficulty = mcq.difficulty)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit MCQ $index")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete MCQ $index")
                    }
                }
            }

            Text(
                text = mcq.question,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            mcq.options.forEachIndexed { optIdx, opt ->
                val isCorrect = optIdx == mcq.correctIndex
                val letter = ('A' + optIdx)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCorrect) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = "$letter. $opt" + if (isCorrect) "  ✓" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCorrect) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Explanation: ${mcq.explanation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DraftSubjectivePreviewCard(
    index: Int,
    card: DraftSubjective,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "MAINS #$index · ${card.marks} MARKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Mains card $index")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Mains card $index")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.question,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Intro: ${card.intro}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            card.keyPoints.forEach { pt ->
                Text(
                    text = "• $pt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Conclusion: ${card.conclusion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EditMcqDialog(
    mcq: DraftMcq,
    onDismiss: () -> Unit,
    onSave: (DraftMcq) -> Unit
) {
    var question by remember(mcq) { mutableStateOf(mcq.question) }
    var optA by remember(mcq) { mutableStateOf(mcq.options.getOrElse(0) { "" }) }
    var optB by remember(mcq) { mutableStateOf(mcq.options.getOrElse(1) { "" }) }
    var optC by remember(mcq) { mutableStateOf(mcq.options.getOrElse(2) { "" }) }
    var optD by remember(mcq) { mutableStateOf(mcq.options.getOrElse(3) { "" }) }
    var correctIdx by remember(mcq) { mutableIntStateOf(mcq.correctIndex) }
    var explanation by remember(mcq) { mutableStateOf(mcq.explanation) }
    var difficulty by remember(mcq) { mutableStateOf(QuestionDifficulty.fromRaw(mcq.difficulty).label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Prelims MCQ") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hardness Level",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestionDifficulty.entries.forEach { level ->
                        FilterChip(
                            selected = difficulty.equals(level.label, ignoreCase = true),
                            onClick = { difficulty = level.label },
                            label = { Text(level.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question Stem") },
                    modifier = Modifier.fillMaxWidth()
                )
                listOf(
                    0 to Pair(optA) { v: String -> optA = v },
                    1 to Pair(optB) { v: String -> optB = v },
                    2 to Pair(optC) { v: String -> optC = v },
                    3 to Pair(optD) { v: String -> optD = v }
                ).forEach { (idx, statePair) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = correctIdx == idx,
                            onClick = { correctIdx = idx }
                        )
                        OutlinedTextField(
                            value = statePair.first,
                            onValueChange = statePair.second,
                            label = { Text("Option ${'A' + idx}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        mcq.copy(
                            question = question,
                            options = listOf(optA, optB, optC, optD),
                            correctIndex = correctIdx,
                            explanation = explanation,
                            difficulty = difficulty
                        )
                    )
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditSubjectiveDialog(
    card: DraftSubjective,
    onDismiss: () -> Unit,
    onSave: (DraftSubjective) -> Unit
) {
    var question by remember(card) { mutableStateOf(card.question) }
    var marks by remember(card) { mutableIntStateOf(card.marks) }
    var intro by remember(card) { mutableStateOf(card.intro) }
    var pointsText by remember(card) { mutableStateOf(card.keyPoints.joinToString("\n")) }
    var conclusion by remember(card) { mutableStateOf(card.conclusion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Mains Flashcard") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = marks == 10,
                        onClick = { marks = 10 },
                        label = { Text("10 Marks (150w)") }
                    )
                    FilterChip(
                        selected = marks == 15,
                        onClick = { marks = 15 },
                        label = { Text("15 Marks (250w)") }
                    )
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Mains Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = intro,
                    onValueChange = { intro = it },
                    label = { Text("Introduction") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it },
                    label = { Text("Key Points (one per line)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conclusion,
                    onValueChange = { conclusion = it },
                    label = { Text("Conclusion / Way Forward") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPoints = pointsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(
                        card.copy(
                            question = question,
                            marks = marks,
                            intro = intro,
                            keyPoints = parsedPoints,
                            conclusion = conclusion
                        )
                    )
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
