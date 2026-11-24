package com.example.ui.theme

import com.example.data.VisualStyleMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

data class SeasonalHolidayWindow(
    val mode: VisualStyleMode,
    val holidayName: String,
    val badgeText: String,
    val activeWindowLabel: String
)

private val HOLIDAY_MODES: Set<VisualStyleMode> = setOf(
    VisualStyleMode.HOLIDAY_NEW_YEARS,
    VisualStyleMode.HOLIDAY_VALENTINES,
    VisualStyleMode.HOLIDAY_ST_PATRICKS,
    VisualStyleMode.HOLIDAY_EASTER,
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY,
    VisualStyleMode.HOLIDAY_FOURTH_JULY,
    VisualStyleMode.HOLIDAY_LABOR_DAY,
    VisualStyleMode.HOLIDAY_HALLOWEEN,
    VisualStyleMode.HOLIDAY_VETERANS_DAY,
    VisualStyleMode.HOLIDAY_THANKSGIVING,
    VisualStyleMode.HOLIDAY_CHRISTMAS
)

fun VisualStyleMode.isHolidayTheme(): Boolean = this in HOLIDAY_MODES

/**
 * Computes Easter Sunday for a Gregorian year using the Anonymous Gregorian algorithm.
 */
internal fun computeEasterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = ((h + l - 7 * m + 114) % 31) + 1
    return LocalDate.of(year, month, day)
}

private fun memorialDay(year: Int): LocalDate =
    LocalDate.of(year, Month.MAY, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun laborDay(year: Int): LocalDate =
    LocalDate.of(year, Month.SEPTEMBER, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))

private fun thanksgivingDay(year: Int): LocalDate =
    LocalDate.of(year, Month.NOVEMBER, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))

fun isVisualStyleAvailable(
    mode: VisualStyleMode,
    today: LocalDate = LocalDate.now()
): Boolean {
    // Always allow all holiday themes to be selectable and viewable at any time of year per user request
    return true
}

fun availableVisualStyles(today: LocalDate = LocalDate.now()): List<VisualStyleMode> =
    VisualStyleMode.entries

fun activeHolidayStyles(today: LocalDate = LocalDate.now()): List<VisualStyleMode> =
    VisualStyleMode.entries.filter { it.isHolidayTheme() }

fun sanitizeVisualStyleForSeason(
    mode: VisualStyleMode,
    today: LocalDate = LocalDate.now()
): VisualStyleMode = mode

fun holidayBadgeInfo(mode: VisualStyleMode): SeasonalHolidayWindow? = when (mode) {
    VisualStyleMode.HOLIDAY_NEW_YEARS -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "New Year's Midnight",
        badgeText = "SEASONAL • NEW YEAR'S",
        activeWindowLabel = "Dec 28 – Jan 3"
    )
    VisualStyleMode.HOLIDAY_VALENTINES -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Valentine's Crimson",
        badgeText = "SEASONAL • VALENTINE'S",
        activeWindowLabel = "Feb 7 – Feb 16"
    )
    VisualStyleMode.HOLIDAY_ST_PATRICKS -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "St. Patrick's Clover",
        badgeText = "SEASONAL • ST. PATRICK'S",
        activeWindowLabel = "Mar 10 – Mar 19"
    )
    VisualStyleMode.HOLIDAY_EASTER -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Easter Spring Bloom",
        badgeText = "SEASONAL • EASTER",
        activeWindowLabel = "Easter Week"
    )
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Memorial Honor",
        badgeText = "SEASONAL • MEMORIAL DAY",
        activeWindowLabel = "Memorial Weekend"
    )
    VisualStyleMode.HOLIDAY_FOURTH_JULY -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "4th of July Liberty",
        badgeText = "SEASONAL • INDEPENDENCE DAY",
        activeWindowLabel = "Jun 28 – Jul 7"
    )
    VisualStyleMode.HOLIDAY_LABOR_DAY -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Labor Day Iron",
        badgeText = "SEASONAL • LABOR DAY",
        activeWindowLabel = "Labor Day Weekend"
    )
    VisualStyleMode.HOLIDAY_HALLOWEEN -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Halloween Midnight",
        badgeText = "SEASONAL • HALLOWEEN",
        activeWindowLabel = "Oct 18 – Nov 1"
    )
    VisualStyleMode.HOLIDAY_VETERANS_DAY -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Veterans Valor",
        badgeText = "SEASONAL • VETERANS DAY",
        activeWindowLabel = "Nov 5 – Nov 12"
    )
    VisualStyleMode.HOLIDAY_THANKSGIVING -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Thanksgiving Harvest",
        badgeText = "SEASONAL • THANKSGIVING",
        activeWindowLabel = "Thanksgiving Week"
    )
    VisualStyleMode.HOLIDAY_CHRISTMAS -> SeasonalHolidayWindow(
        mode = mode,
        holidayName = "Christmas Pine & Holly",
        badgeText = "SEASONAL • CHRISTMAS",
        activeWindowLabel = "Dec 1 – Dec 27"
    )
    else -> null
}
