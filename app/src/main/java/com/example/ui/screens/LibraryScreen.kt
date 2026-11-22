package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExamYearConfig
import com.example.data.LocalBackupManager
import com.example.data.local.DeckSummary
import com.example.data.local.McqQuestionEntity
import com.example.domain.ExternalAiLauncher
import com.example.domain.ParsedStudyPayload
import com.example.domain.QuestionDifficulty
import com.example.ui.components.ClipboardAutoImportBanner
import com.example.ui.components.ExamYearHeaderBar
import com.example.ui.components.ManualJsonPasteDialog
import com.example.ui.components.OpenBackupDocumentContract
import com.example.viewmodel.LibraryCategory
import com.example.viewmodel.LibraryFilterState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    examYearConfig: ExamYearConfig,
    decks: List<DeckSummary>,
    totalDeckCount: Int,
    prelimsDeckCount: Int,
    mainsDeckCount: Int,
    allMcqs: List<McqQuestionEntity>,
    filterState: LibraryFilterState,
    availableTags: List<String>,
    detectedClipboard: ParsedStudyPayload?,
    importErrorMessage: String?,
    showFreshRestorePrompt: Boolean,
    onUpdatePrelimsYear: (Int) -> Unit,
    onUpdateMainsYear: (Int) -> Unit,
    onCategorySelected: (LibraryCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onOpenDeck: (Long) -> Unit,
    onDeleteDeck: (Long) -> Unit,
    onOpenStudio: () -> Unit,
    onReviseByDifficulty: (String) -> Unit,
    onRestoreFromBackupUri: (Uri) -> Unit,
    onDismissFreshRestorePrompt: () -> Unit,
    onReviewClipboardBanner: () -> Unit,
    onDismissClipboardBanner: () -> Unit,
    onParseManualJson: (String) -> Unit,
    onClearImportError: () -> Unit
) {
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }
    var pendingDeleteDeck by remember { mutableStateOf<DeckSummary?>(null) }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = OpenBackupDocumentContract()
    ) { uri ->
        if (uri != null) {
            onRestoreFromBackupUri(uri)
        }
    }

    fun launchBackupFilePicker() {
        restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    val easyMcqCount = remember(allMcqs) {
        allMcqs.count { QuestionDifficulty.fromRaw(it.difficulty) == QuestionDifficulty.EASY }
    }
    val mediumMcqCount = remember(allMcqs) {
        allMcqs.count { QuestionDifficulty.fromRaw(it.difficulty) == QuestionDifficulty.MEDIUM }
    }
    val hardMcqCount = remember(allMcqs) {
        allMcqs.count { QuestionDifficulty.fromRaw(it.difficulty) == QuestionDifficulty.HARD }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "masthead") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FOLIO PREP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "UPSC Study Archive",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clip = ExternalAiLauncher.readClipboardText(context).orEmpty()
                            if (clip.isNotBlank() && com.example.domain.JsonPayloadParser.looksLikeStudyPayload(clip)) {
                                onParseManualJson(clip)
                            } else {
                                onClearImportError()
                                showPasteDialog = true
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("library_paste_clipboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPasteGo,
                            contentDescription = "Paste AI JSON from Clipboard",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste JSON")
                    }
                }
            }

            item(key = "exam_year_header") {
                ExamYearHeaderBar(
                    examYearConfig = examYearConfig,
                    onUpdatePrelimsYear = onUpdatePrelimsYear,
                    onUpdateMainsYear = onUpdateMainsYear
                )
            }

            item(key = "clipboard_banner") {
                ClipboardAutoImportBanner(
                    detectedPayload = detectedClipboard,
                    onReviewAndImport = onReviewClipboardBanner,
                    onDismiss = onDismissClipboardBanner
                )
            }

            if (totalDeckCount > 0) {
                item(key = "category_selector") {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_category_selector")
                    ) {
                        LibraryCategory.entries.forEachIndexed { index, category ->
                            val count = when (category) {
                                LibraryCategory.ALL -> totalDeckCount
                                LibraryCategory.PRELIMS -> prelimsDeckCount
                                LibraryCategory.MAINS -> mainsDeckCount
                            }
                            SegmentedButton(
                                selected = filterState.selectedCategory == category,
                                onClick = { onCategorySelected(category) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = LibraryCategory.entries.size
                                ),
                                icon = {},
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("library_category_${category.name.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = category.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (filterState.selectedCategory == category) {
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (allMcqs.isNotEmpty() && filterState.selectedCategory != LibraryCategory.MAINS) {
                item(key = "revise_by_hardness_card") {
                    ReviseByDifficultyCard(
                        easyCount = easyMcqCount,
                        mediumCount = mediumMcqCount,
                        hardCount = hardMcqCount,
                        onSelectDifficulty = onReviseByDifficulty
                    )
                }
            }

            if (totalDeckCount > 0) {
                val activeCategoryDeckCount = when (filterState.selectedCategory) {
                    LibraryCategory.ALL -> totalDeckCount
                    LibraryCategory.PRELIMS -> prelimsDeckCount
                    LibraryCategory.MAINS -> mainsDeckCount
                }
                item(key = "search_and_tags") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = filterState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search UPSC decks or subject tags...") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (filterState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterState.selectedTag == null,
                                onClick = { onTagSelected(null) },
                                label = { Text("All ($activeCategoryDeckCount)") },
                                modifier = Modifier.testTag("tag_filter_all")
                            )
                            availableTags.forEach { tag ->
                                FilterChip(
                                    selected = filterState.selectedTag.equals(tag, ignoreCase = true),
                                    onClick = { onTagSelected(tag) },
                                    label = { Text(tag) },
                                    modifier = Modifier.testTag("tag_filter_$tag")
                                )
                            }
                        }
                    }
                }
            }

            if (totalDeckCount == 0) {
                item(key = "empty_state") {
                    EmptyLibraryCard(
                        onOpenStudio = onOpenStudio,
                        onOpenPasteDialog = {
                            onClearImportError()
                            showPasteDialog = true
                        },
                        onRestoreBackup = { launchBackupFilePicker() }
                    )
                }
            } else if (decks.isEmpty()) {
                val emptyTitle = when (filterState.selectedCategory) {
                    LibraryCategory.PRELIMS -> if (prelimsDeckCount == 0) "No Prelims - MCQ decks yet" else "No matching Prelims decks"
                    LibraryCategory.MAINS -> if (mainsDeckCount == 0) "No Mains decks yet" else "No matching Mains decks"
                    LibraryCategory.ALL -> "No matching study decks"
                }
                val emptySubtitle = when (filterState.selectedCategory) {
                    LibraryCategory.PRELIMS -> if (prelimsDeckCount == 0) {
                        "Generate Prelims MCQs in Studio or switch to All to view your Mains decks."
                    } else {
                        "Try clearing the search query or switching to All subjects."
                    }
                    LibraryCategory.MAINS -> if (mainsDeckCount == 0) {
                        "Generate 10/15-mark Mains flashcards in Studio or switch to All to view your Prelims decks."
                    } else {
                        "Try clearing the search query or switching to All subjects."
                    }
                    LibraryCategory.ALL -> "Try clearing the search query or switching to All subjects."
                }
                item(key = "no_matches") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = emptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    onCategorySelected(LibraryCategory.ALL)
                                    onSearchQueryChange("")
                                    onTagSelected(null)
                                },
                                modifier = Modifier.testTag("library_reset_filters_button")
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                items(items = decks, key = { it.id }) { deck ->
                    EditorialDeckCard(
                        deck = deck,
                        onClick = { onOpenDeck(deck.id) },
                        onRequestDelete = { pendingDeleteDeck = deck }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showPasteDialog) {
        ManualJsonPasteDialog(
            initialError = importErrorMessage,
            onDismiss = {
                showPasteDialog = false
                onClearImportError()
            },
            onParseJson = { raw ->
                onParseManualJson(raw)
                showPasteDialog = false
            }
        )
    }

    pendingDeleteDeck?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteDeck = null },
            title = { Text("Delete Study Deck?") },
            text = {
                Text(
                    "Remove \"${target.title}\" and all its saved Prelims MCQs, Mains flashcards, and score history from this device?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDeck(target.id)
                        pendingDeleteDeck = null
                    },
                    modifier = Modifier.testTag("confirm_delete_deck")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDeck = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFreshRestorePrompt) {
        AlertDialog(
            onDismissRequest = onDismissFreshRestorePrompt,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Restore Previous Local Backup?") },
            text = {
                Text(
                    "Reinstalled FolioPrep? If you previously backed up your UPSC CSE questions, select ${LocalBackupManager.defaultBackupDisplayPath()} from your Android folder to restore all decks, MCQs, Mains flashcards, and score history."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissFreshRestorePrompt()
                        launchBackupFilePicker()
                    },
                    modifier = Modifier.testTag("fresh_install_restore_button")
                ) {
                    Text("Restore from File")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissFreshRestorePrompt,
                    modifier = Modifier.testTag("fresh_install_start_fresh_button")
                ) {
                    Text("Start Fresh")
                }
            }
        )
    }
}

