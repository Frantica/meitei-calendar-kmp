package io.github.frantica.meitei

import io.github.frantica.meitei.internal.Astronomy
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.floor

/** A place whose sunrise decides the lunar day. Defaults to Imphal. */
public data class Location(
    public val latitude: Double,
    public val longitudeEast: Double,
    public val utcOffsetHours: Double,
) {
    public companion object {
        public val IMPHAL: Location = Location(latitude = 24.817, longitudeEast = 93.937, utcOffsetHours = 5.5)
    }
}

/**
 * Converts between the Gregorian and Meitei calendars, computed astronomically.
 *
 * - A month runs from one new moon to the next and starts on the first day whose sunrise falls after the new moon.
 * - A month is named by the sidereal sign (Lahiri ayanamsa) of the Sun at its starting new moon: Sun in
 *   Meena (Pisces) starts Sajibu, Sun in Mesha starts Kalen, and so on. When the Sun changes sign at neither
 *   end of a month, that month is a leap month and takes the name of the month after it.
 * - The day of the month is the tithi (lunar day) at sunrise.
 *
 * ```kotlin
 * val d = MeiteiCalendar.fromGregorian(LocalDate(2025, 3, 30)) // Sajibu 1
 * val festivals = MeiteiCalendar.festivals(2026)
 * ```
 */
public class MeiteiCalendar(private val location: Location = Location.IMPHAL) {

    /** The Meitei date for a Gregorian date. */
    public fun fromGregorian(date: LocalDate): MeiteiDate {
        val day = date.epochDay()
        val sunrise = sunrise(day)
        val k = lunationBefore(sunrise)
        val (month, leap) = monthName(k)
        return MeiteiDate(date, month, tithiAt(sunrise, k), leap)
    }

    /** The lunar month that contains [date]. */
    public fun monthContaining(date: LocalDate): MeiteiMonthSpan = span(lunationBefore(sunrise(date.epochDay())))

    /** All lunar months that overlap the Gregorian [year], in order. */
    public fun monthsOverlapping(year: Int): List<MeiteiMonthSpan> {
        val first = lunationBefore(sunrise(LocalDate(year, 1, 1).epochDay()))
        val last = lunationBefore(sunrise(LocalDate(year, 12, 31).epochDay()))
        return (first..last).map { span(it) }
    }

    /** The month after [span]. */
    public fun next(span: MeiteiMonthSpan): MeiteiMonthSpan = monthContaining(span.endDate.plus(1, DateTimeUnit.DAY))

    /** The month before [span]. */
    public fun previous(span: MeiteiMonthSpan): MeiteiMonthSpan = monthContaining(span.startDate.plus(-1, DateTimeUnit.DAY))

    /**
     * Gregorian dates of [festivals] falling in [year], sorted by date.
     *
     * A festival falls on the first day of its month whose sunrise tithi has reached the festival's tithi, so a
     * skipped tithi moves the festival to the next day, as almanacs do. Leap months never hold festivals.
     */
    public fun festivals(year: Int, festivals: List<Festival> = Festivals.ALL): List<FestivalOccurrence> {
        val spans = monthsOverlapping(year)
        val out = mutableListOf<FestivalOccurrence>()
        for (span in spans) {
            if (span.isLeapMonth) continue
            val matching = festivals.filter { it.month == span.month }
            if (matching.isEmpty()) continue
            val days = span.dates().map { fromGregorian(it) }
            for (f in matching) {
                val hit = days.firstOrNull { it.tithi >= f.tithi } ?: continue
                if (hit.gregorian.year == year) out += FestivalOccurrence(f, hit.gregorian)
            }
        }
        return out.sortedBy { it.date }
    }

    /** Date of one festival in [year], or null if it doesn't fall in that year. */
    public fun dateOf(festival: Festival, year: Int): LocalDate? =
        festivals(year, listOf(festival)).firstOrNull()?.date

    // ---- internals ----

    private fun sunrise(epochDay: Int): Double =
        Astronomy.sunriseUt(epochDay, location.latitude, location.longitudeEast)

    /** Index of the last new moon at or before [jdUt]. */
    private fun lunationBefore(jdUt: Double): Int {
        var k = Astronomy.approximateLunation(jdUt)
        while (Astronomy.newMoonUt(k) > jdUt) k--
        while (Astronomy.newMoonUt(k + 1) <= jdUt) k++
        return k
    }

    private fun monthStartDay(k: Int): Int {
        val newMoon = Astronomy.newMoonUt(k)
        var day = Astronomy.civilEpochDay(newMoon, location.utcOffsetHours)
        if (sunrise(day) <= newMoon) day++
        return day
    }

    private fun monthName(k: Int): Pair<MeiteiMonth, Boolean> {
        val signAtStart = Astronomy.siderealSunSign(Astronomy.newMoonUt(k))
        val signAtEnd = Astronomy.siderealSunSign(Astronomy.newMoonUt(k + 1))
        val month = MeiteiMonth.entries[(signAtStart + 1) % 12]
        return month to (signAtStart == signAtEnd)
    }

    private fun span(k: Int): MeiteiMonthSpan {
        val (month, leap) = monthName(k)
        return MeiteiMonthSpan(
            month = month,
            isLeapMonth = leap,
            startDate = epochDayToDate(monthStartDay(k)),
            endDate = epochDayToDate(monthStartDay(k + 1) - 1),
        )
    }

    private fun tithiAt(sunrise: Double, k: Int): Int {
        var tithi = floor(Astronomy.elongation(sunrise) / 12.0).toInt() + 1
        // The lunar-longitude series and the new-moon series can disagree by a few minutes right at a new moon.
        val sinceNewMoon = sunrise - Astronomy.newMoonUt(k)
        if (sinceNewMoon < 1.5 && tithi >= 29) tithi = 1
        if (sinceNewMoon > 20 && tithi <= 2) tithi = 30
        return tithi
    }

    public companion object {
        /** Calendar for Imphal, the reference location for Meitei almanacs. */
        public val Default: MeiteiCalendar = MeiteiCalendar()

        /** Shortcut for `MeiteiCalendar.Default.fromGregorian(date)`. */
        public fun of(date: LocalDate): MeiteiDate = Default.fromGregorian(date)
    }
}

private val EPOCH = LocalDate(1970, 1, 1)
private fun LocalDate.epochDay(): Int = EPOCH.daysUntil(this)
private fun epochDayToDate(epochDay: Int): LocalDate = EPOCH.plus(epochDay, DateTimeUnit.DAY)
