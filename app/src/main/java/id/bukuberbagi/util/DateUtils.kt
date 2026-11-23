package id.bukuberbagi.util

import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val indo = Locale("id", "ID")

    private fun parse(value: String?): OffsetDateTime? =
        value?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

    /** "2 mnt lalu", "3 jam lalu", "Kemarin", "12 Nov" */
    fun relative(value: String?): String {
        val time = parse(value) ?: return ""
        val diff = Duration.between(time, OffsetDateTime.now())
        return when {
            diff.toMinutes() < 1 -> "Baru saja"
            diff.toMinutes() < 60 -> "${diff.toMinutes()} mnt lalu"
            diff.toHours() < 24 -> "${diff.toHours()} jam lalu"
            diff.toDays() < 2 -> "Kemarin"
            diff.toDays() < 7 -> "${diff.toDays()} hari lalu"
            else -> time.atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", indo))
        }
    }

    fun toLocalDate(value: String?): LocalDate? =
        parse(value)?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalDate()

    /** Tanggal penyerahan dikirim sebagai pukul 10.00 waktu lokal. */
    fun toIsoForServer(date: LocalDate): String =
        date.atTime(10, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()

    fun dayShort(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, indo)

    fun longDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", indo))

    fun monthYear(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", indo))
}
