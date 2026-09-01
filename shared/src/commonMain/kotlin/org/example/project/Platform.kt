package com.abtsplazita.posplazita

import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun currentTimeMillis(): Long

expect fun isBluetoothSupported(): Boolean

expect fun playErrorSound()

expect fun initializeFirebase()

expect fun isFirebaseInitialized(): Boolean

interface FilePicker {
    fun pickFile(onFilePicked: (ByteArray?) -> Unit)
}

@Composable
expect fun rememberFilePicker(): FilePicker

expect fun shareText(text: String, phone: String = "")

expect fun generateAndShareDebtPdf(data: com.abtsplazita.posplazita.ui.customers.DebtReportData, phone: String)

fun getStartOfDay(): Long {
    val now = Instant.fromEpochMilliseconds(currentTimeMillis())
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val startOfDay = kotlinx.datetime.LocalDateTime(localDateTime.year, localDateTime.monthNumber, localDateTime.dayOfMonth, 0, 0, 0, 0)
    return startOfDay.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = dt.date
    val time = dt.time
    return "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year} ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
