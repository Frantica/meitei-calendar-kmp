package io.github.frantica.meitei

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeiteiCalendarTest {
    private val cal = MeiteiCalendar.Default

    @Test fun cheiraobaFallsOnSajibuOne() {
        assertEquals(LocalDate(2024, 4, 9), cal.dateOf(Festivals.CHEIRAOBA, 2024))
        assertEquals(LocalDate(2025, 3, 30), cal.dateOf(Festivals.CHEIRAOBA, 2025))
        val d = cal.fromGregorian(LocalDate(2025, 3, 30))
        assertEquals(MeiteiMonth.SAJIBU, d.month)
        assertEquals(1, d.tithi)
        assertFalse(d.isLeapMonth)
    }

    @Test fun festivals2025MatchAlmanac() {
        assertEquals(LocalDate(2025, 3, 14), cal.dateOf(Festivals.YAOSHANG, 2025))
        assertEquals(LocalDate(2025, 6, 27), cal.dateOf(Festivals.KANG, 2025))
        assertEquals(LocalDate(2025, 10, 2), cal.dateOf(Festivals.KWAK_JATRA, 2025))
        assertEquals(LocalDate(2025, 10, 23), cal.dateOf(Festivals.NINGOL_CHAKKOUBA, 2025))
    }

    @Test fun leapThawanIn2023() {
        // 2023 had an intercalary Shravana (Thawan), roughly 18 July – 16 August.
        val d = cal.fromGregorian(LocalDate(2023, 7, 30))
        assertEquals(MeiteiMonth.THAWAN, d.month)
        assertTrue(d.isLeapMonth)
        val after = cal.fromGregorian(LocalDate(2023, 8, 25))
        assertEquals(MeiteiMonth.THAWAN, after.month)
        assertFalse(after.isLeapMonth)
    }

    @Test fun leapIngaIn2026() {
        // 2026 has an intercalary Jyeshtha (Inga), roughly 17 May – 15 June.
        val d = cal.fromGregorian(LocalDate(2026, 6, 1))
        assertEquals(MeiteiMonth.INGA, d.month)
        assertTrue(d.isLeapMonth)
    }

    @Test fun monthsAreContiguousAnd29Or30Days() {
        for (year in 2000..2040) {
            val months = cal.monthsOverlapping(year)
            assertTrue(months.size in 12..14, "$year had ${months.size} months")
            months.forEach { assertTrue(it.length in 29..30, "$it has ${it.length} days") }
            months.zipWithNext().forEach { (a, b) ->
                assertEquals(a.endDate.toEpochDaysCompat() + 1, b.startDate.toEpochDaysCompat(), "gap between $a and $b")
            }
        }
    }

    @Test fun eachMonthStartsAtTithiOneOrTwo() {
        for (span in cal.monthsOverlapping(2025)) {
            val first = cal.fromGregorian(span.startDate)
            assertTrue(first.tithi in 1..2, "$span starts at tithi ${first.tithi}")
        }
    }

    @Test fun everyYearHasEveryFestival() {
        for (year in 2000..2040) {
            val names = cal.festivals(year).map { it.festival.name }
            Festivals.ALL.forEach { f ->
                val count = names.count { it == f.name }
                // Wakching straddles December and January, so Emoinu Eratpa can fall twice in one year,
                // or (after a leap month pushes it into January) not at all.
                val allowed = if (f == Festivals.EMOINU_ERATPA) 0..2 else 1..1
                assertTrue(count in allowed, "${f.name} in $year: $names")
            }
        }
    }

    @Test fun emoinuEratpaCanRecurInOneGregorianYear() {
        val dates = cal.festivals(2006).filter { it.festival == Festivals.EMOINU_ERATPA }.map { it.date }
        assertEquals(2, dates.size)
        assertTrue(dates[0].year == 2006 && dates[0].month.ordinal == 0, "first in January: $dates")
        assertTrue(dates[1].month.ordinal == 11, "second in December: $dates")
    }

    private fun LocalDate.toEpochDaysCompat(): Int = LocalDate(1970, 1, 1).daysUntil(this)
}
