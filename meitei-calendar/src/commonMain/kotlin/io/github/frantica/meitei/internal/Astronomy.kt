package io.github.frantica.meitei.internal

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Low-level astronomy used by the calendar, after Jean Meeus, *Astronomical Algorithms* (2nd ed.).
 *
 * All instants are Julian Days in Universal Time (UT) unless a name says `jde` (Terrestrial Time).
 * Accuracy is well within a minute for new moons and ~0.02° for lunar longitude between
 * 1900 and 2100, which is far finer than the 12° width of a tithi.
 */
internal object Astronomy {
    const val J2000: Double = 2451545.0
    const val UNIX_EPOCH_JD: Double = 2440587.5
    const val SYNODIC_MONTH: Double = 29.530588861
    private const val NEW_MOON_EPOCH: Double = 2451550.09766
    private const val D2R = PI / 180.0

    private fun sind(x: Double) = sin(x * D2R)
    private fun cosd(x: Double) = cos(x * D2R)

    fun norm360(x: Double): Double {
        val r = x % 360.0
        return if (r < 0) r + 360.0 else r
    }

    /** Julian Day at 00:00 UT of the given day counted from 1970-01-01. */
    fun jdAtUtMidnight(epochDay: Int): Double = epochDay + UNIX_EPOCH_JD

