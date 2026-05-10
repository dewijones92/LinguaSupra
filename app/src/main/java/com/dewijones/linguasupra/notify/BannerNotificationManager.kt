package com.dewijones.linguasupra.notify

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        // Inline permission check (lint doesn't track through helper methods).
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

    private fun build(progress: List<LanguageProgress>): Notification {
        val pkg = context.packageName
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
            .setCustomContentView(buildCollapsed(pkg, progress))
            .setCustomBigContentView(buildExpanded(pkg, progress))
            .setContentIntent(launchAppIntent())
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

    private fun buildCollapsed(pkg: String, progress: List<LanguageProgress>): RemoteViews =
        RemoteViews(pkg, R.layout.banner_collapsed).apply {
            setTextViewText(R.id.banner_summary, buildSummary(progress))
        }

    private fun buildExpanded(pkg: String, progress: List<LanguageProgress>): RemoteViews {
        val container = RemoteViews(pkg, R.layout.banner_expanded)
        val rows = if (progress.isEmpty()) {
            listOf(emptyRow(pkg))
        } else {
            progress.map { row(pkg, it) }
        }
        rows.forEach { container.addView(R.id.banner_rows_container, it) }
        return container
    }

    private fun row(pkg: String, lp: LanguageProgress): RemoteViews = bannerRow(
        pkg = pkg,
        emojis = "${lp.flagEmoji}${lp.vibeEmoji}",
        label = "${lp.name} ${lp.completedToday}/${lp.dailyQuota}${if (lp.isComplete) " ✓" else ""}",
        plusFor = lp,
    )

    private fun emptyRow(pkg: String): RemoteViews = bannerRow(
        pkg = pkg,
        emojis = "👋",
        label = context.getString(R.string.banner_empty),
        plusFor = null,
    )

    private fun bannerRow(
        pkg: String,
        emojis: String,
        label: String,
        plusFor: LanguageProgress?,
    ): RemoteViews = RemoteViews(pkg, R.layout.banner_row).apply {
        setImageViewBitmap(R.id.row_vibe, EmojiBitmapFactory.render(emojis))
        setTextViewText(R.id.row_text, label)
        if (plusFor != null) {
            setOnClickPendingIntent(
                R.id.row_plus,
                CompletionReceiver.pendingIntent(context, plusFor.languageId),
            )
            setContentDescription(
                R.id.row_plus,
                context.getString(R.string.banner_plus_one_cd_for, plusFor.name),
            )
        } else {
            setViewVisibility(R.id.row_plus, android.view.View.GONE)
        }
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
