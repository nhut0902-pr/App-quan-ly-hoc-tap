package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import com.example.data.database.ScheduleEvent
import com.example.data.database.Subject
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val events by viewModel.scheduleEvents.collectAsState()

    var selectedDayTab by remember { mutableStateOf(1) } // Default Monday
    val daysOfWeekList = listOf(
        DayTabItem(1, "T2", "Thứ Hai"),
        DayTabItem(2, "T3", "Thứ Ba"),
        DayTabItem(3, "T4", "Thứ Tư"),
        DayTabItem(4, "T5", "Thứ Năm"),
        DayTabItem(5, "T6", "Thứ Sáu"),
        DayTabItem(6, "T7", "Thứ Bảy"),
        DayTabItem(7, "CN", "Chủ Nhật")
    )

    val currentDayEvents = events.filter { it.dayOfWeek == selectedDayTab }.sortedBy { it.startTime }

    var showAddEventDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Thời khóa biểu", fontWeight = FontWeight.Bold) },
                actions = {
                    if (subjects.isNotEmpty()) {
                        IconButton(onClick = { showAddEventDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm lịch học", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- WEEKDAY TABS ROW ---
            TabRow(
                selectedTabIndex = selectedDayTab - 1,
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                daysOfWeekList.forEach { dayTab ->
                    Tab(
                        selected = selectedDayTab == dayTab.id,
                        onClick = { selectedDayTab = dayTab.id },
                        text = {
                            Text(
                                text = dayTab.shortLabel,
                                fontWeight = if (selectedDayTab == dayTab.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- DAY CONTENT LIST ---
            val dayName = daysOfWeekList.find { it.id == selectedDayTab }?.longLabel ?: ""
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Lịch học $dayName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (currentDayEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Trống lịch cho ngày này.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (subjects.isEmpty()) {
                            Text(
                                "Vui lòng tạo ít nhất một Môn học ở Dashboard trước khi lên lịch.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        } else {
                            Button(onClick = { showAddEventDialog = true }) {
                                Text("Lên lịch ngay")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentDayEvents) { event ->
                        val subject = subjects.find { it.id == event.subjectId }
                        ScheduleEventItem(
                            event = event,
                            subject = subject,
                            onAddReminder = { title, type, target, lead, desc ->
                                viewModel.addReminder(title, type, target, lead, desc)
                            },
                            onDelete = { viewModel.deleteScheduleEvent(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        val subjectOptions = subjects
        AddEventDialog(
            dayOfWeek = selectedDayTab,
            subjectOptions = subjectOptions,
            onDismiss = { showAddEventDialog = false },
            onAdd = { subjectId, day, start, end, type ->
                viewModel.addScheduleEvent(subjectId, day, start, end, type)
                showAddEventDialog = false
            }
        )
    }
}

data class DayTabItem(val id: Int, val shortLabel: String, val longLabel: String)

@Composable
fun ScheduleEventItem(
    event: ScheduleEvent,
    subject: Subject?,
    onAddReminder: (String, String, Long, Int, String) -> Unit,
    onDelete: (ScheduleEvent) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showQuickReminder by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Colored bar representing subject
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(55.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(subject?.color ?: 0xFF9E9E9E.toInt()))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = subject?.name ?: "Môn học không xác định",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        Text(
                            "${event.startTime} - ${event.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                event.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (subject != null && subject.room.isNotEmpty()) {
                        Text(
                            "Phòng: ${subject.room}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showQuickReminder = true }) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Cài báo thức học tập", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa sự kiện", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showQuickReminder) {
        var leadMins by remember { mutableStateOf(15) } // Default 15 mins for class
        AlertDialog(
            onDismissRequest = { showQuickReminder = false },
            title = { Text("⏰ Cài Báo Giờ Tiết Học") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Môn học: ${subject?.name ?: "Chưa rõ"}", fontWeight = FontWeight.Bold)
                    Text("Buổi học: ${event.startTime} - ${event.endTime} (${event.type})")
                    Text("Thời gian thông báo trước:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5 to "5p", 15 to "15p", 30 to "30p", 60 to "1h").forEach { (m, label) ->
                            FilterChip(
                                selected = leadMins == m,
                                onClick = { leadMins = m },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parts = event.startTime.split(":")
                        val hr = parts.getOrNull(0)?.toIntOrNull() ?: 8
                        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, hr)
                        cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        
                        // event.dayOfWeek is 1=Monday...7=Sunday
                        // Calendar.DAY_OF_WEEK is 1=Sunday...7=Saturday
                        val calDayOfWeek = when (event.dayOfWeek) {
                            7 -> Calendar.SUNDAY
                            else -> event.dayOfWeek + 1
                        }
                        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        var diff = calDayOfWeek - currentDayOfWeek
                        if (diff < 0) {
                            diff += 7
                        }
                        cal.add(Calendar.DAY_OF_YEAR, diff)
                        
                        onAddReminder(
                            "Chuẩn bị học môn ${subject?.name ?: ""}",
                            "LỊCH HỌC",
                            cal.timeInMillis,
                            leadMins,
                            "Buổi học diễn ra lúc ${event.startTime} tại ${subject?.room ?: "phòng học chính"}. Hãy chuẩn bị sẵn sàng sách vở và tinh thần."
                        )
                        showQuickReminder = false
                    }
                ) {
                    Text("Lên lịch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickReminder = false }) { Text("Hủy") }
            }
        )
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xóa sự kiện?") },
            text = { Text("Bạn có muốn xóa buổi học này khỏi lịch biểu không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(event)
                        showConfirmDelete = false
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun AddEventDialog(
    dayOfWeek: Int,
    subjectOptions: List<Subject>,
    onDismiss: () -> Unit,
    onAdd: (Int, Int, String, String, String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subjectOptions.firstOrNull()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var type by remember { mutableStateOf("Lý thuyết") }

    val typesList = listOf("Lý thuyết", "Thực hành", "Tự học", "Seminar", "Thi/Kiểm tra")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Buổi Học Vào Lịch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Subject Dropdown mock
                Text("Chọn Môn Học:", style = MaterialTheme.typography.labelLarge)
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                    subjectOptions.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubject = sub }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedSubject?.id == sub.id, onClick = { selectedSubject = sub })
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(sub.color)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sub.name)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Bắt đầu (e.g. 08:00)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Kết thúc (e.g. 10:00)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Loại buổi học:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    typesList.take(3).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    typesList.drop(3).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subId = selectedSubject?.id
                    if (subId != null) {
                        onAdd(subId, dayOfWeek, startTime, endTime, type)
                    }
                },
                enabled = selectedSubject != null
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

fun size(dp: androidx.compose.ui.unit.Dp): Modifier {
    return Modifier.size(dp)
}
