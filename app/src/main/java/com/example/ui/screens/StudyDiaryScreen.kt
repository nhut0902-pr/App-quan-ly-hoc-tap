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
import com.example.data.database.StudyDiary
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDiaryScreen(viewModel: StudyViewModel) {
    val diaries by viewModel.diaries.collectAsState()
    var showAddDiaryDialog by remember { mutableStateOf(false) }
    
    // Day, Week, Month, All
    var selectedFilterTab by remember { mutableStateOf("ALL") }

    val filteredDiaries = remember(diaries, selectedFilterTab) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        when (selectedFilterTab) {
            "DAY" -> diaries.filter { now - it.date <= oneDayMs }
            "WEEK" -> diaries.filter { now - it.date <= 7 * oneDayMs }
            "MONTH" -> diaries.filter { now - it.date <= 30 * oneDayMs }
            else -> diaries
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Filter Selector Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bộ lọc:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                val filters = listOf(
                    "ALL" to "Tất cả",
                    "DAY" to "Theo Ngày",
                    "WEEK" to "Theo Tuần",
                    "MONTH" to "Theo Tháng"
                )
                
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilterTab == key,
                        onClick = { selectedFilterTab = key },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_diary_$key")
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredDiaries.isEmpty()) {
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
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Chưa có nhật ký học tập nào!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Hãy ghi lại tiến trình học tập hàng ngày để tự đánh giá kết quả và cải thiện nhé.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddDiaryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Viết nhật ký đầu tiên")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredDiaries) { diary ->
                        DiaryCardItem(
                            diary = diary,
                            onDelete = { viewModel.deleteStudyDiary(it) }
                        )
                    }
                }
            }

            // Floating action button inside tab contents to write diary
            if (filteredDiaries.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDiaryDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("add_diary_fab"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.BorderColor, contentDescription = "Viết nhật ký")
                }
            }
        }
    }

    if (showAddDiaryDialog) {
        WriteDiaryDialog(
            onDismiss = { showAddDiaryDialog = false },
            onSave = { activities, knowledge, difficulties, mood ->
                viewModel.addStudyDiary(activities, knowledge, difficulties, mood)
                showAddDiaryDialog = false
            }
        )
    }
}

@Composable
fun DiaryCardItem(
    diary: StudyDiary,
    onDelete: (StudyDiary) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale("vi", "VN"))
    val dateStr = sdf.format(Date(diary.date))

    val moodColor = when (diary.mood) {
        "Vui vẻ" -> Color(0xFF4CAF50)
        "Tập trung" -> Color(0xFF2196F3)
        "Mệt mỏi" -> Color(0xFFFF9800)
        "Thách thức" -> Color(0xFFE91E63)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date, Mood avatar, and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(moodColor.copy(alpha = 0.15f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (diary.mood) {
                                "Vui vẻ" -> Icons.Default.SentimentVerySatisfied
                                "Tập trung" -> Icons.Default.FilterCenterFocus
                                "Mệt mỏi" -> Icons.Default.SentimentVeryDissatisfied
                                "Thách thức" -> Icons.Default.MoodBad
                                else -> Icons.Default.SentimentSatisfied
                            },
                            contentDescription = diary.mood,
                            tint = moodColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dateStr.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Trạng thái: ${diary.mood}",
                            style = MaterialTheme.typography.bodySmall,
                            color = moodColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa nhật ký",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Study Activities (Hoạt động học tập)
            DiarySection(
                icon = Icons.Default.MenuBook,
                iconColor = Color(0xFF3F51B5),
                title = "Hoạt động học tập hôm nay:",
                content = diary.activities
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Knowledge acquired (Kiến thức thu được)
            DiarySection(
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF4CAF50),
                title = "Kiến thức/Thành tựu mới thu được:",
                content = diary.knowledgeAcquired
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Difficulties (Khó khăn gặp phải)
            DiarySection(
                icon = Icons.Default.Warning,
                iconColor = Color(0xFFF44336),
                title = "Khó khăn/Trở ngại gặp phải:",
                content = diary.difficulties
            )
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xóa nhật kỳ học tập") },
            text = { Text("Bạn có chắc chắn muốn xóa bài viết nhật kỳ ngày \"${dateStr}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(diary); showConfirmDelete = false }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun DiarySection(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = if (content.isNotBlank()) content else "(Không ghi lại tóm tắt)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (content.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun WriteDiaryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var activities by remember { mutableStateOf("") }
    var knowledge by remember { mutableStateOf("") }
    var difficulties by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("Bình thường") }

    val moods = listOf(
        "Vui vẻ" to Icons.Default.SentimentVerySatisfied,
        "Tập trung" to Icons.Default.FilterCenterFocus,
        "Mệt mỏi" to Icons.Default.SentimentVeryDissatisfied,
        "Thách thức" to Icons.Default.MoodBad,
        "Bình thường" to Icons.Default.SentimentSatisfied
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Viết Nhật Ký Học Tập", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mood Selection Flow Row
                Text("Cảm xúc học tập hôm nay:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    moods.forEach { (moodName, moodIcon) ->
                        val isSel = selectedMood == moodName
                        val color = when (moodName) {
                            "Vui vẻ" -> Color(0xFF4CAF50)
                            "Tập trung" -> Color(0xFF2196F3)
                            "Mệt mỏi" -> Color(0xFFFF9800)
                            "Thách thức" -> Color(0xFFE91E63)
                            else -> Color(0xFF9E9E9E)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) color.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedMood = moodName }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(moodIcon, contentDescription = moodName, tint = color, modifier = Modifier.size(22.dp))
                                Text(moodName, fontSize = 9.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = activities,
                    onValueChange = { activities = it },
                    label = { Text("Hôm nay bạn học những hoạt động gì? (*)") },
                    placeholder = { Text("Ví dụ: Học 2 giờ môn Đại số, giải bài tâp lập trình python cơ bản...") },
                    modifier = Modifier.fillMaxWidth().testTag("diary_activities_input")
                )

                OutlinedTextField(
                    value = knowledge,
                    onValueChange = { knowledge = it },
                    label = { Text("Kiến thức mới thu được (*)") },
                    placeholder = { Text("Ví dụ: Hiểu rõ thuật toán quy hoạch động, cấu trúc ma trận chuyển vị...") },
                    modifier = Modifier.fillMaxWidth().testTag("diary_knowledge_input")
                )

                OutlinedTextField(
                    value = difficulties,
                    onValueChange = { difficulties = it },
                    label = { Text("Khó khăn hay rắc rối gặp phải") },
                    placeholder = { Text("Ví dụ: Vẫn còn mơ hồ với cách thiết lập tham số liên kết đa tầng...") },
                    modifier = Modifier.fillMaxWidth().testTag("diary_difficulties_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (activities.isNotBlank() && knowledge.isNotBlank()) {
                        onSave(activities, knowledge, difficulties, selectedMood)
                    }
                },
                enabled = activities.isNotBlank() && knowledge.isNotBlank()
            ) {
                Text("Lưu Nhật Ký")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
