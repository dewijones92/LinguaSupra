package com.dewijones.linguasupra.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dewijones.linguasupra.data.LanguageProgress
import com.dewijones.linguasupra.data.SelfieStorage
import com.dewijones.linguasupra.ui.components.EmojiConfetti
import com.dewijones.linguasupra.ui.components.LanguageProgressRing
import com.dewijones.linguasupra.ui.theme.DisplayBagel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = homeViewModelFactory(context))
    val progress by viewModel.state.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val selfiePath by viewModel.selfiePath.collectAsStateWithLifecycle()
    val onboarding by viewModel.onboardingState.collectAsStateWithLifecycle()
    val showCelebration by viewModel.showCelebration.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()

    val retakeLauncher = rememberSelfieLauncher(prefix = "retake") { path ->
        viewModel.setSelfiePath(path)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LinguaSupra ✨",
                        style = DisplayBagel.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Stats")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        HomeContent(
            progress = progress,
            userName = userName,
            selfiePath = selfiePath,
            streak = streak,
            onPlusOne = viewModel::recordCompletion,
            onAvatarTap = retakeLauncher,
            contentPadding = padding,
        )
    }

    when (val state = onboarding) {
        OnboardingState.Hidden -> Unit
        OnboardingState.AskingName -> FirstLaunchNameDialog(onSubmit = viewModel::setUserName)
        is OnboardingState.AskingSelfie -> OnboardingSelfieDialog(
            name = state.name,
            onCaptured = { path ->
                viewModel.setSelfiePath(path)
                viewModel.completeOnboarding()
            },
            onSkip = viewModel::completeOnboarding,
        )
    }

    if (showCelebration) {
        CelebrationDialog(
            userName = userName,
            onCaptured = { path ->
                viewModel.setSelfiePath(path)
                viewModel.dismissCelebration()
            },
            onSkip = viewModel::dismissCelebration,
        )
    }
}

@Composable
private fun FirstLaunchNameDialog(onSubmit: (String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { /* blocking */ },
        confirmButton = {
            TextButton(
                onClick = { if (draft.isNotBlank()) onSubmit(draft) },
                enabled = draft.isNotBlank(),
            ) { Text("Let's go!") }
        },
        title = { Text("👋 What should I call you?") },
        text = {
            Column {
                Text(
                    "I'll use this for greetings and reminder messages so it feels less robotic.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(40) },
                    label = { Text("Your name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        },
    )
}

@Composable
private fun OnboardingSelfieDialog(
    name: String,
    onCaptured: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val launchCamera = rememberSelfieLauncher(prefix = "onboarding", onCaptured = onCaptured)
    AlertDialog(
        onDismissRequest = { /* blocking */ },
        confirmButton = {
            TextButton(onClick = launchCamera) { Text("Smile! 📸") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Maybe later 🙈") }
        },
        title = { Text("Show us that face, $name! 🤳") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "I'll greet you with it every time. Don't be shy 😉",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                BouncingSelfieEmoji()
            }
        },
    )
}

@Composable
private fun CelebrationDialog(
    userName: String?,
    onCaptured: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val launchCamera = rememberSelfieLauncher(prefix = "celebration", onCaptured = onCaptured)
    val who = userName?.takeIf { it.isNotBlank() } ?: "you legend"
    AlertDialog(
        onDismissRequest = onSkip,
        confirmButton = {
            TextButton(onClick = launchCamera) { Text("Snap a winner pic 📸✨") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Just close this 🙈") }
        },
        title = { Text("🎉 You did it, $who!") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Every quota smashed. That's a winner-selfie kind of day.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("🥳🎊🏆", style = MaterialTheme.typography.displayMedium)
            }
        },
    )
}

@Composable
private fun BouncingSelfieEmoji() {
    val transition = rememberInfiniteTransition(label = "selfieBounce")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text("🤳", fontSize = 72.sp)
    }
}

@Composable
private fun rememberSelfieLauncher(
    prefix: String,
    onCaptured: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingFile
        if (success && file != null && file.length() > 0) onCaptured(file.absolutePath)
    }
    return {
        val file = SelfieStorage.newSelfieFile(context, prefix = prefix)
        pendingFile = file
        runCatching { launcher.launch(SelfieStorage.uriFor(context, file)) }
    }
}

@Composable
private fun HomeContent(
    progress: List<LanguageProgress>,
    userName: String?,
    selfiePath: String?,
    streak: Int,
    onPlusOne: (Long) -> Unit,
    onAvatarTap: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Greeting(
                    userName = userName,
                    selfiePath = selfiePath,
                    onAvatarTap = onAvatarTap,
                )
            }
            if (streak > 0) {
                item { StreakBadge(streak = streak) }
            }
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
private fun StreakBadge(streak: Int) {
    val flames = "🔥".repeat(streak.coerceAtMost(7))
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = flames,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = if (streak == 1) "1 day streak" else "$streak day streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Greeting(
    userName: String?,
    selfiePath: String?,
    onAvatarTap: () -> Unit,
) {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
    val greeting = if (userName.isNullOrBlank()) "Hi 👋" else "Hi $userName 👋"
    Row(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelfieAvatar(
            selfiePath = selfiePath,
            name = userName,
            size = 72.dp,
            onClick = onAvatarTap,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(greeting, style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                today,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelfieAvatar(
    selfiePath: String?,
    name: String?,
    size: Dp,
    onClick: () -> Unit,
) {
    val bitmap = remember(selfiePath) {
        if (selfiePath.isNullOrBlank()) {
            null
        } else {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(selfiePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Your selfie — tap to retake",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
            if (initial != null) {
                Text(
                    initial,
                    style = DisplayBagel.copy(fontSize = (size.value * 0.5f).sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Text("👋", fontSize = (size.value * 0.45f).sp)
            }
        }
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
