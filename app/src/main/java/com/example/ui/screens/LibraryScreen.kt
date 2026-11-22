package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExamYearConfig
import com.example.data.local.DeckSummary
import com.example.domain.ExternalAiLauncher
import com.example.domain.ParsedStudyPayload
import com.example.ui.components.ClipboardAutoImportBanner
import com.example.ui.components.ExamYearHeaderBar
import com.example.ui.components.ManualJsonPasteDialog
import com.example.viewmodel.LibraryFilterState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    examYearConfig: ExamYearConfig,
    decks: List<DeckSummary>,
    totalDeckCount: Int,
    filterState: LibraryFilterState,
    availableTags: List<String>,
    detectedClipboard: ParsedStudyPayload?,
    importErrorMessage: String?,
    onUpdatePrelimsYear: (Int) -> Unit,
    onUpdateMainsYear: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onOpenDeck: (Long) -> Unit,
    onDeleteDeck: (Long) -> Unit,
    onOpenStudio: () -> Unit,
    onReviewClipboardBanner: () -> Unit,
    onDismissClipboardBanner: () -> Unit,
    onParseManualJson: (String) -> Unit,
    onClearImportError: () -> Unit
) {
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }
    var pendingDeleteDeck by remember { mutableStateOf<DeckSummary?>(null) }

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
                                label = { Text("All (${totalDeckCount})") },
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
                        }
                    )
                }
            } else if (decks.isEmpty()) {
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
                                text = "No matching study decks",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try clearing the search query or switching to All subjects.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = {
                                onSearchQueryChange("")
                                onTagSelected(null)
                            }) {
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
}

@Composable
private fun EmptyLibraryCard(
    onOpenStudio: () -> Unit,
    onOpenPasteDialog: () -> Unit
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
