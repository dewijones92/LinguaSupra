package com.dewijones.linguasupra.notify

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dewijones.linguasupra.MainActivity
import com.dewijones.linguasupra.R
import com.dewijones.linguasupra.data.LanguageProgress
import com.dewijones.linguasupra.data.Repository
import kotlinx.coroutines.flow.first

class BannerNotificationManager(
    private val context: Context,
    private val repository: Repository,
) {

    suspend fun refresh() {
        Notifications.ensureChannels(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val progress = repository.observeTodayProgress().first().sortedBy { it.displayOrder }
        nm.notify(Notifications.BANNER_NOTIFICATION_ID, build(progress))
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(Notifications.BANNER_NOTIFICATION_ID)
    }

    fun notificationFor(progress: List<LanguageProgress>): Notification =
        build(progress.sortedBy { it.displayOrder })

    fun placeholderNotification(): Notification {
        Notifications.ensureChannels(context)
        return NotificationCompat.Builder(context, Notifications.BANNER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(context.getString(R.string.banner_title))
            .setContentText("…")
            .setContentIntent(launchAppIntent())
            .setDeleteIntent(BannerDismissReceiver.pendingIntent(context))
            .build()
    }

    private fun build(progress: List<LanguageProgress>): Notification {
        val pkg = context.packageName
        // Same rich layout for collapsed and expanded so the banner reads as
        // "always expanded" — the platform clamps the collapsed view height,
        // but with a small per-language list the top rows stay visible.
        val view = buildView(pkg, progress)
        return NotificationCompat.Builder(context, Notifications.BANNER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentTitle(context.getString(R.string.banner_title))
            .setContentText(buildSummary(progress))
            .setCustomContentView(view)
            .setCustomBigContentView(view)
            .setContentIntent(launchAppIntent())
            .setDeleteIntent(BannerDismissReceiver.pendingIntent(context))
            .build()
    }

    private fun buildSummary(progress: List<LanguageProgress>): String =
        if (progress.isEmpty()) {
            context.getString(R.string.banner_empty)
        } else {
            progress.joinToString("  ·  ") {
                "${it.flagEmoji}${it.vibeEmoji} ${it.name} ${it.completedToday}/${it.dailyQuota}"
            }
        }

    private fun buildView(pkg: String, progress: List<LanguageProgress>): RemoteViews {
        val container = RemoteViews(pkg, R.layout.banner_expanded)
        val (moodEmoji, greeting) = greetingFor(progress)
        container.setImageViewBitmap(R.id.banner_mood, EmojiBitmapFactory.render(moodEmoji, heightPx = 96))
        container.setTextViewText(R.id.banner_greeting, greeting)
        val rows = if (progress.isEmpty()) listOf(emptyRow(pkg)) else progress.map { row(pkg, it) }
        rows.forEach { container.addView(R.id.banner_rows_container, it) }
        return container
    }

    private fun greetingFor(progress: List<LanguageProgress>): Pair<String, String> {
        if (progress.isEmpty()) return "👋" to context.getString(R.string.banner_empty)
        val totalCompleted = progress.sumOf { it.completedToday }
        val totalQuota = progress.sumOf { it.dailyQuota }.coerceAtLeast(1)
        val ratio = totalCompleted.toFloat() / totalQuota
        return when {
            progress.all { it.isComplete } -> "🎉" to "Smashed it! All done for today."
            ratio >= 0.66f -> "🔥" to "Nearly there — keep going!"
            ratio >= 0.33f -> "💪" to "Strong start. Don't stop now."
            totalCompleted > 0 -> "✨" to "You're rolling. Keep at it."
            else -> "👋" to "Today's plan, ready when you are."
        }
    }

    private fun row(pkg: String, lp: LanguageProgress): RemoteViews =
        RemoteViews(pkg, R.layout.banner_row).apply {
            setImageViewBitmap(R.id.row_vibe, EmojiBitmapFactory.render("${lp.flagEmoji}${lp.vibeEmoji}", heightPx = 132))
            setInt(R.id.row_accent, "setBackgroundColor", BannerAccents.colorFor(lp))
            setTextViewText(R.id.row_name, "${lp.name}${if (lp.isComplete) "  ✓" else ""}")
            val percent = if (lp.dailyQuota > 0) {
                ((lp.completedToday.toFloat() / lp.dailyQuota) * 100).toInt().coerceIn(0, 100)
            } else 0
            setProgressBar(R.id.row_progress, 100, percent, false)
            setTextViewText(R.id.row_count, "${lp.completedToday}/${lp.dailyQuota}")

            setOnClickPendingIntent(
                R.id.row_plus,
                CompletionReceiver.pendingIntent(context, lp),
            )
            setContentDescription(
                R.id.row_plus,
                context.getString(R.string.banner_plus_one_cd_for, lp.name),
            )
        }

    private fun emptyRow(pkg: String): RemoteViews =
        RemoteViews(pkg, R.layout.banner_row).apply {
            setImageViewBitmap(R.id.row_vibe, EmojiBitmapFactory.render("👋", heightPx = 132))
            setInt(R.id.row_accent, "setBackgroundColor", android.graphics.Color.parseColor("#FF7043"))
            setTextViewText(R.id.row_name, context.getString(R.string.banner_empty))
            setTextViewText(R.id.row_count, "")
            setProgressBar(R.id.row_progress, 100, 0, false)
            setViewVisibility(R.id.row_plus, View.GONE)
        }

    private fun launchAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
