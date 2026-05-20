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
import androidx.compose.ui.unit.sp
import com.example.data.database.StudyReminder
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(viewModel: StudyViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val schedules by viewModel.scheduleEvents.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showAddReminderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Warning Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillPadding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Trung tâm báo giờ học tập",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Cảnh báo trong ứng dụng được kích hoạt tự động theo thời gian thực để giúp nhắc bạn trước mỗi buổi học và hạn bài tập lý tưởng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.9f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.AlarmOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Chưa có thiết lập nhắc nhở!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Bạn có thể thiết lập nhắc nhở ôn bài, chuẩn bị thi cử, hay dọn sạch deadline dồn dập tại màn hình này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showAddReminderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tạo nhắc nhở ngay")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Danh sách nhắc nhở chủ động (${reminders.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(reminders) { reminder ->
                        ReminderCardItem(
                            reminder = reminder,
                            onToggleTriggered = { viewModel.toggleReminderTriggered(it) },
                            onDelete = { viewModel.deleteReminder(it) }
                        )
                    }
                }
            }

            // Floating action button inside tab contents to write reminder
            if (reminders.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddReminderDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("add_reminder_fab"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = "Tạo nhắc nhở")
                }
            }
        }
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onAdd = { title, type, targetTime, leadMinutes, description ->
                viewModel.addReminder(title, type, targetTime, leadMinutes, description)
                showAddReminderDialog = false
            }
        )
    }
}

@Composable
fun ReminderCardItem(
    reminder: StudyReminder,
    onToggleTriggered: (StudyReminder) -> Unit,
    onDelete: (StudyReminder) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
    val targetTimeStr = sdfFull.format(Date(reminder.targetTime))

    val alertTimeCalc = reminder.targetTime - reminder.leadMinutes * 60 * 1000L
    val alertTimeStr = sdfFull.format(Date(alertTimeCalc))

    val reminderTypeIcon = when (reminder.type) {
        "LỊCH HỌC" -> Icons.Default.CalendarToday
        "DEADLINE BÀI TẬP" -> Icons.Default.AssignmentLate
        else -> Icons.Default.Label
    }
    
    val badgeColor = when (reminder.type) {
        "LỊCH HỌC" -> Color(0xFF2196F3)
        "DEADLINE BÀI TẬP" -> Color(0xFFE91E63)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isTriggered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        reminderTypeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                reminder.type,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (reminder.isTriggered) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Đã thông báo",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Đang chờ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        reminder.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isTriggered) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (reminder.description.isNotEmpty()) {
                        Text(
                            reminder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, size(12.dp), tint = MaterialTheme.colorScheme.outline)
                        Text(
                            "Sự kiện: $targetTimeStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Thời gian báo: Nhắc trước ${reminder.leadMinutes}p ($alertTimeStr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onToggleTriggered(reminder) }) {
                    Icon(
                        imageVector = if (reminder.isTriggered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Cập nhật kích hoạt",
                        tint = if (reminder.isTriggered) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                    )
                }
                
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa nhắc nhở",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xóa nhắc nhở") },
            text = { Text("Bạn có chắc chắn muốn xóa nhắc nhở \"${reminder.title}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(reminder); showConfirmDelete = false }) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Long, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LỊCH HỌC") } // "LỊCH HỌC", "DEADLINE BÀI TẬP", "KHÁC"
    var leadMinutes by remember { mutableStateOf(15) } // default 15 mins
    var description by remember { mutableStateOf("") }
    
    // Simple robust date selection inputs for streaming
    var rawDaysText by remember { mutableStateOf("0") } // days to target, 0 means today
    var hourString by remember { mutableStateOf("15") }
    var minString by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Nhắc Nhở Học Tập New") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chọn Thể Loại:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("LỊCH HỌC", "DEADLINE BÀI TẬP", "KHÁC").forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề báo thức/nhắc nhở (*)") },
                    placeholder = { Text("Ví dụ: Ôn thi giữa kỳ Sinh học, Đi họp lớp") },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả / Ghi chú hành động") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Thời gian diễn ra sự kiện:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rawDaysText,
                        onValueChange = { rawDaysText = it },
                        label = { Text("Số ngày nữa") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hourString,
                        onValueChange = { hourString = it },
                        label = { Text("Mấy giờ (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minString,
                        onValueChange = { minString = it },
                        label = { Text("Phút (0-59)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Nhắc nhở trước bao nhiêu lâu?", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5 to "5p", 15 to "15p", 30 to "30p", 60 to "1h", 1440 to "1 ngày").forEach { (mins, label) ->
                        FilterChip(
                            selected = leadMinutes == mins,
                            onClick = { leadMinutes = mins },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = rawDaysText.toIntOrNull() ?: 0
                    val hr = hourString.toIntOrNull() ?: 12
                    val min = minString.toIntOrNull() ?: 0
                    
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, days)
                        set(Calendar.HOUR_OF_DAY, hr)
                        set(Calendar.MINUTE, min)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    if (title.isNotBlank()) {
                        onAdd(title, selectedType, cal.timeInMillis, leadMinutes, description)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Cài Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

fun Modifier.fillPadding(dp: androidx.compose.ui.unit.Dp): Modifier {
    return this.padding(dp)
}
