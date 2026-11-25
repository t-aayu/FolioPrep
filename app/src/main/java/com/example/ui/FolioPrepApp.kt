package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.ImportPreviewScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.McqSessionMode
import com.example.ui.screens.McqSessionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.SubjectiveFlashcardScreen
import com.example.viewmodel.FolioPrepViewModel

object Routes {
    const val LIBRARY = "library"
    const val STUDIO = "studio"
    const val SETTINGS = "settings"
    const val IMPORT_PREVIEW = "import_preview"
    const val DECK_DETAIL = "deck/{deckId}"
    const val QUICK_SOLVE = "quick_solve/{deckId}/{startIndex}/{difficulty}"
    const val PRACTICE = "practice/{deckId}/{difficulty}"
    const val TIMED_EXAM = "timed_exam/{deckId}/{durationMinutes}/{negativeFactor}/{difficulty}"
    const val REVISE_BY_DIFFICULTY = "revise_difficulty/{difficulty}"
    const val FLASHCARDS = "flashcards/{deckId}"

    fun deckDetail(deckId: Long) = "deck/$deckId"
    fun quickSolve(deckId: Long, startIndex: Int = 0, difficulty: String = "ALL") =
        "quick_solve/$deckId/$startIndex/$difficulty"
    fun practice(deckId: Long, difficulty: String = "ALL") = "practice/$deckId/$difficulty"
    fun timedExam(deckId: Long, durationMinutes: Int, negativeFactor: Double, difficulty: String = "ALL") =
        "timed_exam/$deckId/$durationMinutes/$negativeFactor/$difficulty"
    fun reviseByDifficulty(difficulty: String) = "revise_difficulty/$difficulty"
    fun flashcards(deckId: Long) = "flashcards/$deckId"
}

