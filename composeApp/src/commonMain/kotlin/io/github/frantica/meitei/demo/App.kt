package io.github.frantica.meitei.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.frantica.meitei.FestivalOccurrence
import io.github.frantica.meitei.MeiteiCalendar
import io.github.frantica.meitei.MeiteiMonthSpan
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val Light = lightColorScheme(
    primary = Color(0xFF8A1C2B), // Phanek maroon
    onPrimary = Color.White,
    secondary = Color(0xFF2F6B4F),
    surface = Color(0xFFFBF8F4),
    surfaceVariant = Color(0xFFF1E9E2),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFF2A3AC),
    onPrimary = Color(0xFF3A0710),
    secondary = Color(0xFF8FD1AE),
    surface = Color(0xFF16120F),
    surfaceVariant = Color(0xFF2A221E),
)

@OptIn(ExperimentalTime::class)
private fun todayInImphal(): LocalDate {
    val millis = Clock.System.now().toEpochMilliseconds() + 5L * 3_600_000 + 1_800_000
    return LocalDate(1970, 1, 1).plus((millis / 86_400_000L).toInt(), DateTimeUnit.DAY)
}

@Composable
fun App() {
    val calendar = MeiteiCalendar.Default
    val today = remember { todayInImphal() }
    var span by remember { mutableStateOf(calendar.monthContaining(today)) }
    val festivals = remember(span) {
        (calendar.festivals(span.startDate.year) + calendar.festivals(span.endDate.year))
            .distinct()
            .filter { it.date in span }
    }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TodayCard(calendar, today)
                    MonthHeader(span, onPrev = { span = calendar.previous(span) }, onNext = { span = calendar.next(span) })
                    MonthGrid(calendar, span, today, festivals)
                    FestivalList(festivals)
                }
            }
        }
    }
}

@Composable
private fun TodayCard(calendar: MeiteiCalendar, today: LocalDate) {
    val m = remember(today) { calendar.fromGregorian(today) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary).padding(20.dp),
    ) {
        Text("TODAY IN IMPHAL", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f), fontSize = 12.sp, letterSpacing = 1.5.sp)
        Text(m.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            "${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, $today · ${m.month.indianName} · " +
                when {
                    m.isFullMoon -> "full moon"
                    m.isNewMoon -> "new moon"
                    m.paksha.name == "WAXING" -> "waxing moon"
                    else -> "waning moon"
                },
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun MonthHeader(span: MeiteiMonthSpan, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrev) { Text("‹ Prev") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (if (span.isLeapMonth) "Leap " else "") + span.month.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("${span.startDate} – ${span.endDate}", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onNext) { Text("Next ›") }
    }
}

@Composable
private fun MonthGrid(calendar: MeiteiCalendar, span: MeiteiMonthSpan, today: LocalDate, festivals: List<FestivalOccurrence>) {
    val days = remember(span) { span.dates().map { calendar.fromGregorian(it) } }
    val lead = span.startDate.dayOfWeek.isoDayNumber - 1
    val cells: List<Int?> = List(lead) { null } + days.indices.toList()
    val festivalDates = festivals.map { it.date }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val idx = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (idx != null) {
                            val d = days[idx]
                            DayCell(d.tithi, d.gregorian, d.gregorian == today, d.gregorian in festivalDates, d.isFullMoon, d.isNewMoon)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(tithi: Int, date: LocalDate, isToday: Boolean, isFestival: Boolean, fullMoon: Boolean, newMoon: Boolean) {
    val bg = when {
        isToday -> MaterialTheme.colorScheme.primary
        isFestival -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(bg).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$tithi", color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${date.day} ${date.month.name.take(3).lowercase()}", color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
        if (fullMoon || newMoon) {
            Box(
                Modifier.padding(top = 2.dp).size(6.dp).clip(CircleShape)
                    .background(if (fullMoon) Color(0xFFE8C547) else fg.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun FestivalList(festivals: List<FestivalOccurrence>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Festivals this month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (festivals.isEmpty()) {
            Text("No major festivals this month.", style = MaterialTheme.typography.bodyMedium)
        }
        festivals.forEach { f ->
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.festival.name, fontWeight = FontWeight.SemiBold)
                    Text(f.festival.description, style = MaterialTheme.typography.bodySmall)
                }
                Text(f.date.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Computed astronomically for Imphal · meitei-calendar-kmp",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
