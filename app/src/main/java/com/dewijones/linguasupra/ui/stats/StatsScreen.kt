package com.dewijones.linguasupra.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewijones.linguasupra.data.CompletionWithLanguage
import com.dewijones.linguasupra.data.DailySeriesPoint
import com.dewijones.linguasupra.notify.LanguagePalette
import com.dewijones.linguasupra.ui.theme.LinguaSupraTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: StatsViewModel = viewModel(factory = statsViewModelFactory(context))

    val series14 by vm.series14.collectAsStateWithLifecycle()
    val all by vm.allCompletions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CompletionWithLanguage?>(null) }
    var deleteTarget by remember { mutableStateOf<CompletionWithLanguage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Stats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add a past lesson")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ChartCard(series = series14) }
            item { TotalsCard(series = series14) }
            item {
                Text(
                    "Activity log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val grouped = all.groupBy { it.localDay() }.toSortedMap(reverseOrder())
            grouped.forEach { (day, rows) ->
                item(key = "header-$day") {
                    DayHeader(day = day, count = rows.size)
                }
                items(rows, key = { it.completion.id }) { row ->
                    CompletionRow(
                        row = row,
                        onEdit = { editTarget = row },
                        onDelete = { deleteTarget = row },
                    )
                }
            }

            if (all.isEmpty()) {
                item {
                    Card {
                        Text(
                            "No lessons logged yet. Tap + to add a past one, or use the banner to log live.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCompletionDialog(
            languages = remember(all) { all.map { it.language }.distinctBy { it.id } },
            onDismiss = { showAddDialog = false },
            onConfirm = { langId, instant ->
                vm.addManualCompletion(langId, instant)
                showAddDialog = false
            },
        )
    }

    editTarget?.let { row ->
        EditTimeDialog(
            current = Instant.ofEpochMilli(row.completion.completedAtEpochMs),
            onDismiss = { editTarget = null },
            onConfirm = { newInstant ->
                vm.updateCompletionTime(row.completion.id, newInstant)
                editTarget = null
            },
        )
    }

    deleteTarget?.let { row ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this lesson?") },
            text = {
                Text("${row.language.flagEmoji}${row.language.vibeEmoji} ${row.language.name} at ${row.timeLabel()}")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCompletion(row.completion.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ChartCard(series: List<DailySeriesPoint>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Last 14 days",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(12.dp))
            DailyBarChart(series = series, modifier = Modifier.fillMaxWidth().height(180.dp))
            Spacer(Modifier.height(8.dp))
            ChartLegend(series = series)
        }
    }
}

@Composable
private fun DailyBarChart(series: List<DailySeriesPoint>, modifier: Modifier = Modifier) {
    val byDay = series.groupBy { it.day }.toSortedMap()
    if (byDay.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data yet — log a lesson to see it here.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val maxTotal = (byDay.values.maxOf { it.sumOf { p -> p.count } }).coerceAtLeast(1)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val labelHeight = 18.dp.toPx()
        val chartH = h - labelHeight
        val cols = byDay.size
        val gap = 4.dp.toPx()
        val colW = (w - gap * (cols - 1)) / cols

        byDay.entries.forEachIndexed { idx, (day, points) ->
            val x = idx * (colW + gap)
            val total = points.sumOf { it.count }
            if (total == 0) {
                drawRect(
                    color = Color(0xFFE0E0E0),
                    topLeft = Offset(x, chartH - 2.dp.toPx()),
                    size = Size(colW, 2.dp.toPx()),
                )
            } else {
                var stackedHeight = 0f
                points.sortedBy { it.languageId }.forEach { p ->
                    if (p.count == 0) return@forEach
                    val segH = (p.count.toFloat() / maxTotal) * chartH
                    drawRect(
                        color = Color(LanguagePalette.colorForName(p.languageName)),
                        topLeft = Offset(x, chartH - stackedHeight - segH),
                        size = Size(colW, segH),
                    )
                    stackedHeight += segH
                }
            }

            // Day label
            val label = when (cols) {
                in 0..14 -> day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
                else -> day.dayOfMonth.toString()
            }
            val paint = android.graphics.Paint().apply {
                color = labelColor
                textSize = 11.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(label, x + colW / 2f, h - 4.dp.toPx(), paint)
        }
    }
}

@Composable
private fun ChartLegend(series: List<DailySeriesPoint>) {
    val languages = series.map { it.languageId to it.languageName }.distinct()
    if (languages.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        languages.forEach { (_, name) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = Color(LanguagePalette.colorForName(name)),
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
                )
                Spacer(Modifier.width(4.dp))
                Text(name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TotalsCard(series: List<DailySeriesPoint>) {
    val byLanguage = series.groupBy { it.languageId to it.languageName }
    if (byLanguage.isEmpty()) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Last 14 days, totals",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            byLanguage.forEach { (langKey, points) ->
                val (_, name) = langKey
                val total = points.sumOf { it.count }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = Color(LanguagePalette.colorForName(name)),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(name, fontWeight = FontWeight.Medium)
                    }
                    Text("$total", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, count: Int) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val label = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(
            "$count lesson${if (count == 1) "" else "s"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

@Composable
private fun CompletionRow(
    row: CompletionWithLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 32.dp)
                .background(Color(LanguagePalette.colorForName(row.language.name))),
        )
        Spacer(Modifier.width(12.dp))
        Text("${row.language.flagEmoji}${row.language.vibeEmoji}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.language.name, fontWeight = FontWeight.Bold)
            Text(row.timeLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit or delete")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit time") },
                    onClick = {
                        menu = false
                        onEdit()
                    },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCompletionDialog(
    languages: List<com.dewijones.linguasupra.data.Language>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Instant) -> Unit,
) {
    var selectedId by remember { mutableStateOf(languages.firstOrNull()?.id ?: -1L) }
    val now = remember { LocalDateTime.now(ZoneId.systemDefault()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = now.toEpoch())
    val timePickerState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute)
    var step by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when (step) { 0 -> "Pick language"; 1 -> "Pick day"; else -> "Pick time" }) },
        text = {
            when (step) {
                0 -> Column {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = lang.id == selectedId, onClick = { selectedId = lang.id })
                            Text("${lang.flagEmoji}${lang.vibeEmoji} ${lang.name}")
                        }
                    }
                }
                1 -> DatePicker(state = datePickerState)
                else -> TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (step < 2) step += 1 else {
                    val date = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: LocalDate.now()
                    val instant = LocalDateTime.of(
                        date,
                        LocalTime.of(timePickerState.hour, timePickerState.minute),
                    ).atZone(ZoneId.systemDefault()).toInstant()
                    onConfirm(selectedId, instant)
                }
            }) { Text(if (step < 2) "Next" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = if (step > 0) ({ step -= 1 }) else onDismiss) {
                Text(if (step > 0) "Back" else "Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTimeDialog(
    current: Instant,
    onDismiss: () -> Unit,
    onConfirm: (Instant) -> Unit,
) {
    val zoned = current.atZone(ZoneId.systemDefault())
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = current.toEpochMilli())
    val timePickerState = rememberTimePickerState(initialHour = zoned.hour, initialMinute = zoned.minute)
    var step by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 0) "Pick day" else "Pick time") },
        text = {
            if (step == 0) DatePicker(state = datePickerState) else TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = {
                if (step == 0) step = 1 else {
                    val date = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: zoned.toLocalDate()
                    val instant = LocalDateTime.of(
                        date,
                        LocalTime.of(timePickerState.hour, timePickerState.minute),
                    ).atZone(ZoneId.systemDefault()).toInstant()
                    onConfirm(instant)
                }
            }) { Text(if (step == 0) "Next" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = if (step > 0) ({ step = 0 }) else onDismiss) {
                Text(if (step > 0) "Back" else "Cancel")
            }
        },
    )
}

private fun CompletionWithLanguage.localDay(): LocalDate =
    Instant.ofEpochMilli(completion.completedAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

private fun CompletionWithLanguage.timeLabel(): String =
    Instant.ofEpochMilli(completion.completedAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

private fun LocalDateTime.toEpoch(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Preview
@Composable
private fun StatsScreenPreview() {
    LinguaSupraTheme { StatsScreen(onBack = {}) }
}