@Composable
fun FolioPrepApp(viewModel: FolioPrepViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val examYearConfig by viewModel.examYearConfig.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val filteredDecks by viewModel.filteredDecks.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allMcqs by viewModel.allMcqs.collectAsStateWithLifecycle()
    val showFreshRestorePrompt by viewModel.shouldShowFreshRestorePrompt.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableLibraryTags.collectAsStateWithLifecycle()
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    val quickAskState by viewModel.quickAskState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.LIBRARY
    val isTopLevelDestination =
        currentRoute == Routes.LIBRARY || currentRoute == Routes.STUDIO || currentRoute == Routes.SETTINGS

    LaunchedEffect(studioState.transientToastMessage) {
        val msg = studioState.transientToastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    fun navigateToTopLevel(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(Routes.LIBRARY) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(studioState.pendingNavigateToStudio) {
        if (studioState.pendingNavigateToStudio) {
            if (currentRoute != Routes.STUDIO) {
                navController.navigate(Routes.STUDIO) {
                    popUpTo(Routes.LIBRARY) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            viewModel.consumeNavigateToStudio()
        }
    }

    fun openImportPreviewIfReady(parsedOk: Boolean) {
        if (parsedOk && currentRoute != Routes.IMPORT_PREVIEW) {
            navController.navigate(Routes.IMPORT_PREVIEW)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (isTopLevelDestination && !useNavigationRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = currentRoute == Routes.LIBRARY,
                            onClick = { navigateToTopLevel(Routes.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == Routes.LIBRARY) Icons.Filled.Book else Icons.Outlined.Book,
                                    contentDescription = stringResource(R.string.nav_library)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_library)) },
                            modifier = Modifier.testTag("nav_library")
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.STUDIO,
                            onClick = { navigateToTopLevel(Routes.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == Routes.STUDIO) Icons.Filled.AutoFixHigh else Icons.Outlined.AutoFixHigh,
                                    contentDescription = stringResource(R.string.nav_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            modifier = Modifier.testTag("nav_studio")
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.SETTINGS,
                            onClick = { navigateToTopLevel(Routes.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == Routes.LIBRARY) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openQuickAskSheet() },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "Open Quick Ask AI"
                            )
                        },
                        text = { Text("Quick Ask AI") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("quick_ask_ai_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelDestination && useNavigationRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationRailItem(
                            selected = currentRoute == Routes.LIBRARY,
                            onClick = { navigateToTopLevel(Routes.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == Routes.LIBRARY) Icons.Filled.Book else Icons.Outlined.Book,
                                    contentDescription = stringResource(R.string.nav_library)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_library)) },
                            modifier = Modifier.testTag("rail_library")
                        )
                        NavigationRailItem(
                            selected = currentRoute == Routes.STUDIO,
                            onClick = { navigateToTopLevel(Routes.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == Routes.STUDIO) Icons.Filled.AutoFixHigh else Icons.Outlined.AutoFixHigh,
                                    contentDescription = stringResource(R.string.nav_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            modifier = Modifier.testTag("rail_studio")
                        )
                        NavigationRailItem(
                            selected = currentRoute == Routes.SETTINGS,
                            onClick = { navigateToTopLevel(Routes.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            modifier = Modifier.testTag("rail_settings")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.LIBRARY
                    ) {
                        composable(Routes.LIBRARY) {
                            val prelimsDeckCount = remember(allDecks) { allDecks.count { it.mcqCount > 0 } }
                            val mainsDeckCount = remember(allDecks) { allDecks.count { it.subjectiveCount > 0 } }
                            LibraryScreen(
                                examYearConfig = examYearConfig,
                                decks = filteredDecks,
                                totalDeckCount = allDecks.size,
                                prelimsDeckCount = prelimsDeckCount,
                                mainsDeckCount = mainsDeckCount,
                                allMcqs = allMcqs,
                                filterState = filterState,
                                availableTags = availableTags,
                                detectedClipboard = studioState.detectedClipboardBanner,
                                importErrorMessage = studioState.importErrorMessage,
                                showFreshRestorePrompt = showFreshRestorePrompt,
                                quickAskState = quickAskState,
                                installedAiApps = studioState.installedAiApps,
                                onRefreshInstalledApps = viewModel::refreshInstalledAiApps,
                                onDismissQuickAskSheet = viewModel::closeQuickAskSheet,
                                onQuickAskKeywordsChange = viewModel::updateQuickAskKeywords,
                                onQuickAskTypeChange = viewModel::selectQuickAskType,
                                onQuickAskDifficultyChange = viewModel::selectQuickAskDifficulty,
                                onQuickAskCountAdjust = viewModel::adjustQuickAskCount,
                                onPrepareQuickAskPrompt = viewModel::prepareQuickAskPrompt,
                                onShowMessage = viewModel::showToast,
                                onUpdatePrelimsYear = viewModel::updatePrelimsYear,
                                onUpdateMainsYear = viewModel::updateMainsYear,
                                onCategorySelected = viewModel::onCategorySelected,
                                onSearchQueryChange = viewModel::onSearchQueryChanged,
                                onTagSelected = viewModel::onTagFilterSelected,
                                onOpenDeck = { deckId ->
                                    navController.navigate(Routes.deckDetail(deckId))
                                },
                                onDeleteDeck = viewModel::deleteDeck,
                                onOpenStudio = { navigateToTopLevel(Routes.STUDIO) },
                                onReviseByDifficulty = { difficulty ->
                                    navController.navigate(Routes.reviseByDifficulty(difficulty))
                                },
                                onRestoreFromBackupUri = viewModel::restoreFromBackupUri,
                                onDismissFreshRestorePrompt = viewModel::dismissFreshRestorePrompt,
                                onReviewClipboardBanner = {
                                    openImportPreviewIfReady(viewModel.openDetectedClipboardForPreview())
                                },
                                onDismissClipboardBanner = viewModel::dismissClipboardBanner,
                                onParseManualJson = { raw, fromQuickAsk ->
                                    val ok = viewModel.parseAndOpenManualPayload(raw, fromQuickAsk)
                                    openImportPreviewIfReady(ok)
                                    ok
                                },
                                onClearImportError = viewModel::clearImportError
                            )
                        }

                        composable(Routes.STUDIO) {
                            StudioScreen(
                                state = studioState,
                                examYearConfig = examYearConfig,
                                appSettings = appSettings,
                                onRefreshInstalledApps = viewModel::refreshInstalledAiApps,
                                onTryAutoPasteClipboard = viewModel::tryAutoPasteClipboard,
                                onSelectInputTab = viewModel::selectInputTab,
                                onUrlInputChange = viewModel::updateUrlInput,
                                onExtractArticle = viewModel::extractArticleFromUrl,
                                onRunOcrOnImages = viewModel::runOcrOnImages,
                                onSourceTextChange = viewModel::updateSourceText,
                                onClearSourceText = viewModel::clearSourceText,
                                onSelectChunk = viewModel::selectChunk,
                                onSelectSubjectFocus = viewModel::selectSubjectFocus,
                                onSelectQuestionMode = viewModel::selectQuestionMode,
                                onSelectDifficulty = viewModel::selectDifficulty,
                                onSelectMcqTypeOption = viewModel::selectMcqTypeOption,
                                onCustomMcqTypeTextChange = viewModel::updateCustomMcqTypeText,
                                onRotateAutoMcqType = viewModel::rotateAutoMcqType,
                                onAdjustMcqCount = viewModel::adjustMcqCount,
                                onAdjustMains10Count = viewModel::adjustMains10Count,
                                onAdjustMains15Count = viewModel::adjustMains15Count,
                                onPromptTextChange = viewModel::updatePromptManually,
                                onResetPromptToTemplate = viewModel::resetPromptToTemplate,
                                onMarkAwaitingAiReturn = viewModel::markAwaitingAiReturn,
                                onReviewClipboardBanner = {
                                    openImportPreviewIfReady(viewModel.openDetectedClipboardForPreview())
                                },
                                onDismissClipboardBanner = viewModel::dismissClipboardBanner,
                                onParseManualJson = { raw ->
                                    openImportPreviewIfReady(viewModel.parseAndOpenManualPayload(raw))
                                },
                                onClearImportError = viewModel::clearImportError,
                                onShowMessage = viewModel::showToast
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settings = appSettings,
                                onSelectThemeMode = viewModel::updateThemeMode,
                                onToggleUseCustomSkill = viewModel::updateUseCustomSkill,
                                onSaveCustomSkill = viewModel::saveCustomSkillText,
                                onToggleAutoBackup = viewModel::updateAutoBackupEnabled,
                                onBackupNow = viewModel::backupNow,
                                onChooseBackupFolder = viewModel::setBackupFolderUri,
                                onRestoreFromBackupUri = viewModel::restoreFromBackupUri
                            )
                        }

                        composable(Routes.IMPORT_PREVIEW) {
                            ImportPreviewScreen(
                                draft = studioState.draftImport,
                                onBack = {
                                    viewModel.clearDraftImport()
                                    navController.popBackStack()
                                },
                                onUpdateTitle = viewModel::updateDraftTitle,
                                onToggleTag = viewModel::toggleDraftTag,
                                onAddCustomTag = viewModel::addCustomDraftTag,
                                onUpdateMcq = viewModel::updateDraftMcq,
                                onDeleteMcq = viewModel::deleteDraftMcq,
                                onUpdateSubjective = viewModel::updateDraftSubjective,
                                onDeleteSubjective = viewModel::deleteDraftSubjective,
                                onSaveDeck = {
                                    viewModel.saveDraftDeckToLibrary { savedDeckId ->
                                        navController.navigate(Routes.deckDetail(savedDeckId)) {
                                            popUpTo(Routes.LIBRARY) { inclusive = false }
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.DECK_DETAIL,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: -1L
                            val detailFlow = remember(deckId) { viewModel.observeDeckDetail(deckId) }
                            val bundle by detailFlow.collectAsStateWithLifecycle(initialValue = null)

                            DeckDetailScreen(
                                bundle = bundle,
                                onBack = { navController.popBackStack() },
                                onStartQuickSolve = { id, startIdx, difficulty ->
                                    navController.navigate(Routes.quickSolve(id, startIdx, difficulty))
                                },
                                onStartPractice = { id, difficulty ->
                                    navController.navigate(Routes.practice(id, difficulty))
                                },
                                onStartTimedExam = { id, mins, neg, difficulty ->
                                    navController.navigate(Routes.timedExam(id, mins, neg, difficulty))
                                },
                                onStartSubjectiveFlashcards = { id ->
                                    navController.navigate(Routes.flashcards(id))
                                }
                            )
                        }

                        composable(
                            route = Routes.QUICK_SOLVE,
                            arguments = listOf(
                                navArgument("deckId") { type = NavType.LongType },
                                navArgument("startIndex") { type = NavType.IntType },
                                navArgument("difficulty") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: -1L
                            val startIndex = entry.arguments?.getInt("startIndex") ?: 0
                            val difficulty = entry.arguments?.getString("difficulty") ?: "ALL"
                            val detailFlow = remember(deckId) { viewModel.observeDeckDetail(deckId) }
                            val bundle by detailFlow.collectAsStateWithLifecycle(initialValue = null)

                            McqSessionScreen(
                                bundle = bundle,
                                sessionMode = McqSessionMode.QUICK_SOLVE,
                                initialQuestionIndex = startIndex,
                                difficultyFilter = difficulty,
                                onBack = { navController.popBackStack() },
                                onShowMessage = viewModel::showToast
                            )
                        }

                        composable(
                            route = Routes.PRACTICE,
                            arguments = listOf(
                                navArgument("deckId") { type = NavType.LongType },
                                navArgument("difficulty") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: -1L
                            val difficulty = entry.arguments?.getString("difficulty") ?: "ALL"
                            val detailFlow = remember(deckId) { viewModel.observeDeckDetail(deckId) }
                            val bundle by detailFlow.collectAsStateWithLifecycle(initialValue = null)

                            McqSessionScreen(
                                bundle = bundle,
                                sessionMode = McqSessionMode.PRACTICE,
                                durationMinutes = 15,
                                negativeFactor = 0.0,
                                difficultyFilter = difficulty,
                                onBack = { navController.popBackStack() },
                                onShowMessage = viewModel::showToast,
                                onCompleteAttempt = viewModel::recordQuizAttempt
                            )
                        }

                        composable(
                            route = Routes.TIMED_EXAM,
                            arguments = listOf(
                                navArgument("deckId") { type = NavType.LongType },
                                navArgument("durationMinutes") { type = NavType.IntType },
                                navArgument("negativeFactor") { type = NavType.StringType },
                                navArgument("difficulty") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: -1L
                            val durationMinutes = entry.arguments?.getInt("durationMinutes") ?: 10
                            val negativeFactor = entry.arguments?.getString("negativeFactor")?.toDoubleOrNull() ?: 0.3333
                            val difficulty = entry.arguments?.getString("difficulty") ?: "ALL"
                            val detailFlow = remember(deckId) { viewModel.observeDeckDetail(deckId) }
                            val bundle by detailFlow.collectAsStateWithLifecycle(initialValue = null)

                            McqSessionScreen(
                                bundle = bundle,
                                sessionMode = McqSessionMode.TIMED_EXAM,
                                durationMinutes = durationMinutes,
                                negativeFactor = negativeFactor,
                                difficultyFilter = difficulty,
                                onBack = { navController.popBackStack() },
                                onShowMessage = viewModel::showToast,
                                onCompleteAttempt = viewModel::recordQuizAttempt
                            )
                        }

                        composable(
                            route = Routes.REVISE_BY_DIFFICULTY,
                            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
                        ) { entry ->
                            val difficulty = entry.arguments?.getString("difficulty") ?: "Medium"
                            val mcqsFlow = remember(difficulty) { viewModel.observeMcqsByDifficulty(difficulty) }
                            val mcqs by mcqsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                            McqSessionScreen(
                                bundle = null,
                                sessionMode = McqSessionMode.PRACTICE,
                                difficultyFilter = difficulty,
                                standaloneMcqs = mcqs,
                                standaloneTitle = "All $difficulty MCQs (Library Revision)",
                                onBack = { navController.popBackStack() },
                                onShowMessage = viewModel::showToast
                            )
                        }

                        composable(
                            route = Routes.FLASHCARDS,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                        ) { entry ->
                            val deckId = entry.arguments?.getLong("deckId") ?: -1L
                            val detailFlow = remember(deckId) { viewModel.observeDeckDetail(deckId) }
                            val bundle by detailFlow.collectAsStateWithLifecycle(initialValue = null)

                            SubjectiveFlashcardScreen(
                                bundle = bundle,
                                onBack = { navController.popBackStack() },
                                onToggleMastered = viewModel::toggleSubjectiveMastery,
                                onShowMessage = viewModel::showToast
                            )
                        }
                    }
                }
            }
        }
    }
}
