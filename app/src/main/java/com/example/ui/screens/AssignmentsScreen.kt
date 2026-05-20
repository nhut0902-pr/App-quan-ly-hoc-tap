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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.database.Assignment
import com.example.data.database.Subject
import com.example.data.database.StudyGoal
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val goals by viewModel.goals.collectAsState()

    var activeTabState by remember { mutableStateOf(0) } // 0: Assignments, 1: Goals
    var showAddAssignment by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (activeTabState == 0) "Danh sách bài tập" else "Mục tiêu học tập", fontWeight = FontWeight.Bold) },
                actions = {
                    if (activeTabState == 0 && subjects.isNotEmpty()) {
                        IconButton(onClick = { showAddAssignment = true }, modifier = Modifier.testTag("add_assignment_btn")) {
                            Icon(Icons.Default.AddTask, contentDescription = "Thêm bài kiểm tra", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (activeTabState == 1) {
                        IconButton(onClick = { showAddGoal = true }) {
                            Icon(Icons.Default.Flag, contentDescription = "Thêm mục tiêu", tint = MaterialTheme.colorScheme.primary)
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
            // Tab Selector
            TabRow(
                selectedTabIndex = activeTabState,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(selected = activeTabState == 0, onClick = { activeTabState = 0 }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bài tập (${assignments.count { !it.isCompleted }})")
                    }
                }
                Tab(selected = activeTabState == 1, onClick = { activeTabState = 1 }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mục tiêu (${goals.count { !it.isAchieved }})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTabState == 0) {
                // ASSIGNMENTS PORTION
                if (assignments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.4f))
                            Text("Chưa có bài tập nào cần hoàn thành!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            if (subjects.isEmpty()) {
                                Text("Hãy tạo môn học trước ở Dashboard để gán bài tập.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                Button(onClick = { showAddAssignment = true }) { Text("Thêm bài đầu tiên") }
                            }
                        }
                    }
                } else {
                    val ongoingAssignments = assignments.filter { !it.isCompleted }.sortedBy { it.dueDate }
                    val completedAssignments = assignments.filter { it.isCompleted }.sortedByDescending { it.dueDate }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (ongoingAssignments.isNotEmpty()) {
                            item {
                                Text("Chưa hoàn thành", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(ongoingAssignments) { assignment ->
                                val sub = subjects.find { it.id == assignment.subjectId }
                                AssignmentCard(
                                    assignment = assignment,
                                    subject = sub,
                                    onToggle = { viewModel.toggleAssignmentCompleted(it) },
                                    onAddReminder = { title, type, target, lead, desc ->
                                        viewModel.addReminder(title, type, target, lead, desc)
                                    },
                                    onDelete = { viewModel.deleteAssignment(it) }
                                )
                            }
                        }

                        if (completedAssignments.isNotEmpty()) {
                            item {
                                Text("Đã hoàn thành", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            }
                            items(completedAssignments) { assignment ->
                                val sub = subjects.find { it.id == assignment.subjectId }
                                AssignmentCard(
                                    assignment = assignment,
                                    subject = sub,
                                    onToggle = { viewModel.toggleAssignmentCompleted(it) },
                                    onAddReminder = { title, type, target, lead, desc ->
                                        viewModel.addReminder(title, type, target, lead, desc)
                                    },
                                    onDelete = { viewModel.deleteAssignment(it) }
                                )
                            }
                        }
                    }
                }
            } else {
                // STUDY GOALS PORTION
                if (goals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Flag, contentDescription = null, size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.4f))
                            Text("Chưa có mục tiêu học tập nào!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            Text("Hãy thiết lập kế hoạch tích lũy giờ tập trung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddGoal = true }) { Text("Thêm mục tiêu lý tưởng") }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(goals) { goal ->
                            GoalCardItem(
                                goal = goal,
                                onPlusOneHour = { viewModel.updateGoalHours(it, 1.0f) },
                                onToggleCourseCompleted = { viewModel.toggleGoalCourseCompleted(it) },
                                onDelete = { viewModel.deleteGoal(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddAssignment) {
        val subjectOptions = subjects
        AddAssignmentDialog(
            subjectOptions = subjectOptions,
            onDismiss = { showAddAssignment = false },
            onAdd = { subjectId, title, desc, dueTimestamp, priority ->
                viewModel.addAssignment(subjectId, title, desc, dueTimestamp, priority)
                showAddAssignment = false
            }
        )
    }

    if (showAddGoal) {
        AddGoalDialog(
            subjectOptions = subjects,
            onDismiss = { showAddGoal = false },
            onAdd = { title, targetHrs, deadline, type, targetScore, targetSubject ->
                viewModel.addGoal(
                    title = title,
                    targetHours = targetHrs,
                    deadline = deadline,
                    type = type,
                    targetScore = targetScore,
                    targetSubjectName = targetSubject
                )
                showAddGoal = false
            }
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    subject: Subject?,
    onToggle: (Assignment) -> Unit,
    onAddReminder: (String, String, Long, Int, String) -> Unit,
    onDelete: (Assignment) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showQuickReminder by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(assignment.dueDate))

    val priorityColor = when (assignment.priority) {
        "Cao" -> Color(0xFFE53935)
        "Trung bình" -> Color(0xFFFFA000)
        else -> Color(0xFF43A047)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (assignment.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = assignment.isCompleted,
                    onCheckedChange = { onToggle(assignment) },
                    modifier = Modifier.testTag("assignment_checkbox_${assignment.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (assignment.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (subject != null) {
                            Text(
                                subject.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(subject.color)
                            )
                        }
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text("Hạn: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (assignment.description.isNotEmpty()) {
                        Text(
                            assignment.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Priority Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(assignment.priority, style = MaterialTheme.typography.labelSmall, color = priorityColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showQuickReminder = true }) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Cài nhắc nhở", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa bài tập", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            }
        }
    }

    if (showQuickReminder) {
        var leadMins by remember { mutableStateOf(60) } // Default 1 hour
        AlertDialog(
            onDismissRequest = { showQuickReminder = false },
            title = { Text("⏰ Cài Nhắc Nhở Deadline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bài tập: ${assignment.title}", fontWeight = FontWeight.Bold)
                    Text("Cảnh báo sớm sẽ nhắc bạn thực thi và nộp bài trước khi quá muộn.")
                    Text("Khoảng thời gian báo trước:", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15 to "15p", 30 to "30p", 60 to "1h", 180 to "3h", 1440 to "1 ngày").forEach { (m, label) ->
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
                        onAddReminder(
                            "Bài tập sắp tới hạn: ${assignment.title}",
                            "DEADLINE BÀI TẬP",
                            assignment.dueDate,
                            leadMins,
                            "Môn học: ${subject?.name ?: "Tổng quan"}. Hãy lưu ý kiểm tra và nộp bài đúng giờ."
                        )
                        showQuickReminder = false
                    }
                ) {
                    Text("Thiết lập")
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
            title = { Text("Xóa bài tập?") },
            text = { Text("Xác nhận xóa bài tập \"${assignment.title}\"") },
            confirmButton = {
                TextButton(onClick = { onDelete(assignment); showConfirmDelete = false }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun GoalCardItem(
    goal: StudyGoal,
    onPlusOneHour: (StudyGoal) -> Unit,
    onToggleCourseCompleted: (StudyGoal) -> Unit,
    onDelete: (StudyGoal) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    // Progress computation depending on types
    val progress = when (goal.type) {
        "SCORE" -> if (goal.targetScore > 0f) goal.currentScore / goal.targetScore else 0f
        "COURSE" -> if (goal.courseCompleted) 1f else 0f
        else -> if (goal.targetHours > 0) goal.currentHours / goal.targetHours else 0f
    }
    val displayProgress = progress.coerceIn(0f, 1f)
    val isGoalAchieved = when (goal.type) {
        "SCORE" -> goal.currentScore >= goal.targetScore
        "COURSE" -> goal.courseCompleted
        else -> goal.currentHours >= goal.targetHours
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(goal.deadline))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (goal.type) {
                                "SCORE" -> Icons.Default.Grade
                                "COURSE" -> Icons.Default.School
                                else -> Icons.Default.Timer
                            },
                            contentDescription = null,
                            tint = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (goal.type) {
                                        "SCORE" -> Color(0xFFE0F2F1)
                                        "COURSE" -> Color(0xFFECEFF1)
                                        else -> Color(0xFFE8EAF6)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (goal.type) {
                                    "SCORE" -> "Điểm Số"
                                    "COURSE" -> "Khóa Học"
                                    else -> "Tập Trung"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    if (goal.type == "SCORE" && goal.targetSubjectName.isNotEmpty()) {
                        Text("Môn học: ${goal.targetSubjectName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Hạn chót: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (goal.type == "HOURS" && !isGoalAchieved) {
                        IconButton(onClick = { onPlusOneHour(goal) }) {
                            Icon(Icons.Default.Add, contentDescription = "Tích lũy 1 giờ", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (goal.type == "COURSE") {
                        IconButton(onClick = { onToggleCourseCompleted(goal) }) {
                            Icon(
                                if (isGoalAchieved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Đánh dấu hoàn thành",
                                tint = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showConfirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa mục tiêu", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (goal.type) {
                        "SCORE" -> "Tiến trình: %.1f / %.1f Điểm".format(goal.currentScore, goal.targetScore)
                        "COURSE" -> if (isGoalAchieved) "Hoàn thành khóa học!" else "Đang học khóa này.."
                        else -> "Tiến trình: %.1f / %.1f giờ".format(goal.currentHours, goal.targetHours)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { displayProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isGoalAchieved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xóa mục tiêu học tập") },
            text = { Text("Bạn có muốn xóa mục tiêu \"${goal.title}\" không?") },
            confirmButton = {
                TextButton(onClick = { onDelete(goal); showConfirmDelete = false }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Hủy") }
            }
        )
    }
}

// --- DIALOGS COMPOSABLES ---

@Composable
fun AddAssignmentDialog(
    subjectOptions: List<Subject>,
    onDismiss: () -> Unit,
    onAdd: (Int, String, String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjectOptions.firstOrNull()) }
    var priority by remember { mutableStateOf("Trung bình") }
    var rawDaysText by remember { mutableStateOf("3") } // days to due

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm bài tập mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên bài tập/Nhiệm vụ (*)") },
                    modifier = Modifier.fillMaxWidth().testTag("assignment_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Ghi chú bổ sung") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawDaysText,
                    onValueChange = { rawDaysText = it },
                    label = { Text("Hạn nộp (Số ngày sau hôm nay)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cấp thiết / Mức độ ưu tiên:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Thấp", "Trung bình", "Cao").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
                Text("Môn học gán:", style = MaterialTheme.typography.labelLarge)
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                    subjectOptions.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubject = sub }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedSubject?.id == sub.id, onClick = { selectedSubject = sub })
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(sub.color)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sub.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = rawDaysText.toIntOrNull() ?: 3
                    val dueTimestamp = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                    val subId = selectedSubject?.id
                    if (title.isNotBlank() && subId != null) {
                        onAdd(subId, title, description, dueTimestamp, priority)
                    }
                },
                enabled = title.isNotBlank() && selectedSubject != null
            ) { Text("Thêm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddGoalDialog(
    subjectOptions: List<Subject>,
    onDismiss: () -> Unit,
    onAdd: (String, Float, Long, String, Float, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("HOURS") } // "HOURS", "SCORE", "COURSE"
    var rawTargetText by remember { mutableStateOf("10") } // target hours
    var rawTargetScoreText by remember { mutableStateOf("8.0") } // target grade score
    var selectedSubject by remember { mutableStateOf(subjectOptions.firstOrNull()) }
    var rawDaysText by remember { mutableStateOf("7") } // deadline days

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thiết lập mục tiêu mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Goal type chips index selector
                Text("Phân loại mục tiêu học tập:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HOURS" to "Giờ học", "SCORE" to "Điểm số", "COURSE" to "Khóa học").forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = {
                                selectedType = typeKey
                                if (typeKey == "COURSE") {
                                    title = "Hoàn thành khóa học Swift"
                                } else if (typeKey == "SCORE") {
                                    title = "Đạt điểm cao môn " + (selectedSubject?.name ?: "")
                                } else {
                                    title = "Tập trung tự học"
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên mục tiêu học tập") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == "HOURS") {
                    OutlinedTextField(
                        value = rawTargetText,
                        onValueChange = { rawTargetText = it },
                        label = { Text("Số giờ tập trung mục tiêu (Giờ)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == "SCORE") {
                    OutlinedTextField(
                        value = rawTargetScoreText,
                        onValueChange = { rawTargetScoreText = it },
                        label = { Text("Điểm số trung bình mục tiêu (Thang 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (subjectOptions.isNotEmpty()) {
                        Text("Gắn kết với Môn học:")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        ) {
                            subjectOptions.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSubject = sub
                                            title = "Đạt điểm cao môn ${sub.name}"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedSubject?.id == sub.id,
                                        onClick = {
                                            selectedSubject = sub
                                            title = "Đạt điểm cao môn ${sub.name}"
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(sub.color)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(sub.name)
                                }
                            }
                        }
                    } else {
                        Text(
                            "Lưu ý: Bạn chưa cấu hình môn học nào. Hãy tạo các môn học trong trang chủ Dashboard trước để tự động đồng bộ gán mục tiêu điểm số.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (selectedType == "COURSE") {
                    Text(
                        "Đặt mục tiêu để hoàn chỉnh các chứng chỉ, bài khóa nghiên cứu, học liệu trực tuyến của bạn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedTextField(
                    value = rawDaysText,
                    onValueChange = { rawDaysText = it },
                    label = { Text("Thời hạn tích lũy (Số ngày)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hrs = if (selectedType == "HOURS") rawTargetText.toFloatOrNull() ?: 10f else 0f
                    val score = if (selectedType == "SCORE") rawTargetScoreText.toFloatOrNull() ?: 8f else 0f
                    val subName = if (selectedType == "SCORE") selectedSubject?.name ?: "" else ""
                    val days = rawDaysText.toIntOrNull() ?: 7
                    val deadline = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                    if (title.isNotBlank()) {
                        onAdd(title, hrs, deadline, selectedType, score, subName)
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Tạo mục tiêu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
