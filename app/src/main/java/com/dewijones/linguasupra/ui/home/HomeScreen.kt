package com.dewijones.linguasupra.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewijones.linguasupra.data.LanguageProgress
import com.dewijones.linguasupra.ui.components.EmojiConfetti
import com.dewijones.linguasupra.ui.components.LanguageProgressRing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = homeViewModelFactory(context))
    val progress by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("LinguaSupra ✨", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        HomeContent(
            progress = progress,
            onPlusOne = viewModel::recordCompletion,
            contentPadding = padding,
        )
    }
}

@Composable
private fun HomeContent(
    progress: List<LanguageProgress>,
    onPlusOne: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Greeting() }
            if (progress.isEmpty()) {
                item { EmptyHomeCard() }
            } else {
                items(progress, key = { it.languageId }) { lp ->
                    LanguageRow(progress = lp, onPlusOne = { onPlusOne(lp.languageId) })
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun Greeting() {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Text("Hi 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(today, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LanguageRow(progress: LanguageProgress, onPlusOne: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var confettiTrigger by remember { mutableIntStateOf(0) }
    val cardScaleTarget = if (progress.isComplete) 1.02f else 1f
    val cardScale by animateFloatAsState(
        targetValue = cardScaleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "completeCardScale",
    )
    val buttonScaleTarget = remember(confettiTrigger) { 1.15f }
    val buttonScale by animateFloatAsState(
        targetValue = if (confettiTrigger == 0) 1f else buttonScaleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "buttonPulse",
    )
    Box {
        Card(
            modifier = Modifier.fillMaxWidth().scale(cardScale),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (progress.isComplete) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageProgressRing(
                    fraction = progress.progressFraction,
                    emoji = progress.vibeEmoji,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${progress.flagEmoji} ${progress.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${progress.completedToday} / ${progress.dailyQuota}" +
                            if (progress.isComplete) "  ✓" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (progress.isComplete && progress.motivationPhrase != null) {
                        Text(
                            text = progress.motivationPhrase,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        confettiTrigger += 1
                        onPlusOne()
                    },
                    modifier = Modifier.size(56.dp).scale(buttonScale),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Mark one ${progress.name} lesson done")
                }
            }
        }
        // Burst originates near the centre of the card; particles fly outward
        EmojiConfetti(
            trigger = confettiTrigger,
            emoji = progress.vibeEmoji,
            originX = 200.dp,
            originY = 40.dp,
        )
    }
}

@Composable
private fun EmptyHomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Let's pick some languages 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Open Settings → Add language to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
