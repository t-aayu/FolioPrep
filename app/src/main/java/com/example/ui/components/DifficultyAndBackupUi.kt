package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LocalBackupManager
import com.example.domain.QuestionDifficulty

@Composable
fun DifficultyBadge(
    difficulty: String,
    modifier: Modifier = Modifier
) {
    val normalized = QuestionDifficulty.fromRaw(difficulty)
    val containerColor = when (normalized) {
        QuestionDifficulty.EASY -> MaterialTheme.colorScheme.primaryContainer
        QuestionDifficulty.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        QuestionDifficulty.HARD -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (normalized) {
        QuestionDifficulty.EASY -> MaterialTheme.colorScheme.onPrimaryContainer
        QuestionDifficulty.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
        QuestionDifficulty.HARD -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = normalized.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Zero-permission Storage Access Framework file picker pre-hinted to Documents/FolioPrep
 * so users can select folioprep_backup.json immediately after reinstalling the app.
 */
class OpenBackupDocumentContract : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalBackupManager.buildInitialDocumentsFolderUri()?.let { initialUri ->
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                }
            }
        }
    }
}

/**
 * Zero-permission SAF folder picker for letting users choose a custom Android folder
 * for automatic local backups.
 */
class OpenBackupFolderTreeContract : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }
}
