package com.dewijones.linguasupra.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewijones.linguasupra.data.Language
import com.dewijones.linguasupra.data.PresetLanguage
import com.dewijones.linguasupra.ui.home.settingsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory(context))
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val nanoEnabled by viewModel.nanoEnabled.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<com.dewijones.linguasupra.data.DatabaseImporter.Result?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importDatabase(uri) { result ->
                importResult = result
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add language") },
            )
        },
    ) { padding ->
        SettingsContent(
            languages = languages,
            nanoEnabled = nanoEnabled,
            onSetNanoEnabled = viewModel::setNanoEnabled,
            onIncrementQuota = { viewModel.updateQuota(it, it.dailyQuota + 1) },
            onDecrementQuota = { viewModel.updateQuota(it, it.dailyQuota - 1) },
            onDelete = { viewModel.delete(it.id) },
            onImportDatabase = { showImportConfirm = true },
            contentPadding = padding,
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Import database file?") },
            text = {
                Text(
                    "This replaces your current lessons, languages and history with " +
                        "whatever is in the .db file you pick. The app will close after " +
                        "import — re-open it to see the restored data. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    // SQLite files have no MIME type the SAF picker reliably honours;
                    // accept everything and validate inside DatabaseImporter.
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Pick file…") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") }
            },
        )
    }

    importResult?.let { result ->
        val activity = context as? Activity
        AlertDialog(
            onDismissRequest = {
                importResult = null
                if (result is com.dewijones.linguasupra.data.DatabaseImporter.Result.Success) {
                    activity?.finishAndRemoveTask()
                }
            },
            title = {
                Text(
                    when (result) {
                        is com.dewijones.linguasupra.data.DatabaseImporter.Result.Success -> "Imported ✓"
                        is com.dewijones.linguasupra.data.DatabaseImporter.Result.Failure -> "Import failed"
                    },
                )
            },
            text = {
                Text(
                    when (result) {
                        is com.dewijones.linguasupra.data.DatabaseImporter.Result.Success ->
                            "The app will close now. Re-open it and your data should be back."
                        is com.dewijones.linguasupra.data.DatabaseImporter.Result.Failure -> result.message
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val wasSuccess = result is com.dewijones.linguasupra.data.DatabaseImporter.Result.Success
                    importResult = null
                    if (wasSuccess) activity?.finishAndRemoveTask()
                }) {
                    Text(
                        if (result is com.dewijones.linguasupra.data.DatabaseImporter.Result.Success) "Close app"
                        else "OK",
                    )
                }
            },
        )
    }

    if (showAddSheet) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheet,
        ) {
            AddLanguageSheetContent(
                presets = viewModel.presets,
                existingNames = languages.map { it.name }.toSet(),
                onPick = { preset ->
                    viewModel.add(preset)
                    showAddSheet = false
                },
            )
        }
    }
}

@Composable
private fun SettingsContent(
    languages: List<Language>,
    nanoEnabled: Boolean,
    onSetNanoEnabled: (Boolean) -> Unit,
    onIncrementQuota: (Language) -> Unit,
    onDecrementQuota: (Language) -> Unit,
    onDelete: (Language) -> Unit,
    onImportDatabase: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "reminders-header") {
                ReminderSettingsCard(
                    nanoEnabled = nanoEnabled,
                    onSetNanoEnabled = onSetNanoEnabled,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Languages",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            if (languages.isEmpty()) {
                item(key = "empty-languages") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No languages yet 🌱", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap the + button to add your first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(languages, key = { it.id }) { lang ->
                    LanguageRowSettings(
                        language = lang,
                        onIncrement = { onIncrementQuota(lang) },
                        onDecrement = { onDecrementQuota(lang) },
                        onDelete = { onDelete(lang) },
                    )
                }
            }
            item(key = "data-migration") {
                Spacer(modifier = Modifier.height(12.dp))
                DataMigrationCard(onImportDatabase = onImportDatabase)
                Spacer(modifier = Modifier.height(80.dp)) // breathing room above the FAB
            }
        }
    }
}

@Composable
private fun DataMigrationCard(onImportDatabase: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "📥 Restore data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Import a LinguaSupra database file (e.g. an export from the previous .debug install). Replaces your current data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onImportDatabase) {
                Text("Import .db file…")
            }
        }
    }
}

@Composable
private fun ReminderSettingsCard(
    nanoEnabled: Boolean,
    onSetNanoEnabled: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "✨ AI-written reminder copy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Use on-device Gemini Nano (Pixel 8 Pro+ class) for varied nudges. Off ⇒ curated copy only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(checked = nanoEnabled, onCheckedChange = onSetNanoEnabled)
        }
    }
}

@Composable
private fun LanguageRowSettings(
    language: Language,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${language.flagEmoji}${language.vibeEmoji}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = language.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${language.dailyQuota} per day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDecrement, enabled = language.dailyQuota > 0) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease quota")
            }
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.Add, contentDescription = "Increase quota")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${language.name}")
            }
        }
    }
}

@Composable
private fun AddLanguageSheetContent(
    presets: List<PresetLanguage>,
    existingNames: Set<String>,
    onPick: (PresetLanguage) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Pick a language", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(presets, key = { it.name }) { preset ->
                PresetRow(
                    preset = preset,
                    alreadyAdded = preset.name in existingNames,
                    onPick = { onPick(preset) },
                )
            }
        }
    }
}

@Composable
private fun PresetRow(preset: PresetLanguage, alreadyAdded: Boolean, onPick: () -> Unit) {
    val container = if (alreadyAdded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${preset.flagEmoji}${preset.vibeEmoji}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = preset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = if (alreadyAdded) "Already in your list" else "${preset.defaultDailyQuota} per day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPick, enabled = !alreadyAdded) {
                Icon(Icons.Filled.Add, contentDescription = "Add ${preset.name}")
            }
        }
    }
}
