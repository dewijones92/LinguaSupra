package com.dewijones.linguasupra.data

/**
 * Snapshot of the user's streak with grace-day information surfaced.
 *
 * @property streak Number of consecutive completed days (graced days included).
 * @property graceUsed True if the streak walk consumed a grace day at any point.
 *                     This stays true for the 7-day cool-down window after the
 *                     grace was used, then resets if/when a new walk no longer
 *                     touches a graced day.
 * @property daysUntilGraceRecharge 0 if a grace day is available right now;
 *                                   otherwise the number of days until the
 *                                   7-day cool-down on the most recent grace
 *                                   expires.
 */
data class StreakStatus(
    val streak: Int,
    val graceUsed: Boolean,
    val daysUntilGraceRecharge: Int,
)