    /** ΔT = TT − UT in days (Espenak & Meeus polynomial fits). */
    fun deltaTDays(jdUt: Double): Double {
        val y = 2000.0 + (jdUt - J2000) / 365.25
        val seconds = when {
            y < 1986 -> {
                val t = y - 1975
                45.45 + 1.067 * t - t * t / 260 - t * t * t / 718
            }
            y < 2005 -> 64.0
            y < 2050 -> {
                val t = y - 2000
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
            else -> {
                val u = (y - 1820) / 100
                -20 + 32 * u * u - 0.5628 * (2150 - y)
            }
        }
        return seconds / 86400.0
    }

    /** First lunation index used by [newMoonUt]: k = 0 is the new moon of 2000-01-06. */
    fun approximateLunation(jdUt: Double): Int = floor((jdUt - NEW_MOON_EPOCH) / SYNODIC_MONTH).toInt()

    /** Instant (JD, UT) of the true new moon with lunation index [k] (Meeus ch. 49). */
    fun newMoonUt(k: Int): Double {
        val kd = k.toDouble()
        val t = kd / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        var jde = NEW_MOON_EPOCH + SYNODIC_MONTH * kd + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4
        val e = 1 - 0.002516 * t - 0.0000074 * t2
        val m = 2.5534 + 29.10535670 * kd - 0.0000014 * t2 - 0.00000011 * t3
        val mp = 201.5643 + 385.81693528 * kd + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4
        val f = 160.7108 + 390.67050284 * kd - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4
        val om = 124.7746 - 1.56375588 * kd + 0.0020672 * t2 + 0.00000215 * t3

        jde += -0.40720 * sind(mp) +
            0.17241 * e * sind(m) +
            0.01608 * sind(2 * mp) +
            0.01039 * sind(2 * f) +
            0.00739 * e * sind(mp - m) -
            0.00514 * e * sind(mp + m) +
            0.00208 * e * e * sind(2 * m) -
            0.00111 * sind(mp - 2 * f) -
            0.00057 * sind(mp + 2 * f) +
            0.00056 * e * sind(2 * mp + m) -
            0.00042 * sind(3 * mp) +
            0.00042 * e * sind(m + 2 * f) +
            0.00038 * e * sind(m - 2 * f) -
            0.00024 * e * sind(2 * mp - m) -
            0.00017 * sind(om) -
            0.00007 * sind(mp + 2 * m) +
            0.00004 * sind(2 * mp - 2 * f) +
            0.00004 * sind(3 * m) +
            0.00003 * sind(mp + m - 2 * f) +
            0.00003 * sind(2 * mp + 2 * f) -
            0.00003 * sind(mp + m + 2 * f) +
            0.00003 * sind(mp - m + 2 * f) -
            0.00002 * sind(mp - m - 2 * f) -
            0.00002 * sind(3 * mp + m) +
            0.00002 * sind(4 * mp)

        return jde - deltaTDays(jde)
    }

    /** Geometric (true) longitude of the Sun, degrees (Meeus ch. 25). */
    fun sunTrueLongitude(jde: Double): Double {
        val t = (jde - J2000) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sind(m) +
            (0.019993 - 0.000101 * t) * sind(2 * m) +
            0.000289 * sind(3 * m)
        return norm360(l0 + c)
    }

    /** Apparent tropical longitude of the Sun (aberration + nutation), degrees. */
    fun sunApparentLongitude(jdUt: Double): Double {
        val jde = jdUt + deltaTDays(jdUt)
        val t = (jde - J2000) / 36525.0
        val om = 125.04 - 1934.136 * t
        return norm360(sunTrueLongitude(jde) - 0.00569 - 0.00478 * sind(om))
    }

    // Periodic terms for lunar longitude: D, M, M', F, coefficient (1e-6 degrees). Meeus table 47.A (largest terms).
    private val MOON_TERMS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 1, 0, 6288774), intArrayOf(2, 0, -1, 0, 1274027), intArrayOf(2, 0, 0, 0, 658314),
        intArrayOf(0, 0, 2, 0, 213618), intArrayOf(0, 1, 0, 0, -185116), intArrayOf(0, 0, 0, 2, -114332),
        intArrayOf(2, 0, -2, 0, 58793), intArrayOf(2, -1, -1, 0, 57066), intArrayOf(2, 0, 1, 0, 53322),
        intArrayOf(2, -1, 0, 0, 45758), intArrayOf(0, 1, -1, 0, -40923), intArrayOf(1, 0, 0, 0, -34720),
        intArrayOf(0, 1, 1, 0, -30383), intArrayOf(2, 0, 0, -2, 15327), intArrayOf(0, 0, 1, 2, -12528),
        intArrayOf(0, 0, 1, -2, 10980), intArrayOf(4, 0, -1, 0, 10675), intArrayOf(0, 0, 3, 0, 10034),
        intArrayOf(4, 0, -2, 0, 8548), intArrayOf(2, 1, -1, 0, -7888), intArrayOf(2, 1, 0, 0, -6766),
        intArrayOf(1, 0, -1, 0, -5163), intArrayOf(1, 1, 0, 0, 4987), intArrayOf(2, -1, 1, 0, 4036),
        intArrayOf(2, 0, 2, 0, 3994), intArrayOf(4, 0, 0, 0, 3861), intArrayOf(2, 0, -3, 0, 3665),
        intArrayOf(0, 1, -2, 0, -2689), intArrayOf(2, 0, -1, 2, -2602), intArrayOf(2, -1, -2, 0, 2390),
        intArrayOf(1, 0, 1, 0, -2348), intArrayOf(2, -2, 0, 0, 2236), intArrayOf(0, 1, 2, 0, -2120),
        intArrayOf(0, 2, 0, 0, -2069), intArrayOf(2, -2, -1, 0, 2048), intArrayOf(2, 0, 1, -2, -1773),
        intArrayOf(2, 0, 0, 2, -1595), intArrayOf(4, -1, -1, 0, 1215), intArrayOf(0, 0, 2, 2, -1110),
        intArrayOf(3, 0, -1, 0, -892), intArrayOf(2, 1, 1, 0, -810), intArrayOf(4, -1, -2, 0, 759),
        intArrayOf(0, 2, -1, 0, -713), intArrayOf(2, 2, -1, 0, -700), intArrayOf(2, 1, -2, 0, 691),
        intArrayOf(2, -1, 0, -2, 596), intArrayOf(4, 0, 1, 0, 549), intArrayOf(0, 0, 4, 0, 537),
        intArrayOf(4, -1, 0, 0, 520), intArrayOf(1, 0, -2, 0, -487),
    )

    /** Geometric longitude of the Moon, degrees (Meeus ch. 47, truncated). */
    fun moonLongitude(jde: Double): Double {
        val t = (jde - J2000) / 36525.0
        val t2 = t * t
        val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t2
        val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t2
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t2
        val mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t2
        val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t2
        val e = 1 - 0.002516 * t - 0.0000074 * t2
        val a1 = 119.75 + 131.849 * t
        val a2 = 53.09 + 479264.290 * t

        var sum = 0.0
        for (term in MOON_TERMS) {
            val arg = term[0] * d + term[1] * m + term[2] * mp + term[3] * f
            val eFactor = when (kotlin.math.abs(term[1])) {
                1 -> e
                2 -> e * e
                else -> 1.0
            }
            sum += term[4] * eFactor * sind(arg)
        }
        sum += 3958 * sind(a1) + 1962 * sind(lp - f) + 318 * sind(a2)
        return norm360(lp + sum / 1_000_000.0)
    }

    /** Moon − Sun apparent elongation in degrees, 0 at new moon, 180 at full moon. */
    fun elongation(jdUt: Double): Double {
        val jde = jdUt + deltaTDays(jdUt)
        // Nutation cancels in the difference; only the Sun's aberration remains.
        return norm360(moonLongitude(jde) - (sunTrueLongitude(jde) - 0.00569))
    }

    /** Lahiri (Chitrapaksha) ayanamsa, degrees. Linear fit, good to ~0.01° over 1900–2100. */
    fun lahiriAyanamsa(jdUt: Double): Double = 23.853 + 0.0139694 * (jdUt - J2000) / 365.25

    /** Sidereal zodiac sign of the Sun: 0 = Mesha (Aries) … 11 = Meena (Pisces). */
    fun siderealSunSign(jdUt: Double): Int =
        floor(norm360(sunApparentLongitude(jdUt) - lahiriAyanamsa(jdUt)) / 30.0).toInt()

    /**
     * Sunrise (JD, UT) on the civil day [epochDay] at the given place.
     * Upper limb on the horizon with standard refraction (−0.833°).
     */
    fun sunriseUt(epochDay: Int, latitude: Double, longitudeEast: Double): Double {
        val n = (epochDay - 10957).toDouble() // whole days from J2000.0 noon to this date's noon (UT)
        val jStar = n - longitudeEast / 360.0
        val m = norm360(357.5291 + 0.98560028 * jStar)
        val c = 1.9148 * sind(m) + 0.0200 * sind(2 * m) + 0.0003 * sind(3 * m)
        val lambda = norm360(m + c + 180.0 + 102.9372)
        val transit = J2000 + jStar + 0.0053 * sind(m) - 0.0069 * sind(2 * lambda)
        val decl = asin(sind(lambda) * sind(23.4397))
        val cosOmega = (sind(-0.833) - sind(latitude) * sin(decl)) / (cosd(latitude) * cos(decl))
        val omega = acos(cosOmega.coerceIn(-1.0, 1.0)) / D2R
        return transit - omega / 360.0
    }

    /** Civil date (days from 1970-01-01) of an instant in a fixed UTC offset. */
    fun civilEpochDay(jdUt: Double, utcOffsetHours: Double): Int =
        floor(jdUt - UNIX_EPOCH_JD + utcOffsetHours / 24.0).toInt()
}
