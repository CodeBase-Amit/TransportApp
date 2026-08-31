package com.example.transportapp.domain.transport.tracking

/**
 * §7.3 — ageing. The expected arrival is *stored* at booking (route transit days as they
 * stood then); the late flag is *computed* on open, comparing the last event's time against
 * expected arrival plus the company's current grace period. Storing the date and computing
 * the flag is the split that matters: changing the grace period re-flags everything with no
 * migration. Pure functions over epoch millis.
 */
enum class AgeingBucket(val label: String) {
    ON_TIME("On time"),
    OVERDUE_1_3("1–3 days overdue"),
    OVERDUE_4_7("4–7 days overdue"),
    OVERDUE_7_PLUS("More than a week overdue"),
}

object Ageing {

    const val DEFAULT_GRACE_DAYS = 1L

    /**
     * Pure time comparison only: now past expected plus grace. "Undelivered" is a status
     * question the caller answers from the projection — a timestamp cannot know the goods
     * arrived.
     */
    fun isOverdue(expectedArrivalAt: Long, now: Long, graceDays: Long = DEFAULT_GRACE_DAYS): Boolean =
        now > expectedArrivalAt + graceDays * DAY_MS

    fun daysOverdue(expectedArrivalAt: Long, now: Long): Long =
        ((now - expectedArrivalAt) / DAY_MS).coerceAtLeast(0)

    fun bucket(expectedArrivalAt: Long, now: Long, graceDays: Long = DEFAULT_GRACE_DAYS): AgeingBucket {
        val days = daysOverdue(expectedArrivalAt, now) - graceDays
        return when {
            days < 1 -> AgeingBucket.ON_TIME
            days <= 3 -> AgeingBucket.OVERDUE_1_3
            days <= 7 -> AgeingBucket.OVERDUE_4_7
            else -> AgeingBucket.OVERDUE_7_PLUS
        }
    }

    const val DAY_MS = 86_400_000L
}
