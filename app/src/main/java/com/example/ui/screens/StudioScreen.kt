package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppSettingsConfig
import com.example.data.ExamYearConfig
import com.example.domain.ExternalAiLauncher
import com.example.domain.JsonPayloadParser
import com.example.domain.QuestionMode
import com.example.domain.UpscSubjects
import com.example.ui.components.ClipboardAutoImportBanner
import com.example.ui.components.ManualJsonPasteDialog
import com.example.ui.theme.ChatGptPillBg
import com.example.ui.theme.ChatGptPillBorder
import com.example.ui.theme.ChatGptPillContent
import com.example.ui.theme.GeminiPillBg
import com.example.ui.theme.GeminiPillBorder
import com.example.ui.theme.GeminiPillContent
import com.example.ui.theme.PromptEditorTextStyle
import com.example.viewmodel.InputSourceTab
import com.example.viewmodel.StudioUiState
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    state: StudioUiState,
    examYearConfig: ExamYearConfig,
    appSettings: AppSettingsConfig = AppSettingsConfig(),
    onRefreshInstalledApps: () -> Unit,
    onTryAutoPasteClipboard: (String?, InputSourceTab?) -> Unit,
    onSelectInputTab: (InputSourceTab) -> Unit,
    onUrlInputChange: (String) -> Unit,
    onExtractArticle: () -> Unit,
    onRunOcrOnImages: (List<Uri>, Boolean) -> Unit,
    onSourceTextChange: (String) -> Unit,
    onClearSourceText: () -> Unit,
    onSelectChunk: (Int) -> Unit,
    onSelectSubjectFocus: (String?) -> Unit,
    onSelectQuestionMode: (QuestionMode) -> Unit,
    onAdjustMcqCount: (Int) -> Unit,
    onAdjustMains10Count: (Int) -> Unit,
    onAdjustMains15Count: (Int) -> Unit,
    onPromptTextChange: (String) -> Unit,
    onResetPromptToTemplate: () -> Unit,
    onMarkAwaitingAiReturn: () -> Unit,
    onReviewClipboardBanner: () -> Unit,
    onDismissClipboardBanner: () -> Unit,
    onParseManualJson: (String) -> Unit,
    onClearImportError: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        onRefreshInstalledApps()
    }

    LaunchedEffect(state.inputTab) {
        if (state.inputTab == InputSourceTab.TEXT || state.inputTab == InputSourceTab.LINK) {
            onTryAutoPasteClipboard(ExternalAiLauncher.readClipboardText(context), state.inputTab)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onRunOcrOnImages(uris, false)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onRunOcrOnImages(listOf(uri), state.sourceText.isNotBlank())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            onShowMessage("Camera permission denied. You can still pick note photos via Gallery.")
        }
    }

    fun launchCameraCapture() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createTempCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun validateBeforeShare(): Boolean {
        if (state.sourceText.isBlank() && !state.isPromptManuallyEdited) {
            onShowMessage("Add text, extract an article link, or scan notes with OCR first.")
            return false
        }
        if (state.totalRequestedQuestions <= 0 && !state.isPromptManuallyEdited) {
            onShowMessage("Set at least one MCQ, 10-Mark, or 15-Mark count above 0.")
            return false
        }
        return true
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "studio_header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LOCAL SOURCE STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "UPSC Prompt Studio",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "P:${examYearConfig.latestPrelimsYear} · M:${examYearConfig.latestMainsYear}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Text(
                        text = "Exclusively calibrated for UPSC CSE (Prelims up to ${examYearConfig.latestPrelimsYear}, Mains up to ${examYearConfig.latestMainsYear}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "studio_clipboard_banner") {
                ClipboardAutoImportBanner(
                    detectedPayload = state.detectedClipboardBanner,
                    onReviewAndImport = onReviewClipboardBanner,
                    onDismiss = onDismissClipboardBanner
                )
            }

            // STAGE 1: LOCAL EXTRACTION
            item(key = "stage_1_extract") {
                StudioSectionCard(
                    stepBadge = "STAGE 1",
                    title = "Extract Source Locally",
                    subtitle = "Extract clean article prose or scan textbook notes on-device before drafting your prompt."
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mode_selector")
                    ) {
                        InputSourceTab.entries.forEachIndexed { index, tab ->
                            SegmentedButton(
                                selected = state.inputTab == tab,
                                onClick = { onSelectInputTab(tab) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = InputSourceTab.entries.size
                                ),
                                icon = {
                                    val icon = when (tab) {
                                        InputSourceTab.TEXT -> Icons.Outlined.Description
                                        InputSourceTab.LINK -> Icons.Outlined.Link
                                        InputSourceTab.IMAGE -> Icons.Outlined.PhotoLibrary
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("input_tab_${tab.name.lowercase()}")
                            ) {
                                Text(tab.label, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (state.inputTab) {
                        InputSourceTab.TEXT -> {
                            Text(
                                text = "Paste editorial excerpts, PIB briefs, or NCERT notes directly below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        InputSourceTab.LINK -> {
                            OutlinedTextField(
                                value = state.urlInput,
                                onValueChange = onUrlInputChange,
                                label = { Text("Article URL (The Hindu, IE, PIB, PRS...)") },
                                placeholder = { Text("https://...") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clip = ExternalAiLauncher.readClipboardText(context)?.trim().orEmpty()
                                            if (clip.isNotEmpty()) {
                                                onUrlInputChange(clip)
                                            } else {
                                                onShowMessage("Clipboard is empty.")
                                            }
                                        },
                                        modifier = Modifier.testTag("paste_url_clipboard_icon")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentPaste,
                                            contentDescription = "Paste URL from clipboard"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focus ->
                                        if (focus.isFocused && state.urlInput.isBlank()) {
                                            onTryAutoPasteClipboard(
                                                ExternalAiLauncher.readClipboardText(context),
                                                InputSourceTab.LINK
                                            )
                                        }
                                    }
                                    .testTag("article_url_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onExtractArticle,
                                enabled = !state.isExtracting && state.urlInput.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("extract_article_button")
                            ) {
                                if (state.isExtracting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stripping Ads & Navigation Locally...")
                                } else {
                                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.extract_article))
                                }
                            }
                        }

                        InputSourceTab.IMAGE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !state.isExtracting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pick_ocr_images_button")
                                ) {
                                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Images")
                                }
                                FilledTonalButton(
                                    onClick = { launchCameraCapture() },
                                    enabled = !state.isExtracting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("snap_ocr_photo_button")
                                ) {
                                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera")
                                }
                            }

                            if (state.isExtracting) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Running on-device ML Kit OCR...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (state.selectedImageUris.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.selectedImageUris.forEachIndexed { idx, uri ->
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected study note page ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(MaterialTheme.shapes.small)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !state.extractionStatusMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.extractionStatusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = !state.extractionError.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.extractionError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.sourceText,
                        onValueChange = onSourceTextChange,
                        label = { Text("Extracted Source Text (Editable)") },
                        placeholder = {
                            Text("Paste notes, extract an editorial link, or scan textbook pages above...")
                        },
                        minLines = 5,
                        maxLines = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (focus.isFocused && state.sourceText.isBlank()) {
                                    onTryAutoPasteClipboard(
                                        ExternalAiLauncher.readClipboardText(context),
                                        InputSourceTab.TEXT
                                    )
                                }
                            }
                            .testTag("studio_source_text_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.activeSourceWordCount} words · ${state.sourceText.length} chars",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    val clip = ExternalAiLauncher.readClipboardText(context)?.trim().orEmpty()
                                    if (clip.isNotEmpty()) {
                                        onSourceTextChange(clip)
                                    } else {
                                        onShowMessage("Clipboard is empty.")
                                    }
                                },
                                modifier = Modifier.testTag("paste_source_text_button")
                            ) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste")
                            }
                            if (state.sourceText.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearSourceText,
                                    modifier = Modifier.testTag("clear_source_text_button")
                                ) {
                                    Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    if (state.chunks.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Long source auto-chunked (${state.chunks.size} safe ~2,500-word parts to avoid AI app truncation):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.chunks.forEach { chunk ->
                                        FilterChip(
                                            selected = state.selectedChunkIndex == chunk.index,
                                            onClick = { onSelectChunk(chunk.index) },
                                            label = { Text("Chunk ${chunk.index + 1} (${chunk.wordCount}w)") },
                                            modifier = Modifier.testTag("chunk_chip_${chunk.index}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STAGE 2: CONFIGURE & PROMPT EDITOR
            item(key = "stage_2_configure") {
                StudioSectionCard(
                    stepBadge = "STAGE 2",
                    title = "Configure & Edit UPSC Prompt",
                    subtitle = "Choose Prelims MCQs, Mains 10/15-markers, or both, then inspect or edit the prompt."
                ) {
                    Text(
                        text = "Question Format",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_mode_selector")
                    ) {
                        QuestionMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.questionMode == mode,
                                onClick = { onSelectQuestionMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = QuestionMode.entries.size
                                ),
                                modifier = Modifier.testTag("question_mode_${mode.name.lowercase()}")
                            ) {
                                Text(mode.label, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val modeTagHeader = when (state.questionMode) {
                        QuestionMode.MCQ -> "Prelims MCQ Subject Tags (Optional)"
                        QuestionMode.SUBJECTIVE -> "Mains 10/15M Subject Tags (Optional)"
                        QuestionMode.BOTH -> "Combined Prelims & Mains Tags (Optional)"
                    }
                    val activeModeTags = remember(state.questionMode) {
                        UpscSubjects.tagsForMode(state.questionMode)
                    }

                    Text(
                        text = modeTagHeader,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        activeModeTags.forEach { tag ->
                            FilterChip(
                                selected = state.selectedSubjectFocus == tag,
                                onClick = { onSelectSubjectFocus(tag) },
                                label = { Text(tag) },
                                modifier = Modifier.testTag("subject_focus_$tag")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.questionMode == QuestionMode.MCQ || state.questionMode == QuestionMode.BOTH) {
                            QuestionCountStepperRow(
                                label = "Prelims MCQs",
                                helper = "Pattern up to ${examYearConfig.latestPrelimsYear}",
                                count = state.mcqCount,
                                onDecrement = { onAdjustMcqCount(-1) },
                                onIncrement = { onAdjustMcqCount(1) },
                                onQuickPick = { target -> onAdjustMcqCount(target - state.mcqCount) },
                                quickValues = listOf(5, 10, 15),
                                testTagPrefix = "mcq_count"
                            )
                        }
                        if (state.questionMode == QuestionMode.SUBJECTIVE || state.questionMode == QuestionMode.BOTH) {
                            QuestionCountStepperRow(
                                label = "Mains 10-Markers",
                                helper = "150 words · Pattern up to ${examYearConfig.latestMainsYear}",
                                count = state.mains10Count,
                                onDecrement = { onAdjustMains10Count(-1) },
                                onIncrement = { onAdjustMains10Count(1) },
                                onQuickPick = { target -> onAdjustMains10Count(target - state.mains10Count) },
                                quickValues = listOf(2, 3, 5),
                                testTagPrefix = "mains10_count"
                            )
                            QuestionCountStepperRow(
                                label = "Mains 15-Markers",
                                helper = "250 words · Pattern up to ${examYearConfig.latestMainsYear}",
                                count = state.mains15Count,
                                onDecrement = { onAdjustMains15Count(-1) },
                                onIncrement = { onAdjustMains15Count(1) },
                                onQuickPick = { target -> onAdjustMains15Count(target - state.mains15Count) },
                                quickValues = listOf(2, 3, 5),
                                testTagPrefix = "mains15_count"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Prompt Editor",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (state.isPromptManuallyEdited) {
                            TextButton(
                                onClick = onResetPromptToTemplate,
                                modifier = Modifier.testTag("reset_prompt_template_button")
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Template")
                            }
                        }
                    }

                    val isCustomSkillActive = appSettings.useCustomSkill && appSettings.customSkillText.isNotBlank()
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCustomSkillActive) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        }
                    ) {
                        Text(
                            text = if (isCustomSkillActive) {
                                "Active Skill: Custom Skill (configured in Settings)"
                            } else {
                                "Active Skill: UPSC CSE Paper Setter (Default)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCustomSkillActive) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (state.yearChangedWhileCustomPrompt) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Exam pattern or prompt skill updated. Tap Reset Template to apply P:${examYearConfig.latestPrelimsYear} / M:${examYearConfig.latestMainsYear} and your active skill to the prompt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.promptText,
                        onValueChange = onPromptTextChange,
                        textStyle = PromptEditorTextStyle,
                        minLines = 8,
                        maxLines = 14,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_prompt_editor")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val promptLen = state.promptText.length
                    val lengthStatusColor = if (promptLen > 16000) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = if (promptLen > 16000) {
                            "$promptLen chars · Long for some AI apps—consider Copy Prompt or picking a smaller chunk."
                        } else {
                            "$promptLen chars · Safe for Android Sharesheet & direct AI app sharing"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = lengthStatusColor
                    )
                }
            }

            // STAGE 3: DISPATCH & AUTO-IMPORT
            item(key = "stage_3_dispatch") {
                StudioSectionCard(
                    stepBadge = "STAGE 3",
                    title = "Generate with External AI",
                    subtitle = "Use ChatGPT, Gemini, Claude, or Perplexity installed on your device."
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.micro_guide_steps),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Quick Send to AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val chatGptInstalled = remember(state.installedAiApps) {
                        state.installedAiApps.any { it.packageName == "com.openai.chatgpt" }
                    }
                    val geminiInstalled = remember(state.installedAiApps) {
                        state.installedAiApps.any { it.packageName == "com.google.android.apps.bard" }
                    }
                    val otherInstalledApps = remember(state.installedAiApps) {
                        state.installedAiApps.filterNot {
                            it.packageName == "com.openai.chatgpt" ||
                                it.packageName == "com.google.android.apps.bard"
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeaturedAiPillButton(
                            label = "ChatGPT",
                            containerColor = ChatGptPillBg,
                            borderColor = ChatGptPillBorder,
                            contentColor = ChatGptPillContent,
                            testTag = "direct_ai_gpt",
                            onClick = {
                                if (validateBeforeShare()) {
                                    onMarkAwaitingAiReturn()
                                    if (chatGptInstalled) {
                                        ExternalAiLauncher.sharePromptToSpecificApp(
                                            context = context,
                                            packageName = "com.openai.chatgpt",
                                            prompt = state.promptText
                                        )
                                    } else {
                                        onShowMessage("ChatGPT not detected — opening Android Share sheet")
                                        ExternalAiLauncher.sharePromptViaChooser(context, state.promptText)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FeaturedAiPillButton(
                            label = "Gemini",
                            containerColor = GeminiPillBg,
                            borderColor = GeminiPillBorder,
                            contentColor = GeminiPillContent,
                            testTag = "direct_ai_gem",
                            onClick = {
                                if (validateBeforeShare()) {
                                    onMarkAwaitingAiReturn()
                                    if (geminiInstalled) {
                                        ExternalAiLauncher.sharePromptToSpecificApp(
                                            context = context,
                                            packageName = "com.google.android.apps.bard",
                                            prompt = state.promptText
                                        )
                                    } else {
                                        onShowMessage("Gemini not detected — opening Android Share sheet")
                                        ExternalAiLauncher.sharePromptViaChooser(context, state.promptText)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (otherInstalledApps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            otherInstalledApps.forEach { app ->
                                ElevatedFilterChip(
                                    selected = false,
                                    shape = CircleShape,
                                    onClick = {
                                        if (validateBeforeShare()) {
                                            onMarkAwaitingAiReturn()
                                            ExternalAiLauncher.sharePromptToSpecificApp(
                                                context = context,
                                                packageName = app.packageName,
                                                prompt = state.promptText
                                            )
                                        }
                                    },
                                    label = { Text("Send to ${app.name}") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("direct_ai_${app.shortBadge.lowercase()}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (validateBeforeShare()) {
                                onMarkAwaitingAiReturn()
                                ExternalAiLauncher.sharePromptViaChooser(context, state.promptText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("share_prompt_to_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share_to_any_ai))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (validateBeforeShare()) {
                                    ExternalAiLauncher.copyPromptToClipboard(context, state.promptText)
                                    onMarkAwaitingAiReturn()
                                    onShowMessage("Prompt copied! Paste in your AI app, copy the JSON reply, then return here.")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_prompt_button")
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.copy_prompt))
                        }

                        FilledTonalButton(
                            onClick = {
                                val clip = ExternalAiLauncher.readClipboardText(context).orEmpty()
                                if (clip.isNotBlank() && JsonPayloadParser.looksLikeStudyPayload(clip)) {
                                    onParseManualJson(clip)
                                } else {
                                    onClearImportError()
                                    showPasteDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_paste_clipboard_button")
                        ) {
                            Icon(Icons.Outlined.ContentPasteGo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Reply")
                        }
                    }

                    if (state.pendingAiImport) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Listening for JSON reply on return — copy the AI output and switch back.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item(key = "studio_bottom_spacer") {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showPasteDialog) {
        ManualJsonPasteDialog(
            initialError = state.importErrorMessage,
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
}

@Composable
private fun StudioSectionCard(
    stepBadge: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stepBadge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun QuestionCountStepperRow(
    label: String,
    helper: String,
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onQuickPick: (Int) -> Unit,
    quickValues: List<Int>,
    testTagPrefix: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = onDecrement,
                        enabled = count > 0,
                        modifier = Modifier.testTag("${testTagPrefix}_dec")
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease $label")
                    }
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .testTag("${testTagPrefix}_value")
                    )
                    FilledTonalIconButton(
                        onClick = onIncrement,
                        modifier = Modifier.testTag("${testTagPrefix}_inc")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase $label")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickValues.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (count == preset) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuickPick(preset) }
                            .testTag("${testTagPrefix}_preset_$preset")
                    ) {
                        Text(
                            text = "+$preset",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (count == preset) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedAiPillButton(
    label: String,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(50.dp)
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.5.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Send to $label",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun createTempCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "ocr_captures").apply { mkdirs() }
    val photoFile = File(dir, "upsc_note_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}