@Composable
private fun ReviseByDifficultyCard(
    easyCount: Int,
    mediumCount: Int,
    hardCount: Int,
    onSelectDifficulty: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Revise MCQs by Hardness",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DifficultyRevisionPill(
                    label = "Easy",
                    count = easyCount,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    testTag = "library_revise_easy",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectDifficulty(QuestionDifficulty.EASY.label) }
                )
                DifficultyRevisionPill(
                    label = "Medium",
                    count = mediumCount,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    testTag = "library_revise_medium",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectDifficulty(QuestionDifficulty.MEDIUM.label) }
                )
                DifficultyRevisionPill(
                    label = "Hard",
                    count = hardCount,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    testTag = "library_revise_hard",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectDifficulty(QuestionDifficulty.HARD.label) }
                )
            }
        }
    }
}

@Composable
private fun DifficultyRevisionPill(
    label: String,
    count: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = count > 0,
        shape = MaterialTheme.shapes.medium,
        color = if (count > 0) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (count > 0) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count MCQ${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (count > 0) contentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    onOpenStudio: () -> Unit,
    onOpenPasteDialog: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_desk),
                contentDescription = "Orderly UPSC study desk with linen notebook and flashcards",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_library_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onOpenStudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_open_studio_button")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build Prompt in Studio")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenPasteDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_paste_json_button")
            ) {
                Icon(Icons.Outlined.ContentPasteGo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Already Have AI JSON? Paste Here")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onRestoreBackup,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_restore_backup_button")
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore from Local Backup File")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorialDeckCard(
    deck: DeckSummary,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val dateLabel = remember(deck.lastPracticedAt, deck.createdAt) {
        val ts = deck.lastPracticedAt ?: deck.createdAt
        val prefix = if (deck.lastPracticedAt != null) "Studied" else "Saved"
        "$prefix ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ts))}"
    }

    val sourceIcon = when (deck.sourceType) {
        "LINK" -> Icons.Outlined.Link
        "IMAGE" -> Icons.Outlined.PhotoCamera
        else -> Icons.Outlined.Description
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("deck_card_${deck.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = sourceIcon,
                            contentDescription = "Source type ${deck.sourceType}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "P:${deck.prelimsYear} · M:${deck.mainsYear}  ·  $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.testTag("delete_deck_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete deck ${deck.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val tags = deck.tagList()
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (deck.mcqCount > 0) {
                    MetricBadge(
                        icon = Icons.Outlined.Quiz,
                        text = "${deck.mcqCount} MCQs",
                        highlighted = true
                    )
                }
                if (deck.subjectiveCount > 0) {
                    MetricBadge(
                        icon = Icons.Outlined.Style,
                        text = "${deck.subjectiveCount} Mains (${deck.masteredSubjectiveCount} mastered)",
                        highlighted = false
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                deck.bestAccuracyPercent?.let { best ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Best accuracy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Best $best%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    highlighted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
    }
}
