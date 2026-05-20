package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Subject
import com.example.data.database.StudyGoal
import com.example.data.database.StudySession
import com.example.ui.viewmodel.StudyViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToTimer: () -> Unit,
    onNavigateToAssignments: () -> Unit
) {
    val subjects by viewModel.subjects.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val sessions by viewModel.studySessions.collectAsState()
    val gpaStats = viewModel.getGPAStats()

    val pendingAssignments = assignments.filter { !it.isCompleted }
    val achievedGoals = goals.filter { it.isAchieved }

    // Calculate stats
    val totalFocusMinutes = sessions.sumOf { it.durationMinutes }
    val focusHoursToday = sessions
        .filter { isSessionToday(it.startTime) }
        .sumOf { it.durationMinutes } / 60f

    var showAddSubjectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Xin chào!",
                            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            "EduSmart Dashboard",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddSubjectDialog = true },
                        modifier = Modifier.testTag("add_subject_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Thêm môn học", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MAIN METRICS ROW ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Học hôm nay",
                        value = "%.1f h".format(focusHoursToday),
                        subtext = "Tổng: %.1f giờ".format(totalFocusMinutes / 60f),
                        icon = Icons.Default.Timer,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f).clickable { onNavigateToTimer() }
                    )

                    MetricCard(
                        title = "Bài tập chưa nộp",
                        value = "${pendingAssignments.size}",
                        subtext = "Cần hoàn thành",
                        icon = Icons.Default.Assignment,
                        color = MaterialTheme.colorScheme.errorContainer,
                        textColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f).clickable { onNavigateToAssignments() }
                    )
                }
            }

            // --- GPA CARD ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Điểm trung bình tích lũy",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "GPA: %.2f / 4.0".format(gpaStats.gpa4),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Thang 10: %.1f  •  Học lực: %s".format(gpaStats.gpa10, gpaStats.rank),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // --- CUSTOM CANVAS STUDY STATS CHART ---
            item {
                Text(
                    "Thống kê tập trung môn học (Phút)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                StudySessionsBarChart(sessions = sessions, subjects = subjects)
            }

            // --- GOAL TARGET PROGRESS TRACKER ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mục tiêu học tập",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Đã đạt: ${achievedGoals.size}/${goals.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (goals.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Flag,
                        message = "Chưa có mục tiêu. Hãy đặt mục tiêu học tập ở tab Mục tiêu!"
                    )
                }
            } else {
                items(goals.take(3)) { goal ->
                    GoalProgressItem(goal = goal)
                }
            }

            // --- ACTIVE SUBJECTS CATALOG ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Danh sách Môn học",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${subjects.size} môn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (subjects.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Book,
                        message = "Chưa có môn học nào. Nhấp dấu (+) phía trên để tạo môn mới."
                    )
                }
            } else {
                items(subjects) { subject ->
                    SubjectListItem(
                        subject = subject,
                        onDelete = { viewModel.deleteSubject(it) }
                    )
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onAdd = { name, teacher, color, room ->
                viewModel.addSubject(name, teacher, color, room)
                showAddSubjectDialog = false
            }
        )
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = textColor.copy(alpha = 0.8f))
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = textColor)
                Text(subtext, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun StudySessionsBarChart(sessions: List<StudySession>, subjects: List<Subject>) {
    val chartColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)

    // Aggregate sessions by subject name
    val subjectMinutes = remember(sessions, subjects) {
        val map = mutableMapOf<String, Int>()
        sessions.forEach { sess ->
            val subName = subjects.find { it.id == sess.subjectId }?.name ?: "Khác"
            map[subName] = (map[subName] ?: 0) + sess.durationMinutes
        }
        // Fill other active subjects with 0 if not present
        subjects.forEach { sub ->
            if (!map.containsKey(sub.name)) {
                map[sub.name] = 0
            }
        }
        map.toList().take(5) // Max 5 subjects on chart for spacing
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (subjectMinutes.isEmpty() || subjectMinutes.all { it.second == 0 }) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có thống kê học tập. Hãy bật Pomodoro để bắt đầu!", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val maxMinutes = subjectMinutes.maxOfOrNull { it.second }?.toFloat() ?: 60f
            val maxValue = if (maxMinutes < 60f) 60f else maxMinutes

            Column(modifier = Modifier.padding(16.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 30.dp.toPx()
                    val chartHeight = height - bottomPadding
                    val barSpacing = 40.dp.toPx()
                    val barWidth = 32.dp.toPx()

                    // Draw baseline axes
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, chartHeight),
                        end = Offset(width, chartHeight),
                        strokeWidth = 2f
                    )

                    val barCount = subjectMinutes.size
                    val segmentWidth = width / barCount

                    subjectMinutes.forEachIndexed { index, (subjectName, minutes) ->
                        val barHeightFactor = minutes.toFloat() / maxValue
                        val barHeight = chartHeight * barHeightFactor
                        val xOffset = index * segmentWidth + (segmentWidth - barWidth) / 2

                        // Draw Bar
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(chartColor, chartColor.copy(alpha = 0.5f))
                            ),
                            topLeft = Offset(xOffset, chartHeight - barHeight),
                            size = Size(barWidth, barHeight)
                        )

                        // Draw minutes labels
                        if (minutes > 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "$minutes m",
                                xOffset + barWidth / 2 - 10f,
                                chartHeight - barHeight - 10f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 28f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }

                        // Draw subject name label
                        drawContext.canvas.nativeCanvas.drawText(
                            if (subjectName.length > 8) subjectName.take(6) + ".." else subjectName,
                            xOffset + barWidth / 2,
                            height - 5f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalProgressItem(goal: StudyGoal) {
    val progress = if (goal.targetHours > 0) goal.currentHours / goal.targetHours else 0f
    val displayProgress = progress.coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (goal.isAchieved) Icons.Default.CheckCircle else Icons.Default.Flag,
                        contentDescription = null,
                        tint = if (goal.isAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "%.1f/%.1f giờ".format(goal.currentHours, goal.targetHours),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { displayProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (goal.isAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun SubjectListItem(subject: Subject, onDelete: (Subject) -> Unit) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(subject.color), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(subject.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (subject.teacher.isNotEmpty()) {
                        Text("GV: ${subject.teacher}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (subject.room.isNotEmpty()) {
                        Text("Phòng: ${subject.room}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            IconButton(onClick = { showConfirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa môn", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa môn học \"${subject.name}\"? Hành động này sẽ không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(subject)
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
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// --- ADD SUBJECT DIALOG ---
@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onAdd: (String, String, Int, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF2196F3.toInt()) }

    val colorsList = listOf(
        0xFFF44336.toInt(), // Red
        0xFFE91E63.toInt(), // Pink
        0xFF9C27B0.toInt(), // Purple
        0xFF673AB7.toInt(), // Deep Purple
        0xFF3F51B5.toInt(), // Blue
        0xFF2196F3.toInt(), // Light Blue
        0xFF03A9F4.toInt(), // Cyan
        0xFF009688.toInt(), // Teal
        0xFF4CAF50.toInt(), // Green
        0xFFFF9800.toInt()  // Orange
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Môn Học Mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên môn học (*)") },
                    modifier = Modifier.fillMaxWidth().testTag("subject_name_input")
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Giảng viên/Giáo viên") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Phòng học / Link online") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Color Select Row
                Text("Chọn màu đại diện:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorsList.take(5).forEach { color ->
                        ColorSelectBubble(color = color, isSelected = selectedColor == color) {
                            selectedColor = color
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorsList.drop(5).forEach { color ->
                        ColorSelectBubble(color = color, isSelected = selectedColor == color) {
                            selectedColor = color
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, teacher, selectedColor, room) },
                enabled = name.isNotBlank()
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun ColorSelectBubble(color: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(color))
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

fun isSessionToday(startTime: Long): Boolean {
    val sessionCal = Calendar.getInstance().apply { timeInMillis = startTime }
    val todayCal = Calendar.getInstance()
    return sessionCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
           sessionCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
}
