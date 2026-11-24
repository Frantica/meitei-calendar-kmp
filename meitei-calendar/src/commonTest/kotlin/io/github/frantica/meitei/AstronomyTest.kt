package io.github.frantica.meitei

import io.github.frantica.meitei.internal.Astronomy
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class AstronomyTest {
    private fun jd(y: Int, m: Int, d: Int, hh: Int, mm: Int): Double =
        Astronomy.jdAtUtMidnight(LocalDate(1970, 1, 1).daysUntil(LocalDate(y, m, d))) + (hh * 60 + mm) / 1440.0

    private fun assertNewMoonNear(expected: Double) {
        val k = Astronomy.approximateLunation(expected + 2)
        val actual = listOf(k - 1, k, k + 1).map { Astronomy.newMoonUt(it) }.minBy { abs(it - expected) }
        val minutes = abs(actual - expected) * 1440
        assertTrue(minutes < 3, "new moon off by $minutes minutes")
    }

    // Published new-moon instants (UTC) of recent solar eclipses.
    @Test fun newMoonOfTotalEclipse2024() = assertNewMoonNear(jd(2024, 4, 8, 18, 21))
    @Test fun newMoonOfPartialEclipse2025() = assertNewMoonNear(jd(2025, 3, 29, 10, 58))
    @Test fun newMoonOfAnnularEclipse2023() = assertNewMoonNear(jd(2023, 10, 14, 17, 55))

    @Test fun elongationIsNearZeroAtNewMoon() {
        val e = Astronomy.elongation(jd(2024, 4, 8, 18, 21))
        assertTrue(e < 0.2 || e > 359.8, "elongation was $e")
    }

    @Test fun sunriseInImphalIsPlausible() {
        // Imphal sunrise is roughly 04:30 IST in June and 06:05 IST in early January.
        val june = Astronomy.sunriseUt(LocalDate(1970, 1, 1).daysUntil(LocalDate(2025, 6, 21)), 24.817, 93.937)
        val jan = Astronomy.sunriseUt(LocalDate(1970, 1, 1).daysUntil(LocalDate(2025, 1, 5)), 24.817, 93.937)
        val istHour = { j: Double -> ((j + 0.5 + 5.5 / 24) % 1.0) * 24 }
        assertTrue(istHour(june) in 4.2..4.8, "June sunrise ${istHour(june)}")
        assertTrue(istHour(jan) in 5.8..6.3, "January sunrise ${istHour(jan)}")
    }
}
