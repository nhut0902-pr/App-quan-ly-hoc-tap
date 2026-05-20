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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Grade
import com.example.data.database.Note
import com.example.data.database.Subject
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesGradesScreen(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gpaStats = viewModel.getGPAStats()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Notes, 1: Grades
    var showAddNote by remember { mutableStateOf(false) }
    var showAddGrade by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (activeSubTab == 0) "Sổ tay ghi chú" else "Bảng điểm & Học tập", fontWeight = FontWeight.Bold) },
                actions = {
                    if (activeSubTab == 0 && subjects.isNotEmpty()) {
                        IconButton(onClick = { showAddNote = true }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "Thêm ghi chú", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (activeSubTab == 1 && subjects.isNotEmpty()) {
                        IconButton(onClick = { showAddGrade = true }) {
                            Icon(Icons.Default.AddModerator, contentDescription = "Thêm điểm số", tint = MaterialTheme.colorScheme.primary)
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
            // sub tab switches
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ghi chú (${notes.size})")
                    }
                }
                Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bảng điểm & GPA")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeSubTab == 0) {
                // --- NOTES DIRECTORY LIST ---
                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Assignment, contentDescription = null, size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.4f))
                            Text("Chưa có ghi chú nào!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            if (subjects.isEmpty()) {
                                Text("Hãy tạo ít nhất một môn học để liên kết ghi chú nhé.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 24.dp))
                            } else {
                                Button(onClick = { showAddNote = true }) { Text("Tạo ghi chú đầu tiên") }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes) { note ->
                            val sub = subjects.find { it.id == note.subjectId }
                            NoteCardItem(
                                note = note,
                                subject = sub,
                                onDelete = { viewModel.deleteNote(it) }
                            )
                        }
                    }
                }
            } else {
                // --- GRADEBOOK SHEET ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // GPA Summary Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "BÁO CÁO HỌC LỰC TOÀN KHÓA",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "ĐTB Hệ 10:  %.2f".format(gpaStats.gpa10),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    )
                                    Text(
                                        "ĐTB Hệ 4:  %.2f".format(gpaStats.gpa4),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(0.15f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        gpaStats.rank.uppercase(),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    )
                                }
                            }
                        }
                    }

                    if (grades.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Leaderboard, contentDescription = null, size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.4f))
                                Text("Chưa ghi nhận điểm số môn học.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                                if (subjects.isNotEmpty()) {
                                    Button(onClick = { showAddGrade = true }) { Text("Ghi nhận điểm") }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(grades) { grade ->
                                val sub = subjects.find { it.id == grade.subjectId }
                                GradeRowItem(
                                    grade = grade,
                                    subject = sub,
                                    onDelete = { viewModel.deleteGrade(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNote) {
        val subjectOptions = subjects
        AddNoteDialog(
            subjectOptions = subjectOptions,
            onDismiss = { showAddNote = false },
            onAdd = { subjectId, title, content ->
                viewModel.addNote(subjectId, title, content)
                showAddNote = false
            }
        )
    }

    if (showAddGrade) {
        val subjectOptions = subjects
        AddGradeDialog(
            subjectOptions = subjectOptions,
            onDismiss = { showAddGrade = false },
            onAdd = { subjectId, name, score, weight ->
                viewModel.addGrade(subjectId, name, score, weight)
                showAddGrade = false
            }
        )
    }
}

// --- SUBROW COMPOSABLES ---

@Composable
fun NoteCardItem(
    note: Note,
    subject: Subject?,
    onDelete: (Note) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(note.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (subject != null) {
                            Text(
                                subject.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(subject.color)
                            )
                        }
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )

            if (!isExpanded && note.content.length > 80) {
                Text(
                    "Nhấp để mở rộng...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Xác nhận xóa ghi chú \"${note.title}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(note); showConfirmDelete = false }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun GradeRowItem(
    grade: Grade,
    subject: Subject?,
    onDelete: (Grade) -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(subject?.color ?: 0xFF9E9E9E.toInt()))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "${subject?.name ?: "Khác"} - ${grade.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Hệ số trọng số: ${(grade.weight * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%.1f".format(grade.score),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (grade.score >= 8.0f) Color(0xFF4CAF50) else if (grade.score >= 5.0f) MaterialTheme.colorScheme.primary else Color.Red
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa điểm", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Xóa điểm học?") },
            text = { Text("Quay lại điểm số này?") },
            confirmButton = {
                TextButton(onClick = { onDelete(grade); showConfirm = false }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Hủy") }
            }
        )
    }
}

// --- POPUP DIALOGS ---

@Composable
fun AddNoteDialog(
    subjectOptions: List<Subject>,
    onDismiss: () -> Unit,
    onAdd: (Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjectOptions.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo ghi chú mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề bài học (*)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nội dung chi tiết") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Text("Liên kết Môn học:", style = MaterialTheme.typography.labelLarge)
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
                    val subId = selectedSubject?.id
                    if (title.isNotBlank() && subId != null) {
                        onAdd(subId, title, content)
                    }
                },
                enabled = title.isNotBlank() && selectedSubject != null
            ) { Text("Lưu nháp") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddGradeDialog(
    subjectOptions: List<Subject>,
    onDismiss: () -> Unit,
    onAdd: (Int, String, Float, Float) -> Unit
) {
    var name by remember { mutableStateOf("Điểm cuối kỳ") }
    var rawScore by remember { mutableStateOf("8.5") }
    var rawWeight by remember { mutableStateOf("30") } // weight % (e.g. 30%)
    var selectedSubject by remember { mutableStateOf(subjectOptions.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi nhận điểm số") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên cột điểm (Ví dụ: Giữa kỳ)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rawScore,
                        onValueChange = { rawScore = it },
                        label = { Text("Điểm số (Thang 10)") },
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = rawWeight,
                        onValueChange = { rawWeight = it },
                        label = { Text("Trọng số %") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Áp dụng môn học:", style = MaterialTheme.typography.labelLarge)
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
                    val score = rawScore.toFloatOrNull() ?: 0.0f
                    val weightPct = rawWeight.toFloatOrNull() ?: 100.0f
                    val weight = weightPct / 100.0f
                    val subId = selectedSubject?.id
                    if (name.isNotBlank() && subId != null) {
                        onAdd(subId, name, score, weight)
                    }
                },
                enabled = name.isNotBlank() && selectedSubject != null
            ) { Text("Ghi nhận") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
