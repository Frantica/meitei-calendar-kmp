package io.github.frantica.meitei

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * The twelve months of the Meitei (Manipuri) lunar calendar, in order from the new year.
 *
 * Months are lunar and begin the day after the new moon (amanta reckoning). Each corresponds to a
 * month of the Indian lunisolar calendar, given as [indianName].
 */
public enum class MeiteiMonth(public val displayName: String, public val indianName: String) {
    SAJIBU("Sajibu", "Chaitra"),
    KALEN("Kalen", "Vaishakha"),
    INGA("Inga", "Jyeshtha"),
    INGEN("Ingen", "Ashadha"),
    THAWAN("Thawan", "Shravana"),
    LANGBAN("Langban", "Bhadrapada"),
    MERA("Mera", "Ashvin"),
    HIYANGKEI("Hiyangkei", "Kartika"),
    POINU("Poinu", "Margashirsha"),
    WAKCHING("Wakching", "Pausha"),
    PHAIREL("Phairel", "Magha"),
    LAMTA("Lamta", "Phalguna"),
}

/** Waxing (bright) or waning (dark) half of a lunar month. */
public enum class Paksha { WAXING, WANING }

/**
 * A day in the Meitei calendar.
 *
 * @property tithi the lunar day (1–30) prevailing at sunrise in Imphal. 15 is the full moon, 30 the new moon.
 *   Because lunar days and solar days differ in length, a tithi is occasionally skipped or repeated.
 * @property isLeapMonth true in an intercalary month (*adhika maasa*), which repeats the name of the month after it.
 */
public data class MeiteiDate(
    public val gregorian: LocalDate,
    public val month: MeiteiMonth,
    public val tithi: Int,
    public val isLeapMonth: Boolean,
) {
    public val paksha: Paksha get() = if (tithi <= 15) Paksha.WAXING else Paksha.WANING
    public val isFullMoon: Boolean get() = tithi == 15
    public val isNewMoon: Boolean get() = tithi == 30

    override fun toString(): String =
        (if (isLeapMonth) "Leap " else "") + month.displayName + " " + tithi
}

/** One lunar month as a range of Gregorian dates (both ends inclusive). */
public data class MeiteiMonthSpan(
    public val month: MeiteiMonth,
    public val isLeapMonth: Boolean,
    public val startDate: LocalDate,
    public val endDate: LocalDate,
) {
    /** Number of civil days in this month (29 or 30). */
    public val length: Int get() = startDate.daysUntil(endDate) + 1

    public operator fun contains(date: LocalDate): Boolean = date >= startDate && date <= endDate

    /** Every Gregorian date in this month, in order. */
    public fun dates(): List<LocalDate> = List(length) { startDate.plus(it, DateTimeUnit.DAY) }

    override fun toString(): String =
        (if (isLeapMonth) "Leap " else "") + month.displayName + " (" + startDate + " – " + endDate + ")"
}

/** A festival observed on a fixed lunar day of a Meitei month. */
public data class Festival(
    public val name: String,
    public val month: MeiteiMonth,
    public val tithi: Int,
    public val description: String,
)

/** A festival placed on a Gregorian date. */
public data class FestivalOccurrence(public val festival: Festival, public val date: LocalDate)

/** Major festivals fixed by the lunar calendar. Lai Haraoba and other locally scheduled observances are not included. */
public object Festivals {
    public val CHEIRAOBA: Festival =
        Festival("Cheiraoba", MeiteiMonth.SAJIBU, 1, "Meitei New Year, first day of Sajibu")
    public val KANG: Festival =
        Festival("Kang (Rath Yatra)", MeiteiMonth.INGEN, 2, "Chariot festival of Shri Jagannath")
    public val KWAK_JATRA: Festival =
        Festival("Kwak Jatra", MeiteiMonth.MERA, 10, "Festival of the crow, coincides with Durga Puja's Dashami")
    public val NINGOL_CHAKKOUBA: Festival =
        Festival("Ningol Chakkouba", MeiteiMonth.HIYANGKEI, 2, "Married daughters are invited home for a feast")
    public val EMOINU_ERATPA: Festival =
        Festival("Emoinu Eratpa", MeiteiMonth.WAKCHING, 12, "Worship of Emoinu, goddess of wealth and the hearth")
    public val YAOSHANG: Festival =
        Festival("Yaoshang", MeiteiMonth.LAMTA, 15, "Five-day spring festival beginning on the full moon of Lamta")

    public val ALL: List<Festival> = listOf(CHEIRAOBA, KANG, KWAK_JATRA, NINGOL_CHAKKOUBA, EMOINU_ERATPA, YAOSHANG)
}
