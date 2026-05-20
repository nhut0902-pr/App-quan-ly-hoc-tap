package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Flashcard
import com.example.data.database.FlashcardDeck
import com.example.data.database.Subject
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(viewModel: StudyViewModel) {
    val decks by viewModel.decks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedDeckId by viewModel.selectedDeckId.collectAsState()
    val flashcards by viewModel.currentFlashcards.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()

    var showAddDeckDialog by remember { mutableStateOf(false) }

    val currentDeck = decks.find { it.id == selectedDeckId }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = currentDeck?.title ?: "Bộ thẻ ghi nhớ",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (selectedDeckId != null) {
                        IconButton(onClick = { viewModel.selectDeck(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại")
                        }
                    }
                },
                actions = {
                    if (selectedDeckId == null) {
                        IconButton(onClick = { showAddDeckDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Tạo bộ thẻ", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedDeckId == null) {
                // --- DECKS VIEW SELECTOR ---
                if (decks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.4f))
                            Text("Chưa có bộ thẻ ghi nhớ nào!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            Button(onClick = { showAddDeckDialog = true }) {
                                Text("Tạo bộ thẻ đầu tiên")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(decks) { deck ->
                            val sub = subjects.find { it.id == deck.subjectId }
                            DeckFolderItem(
                                deck = deck,
                                subject = sub,
                                onSelect = { viewModel.selectDeck(deck.id) },
                                onDelete = { viewModel.deleteDeck(it) }
                            )
                        }
                    }
                }
            } else {
                // --- ACTIVE DECK QUIZ SCREEN ---
                val deckIdNotNull = selectedDeckId ?: 0
                FlashcardsQuizView(
                    deckId = deckIdNotNull,
                    flashcards = flashcards,
                    aiLoading = aiLoading,
                    aiResponse = aiResponse,
                    onAddCard = { front, back -> viewModel.addFlashcard(deckIdNotNull, front, back) },
                    onToggleMemorized = { viewModel.toggleFlashcardMemorized(it) },
                    onDeleteCard = { viewModel.deleteFlashcard(it) },
                    onGenerateAi = { topic -> viewModel.generateAiFlashcardsForTopic(topic, deckIdNotNull) },
                    onClearStatus = { viewModel.clearChatHistory() }
                )
            }
        }
    }

    if (showAddDeckDialog) {
        val subjectOptions = subjects
        AddDeckDialog(
            subjectOptions = subjectOptions,
            onDismiss = { showAddDeckDialog = false },
            onAdd = { title, desc, subjectId ->
                viewModel.addDeck(title, desc, subjectId)
                showAddDeckDialog = false
            }
        )
    }
}

// --- DECK ITEM DESIGN ---
@Composable
fun DeckFolderItem(
    deck: FlashcardDeck,
    subject: Subject?,
    onSelect: () -> Unit,
    onDelete: (FlashcardDeck) -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (subject != null) Color(subject.color) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                IconButton(
                    onClick = { showConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

            Column {
                Text(
                    deck.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (deck.description.isNotEmpty()) {
                    Text(
                        deck.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (subject != null) {
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(subject.color),
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Xóa bộ thẻ?") },
            text = { Text("Hành động này sẽ xóa bộ thẻ \"${deck.title}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(deck); showConfirm = false }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Hủy") }
            }
        )
    }
}

// --- QUIZ VIEW ENGINE --
@Composable
fun FlashcardsQuizView(
    deckId: Int,
    flashcards: List<Flashcard>,
    aiLoading: Boolean,
    aiResponse: String,
    onAddCard: (String, String) -> Unit,
    onToggleMemorized: (Flashcard) -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onGenerateAi: (String) -> Unit,
    onClearStatus: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var rotatedStateFlip by remember { mutableStateOf(false) } // Front side (false) vs back side (true)

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAiTopicDialog by remember { mutableStateOf(false) }

    // Synchronize current index safety when flashcards count changes
    LaunchedEffect(flashcards.size) {
        if (currentIndex >= flashcards.size) {
            currentIndex = (flashcards.size - 1).coerceAtLeast(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- DECK CONTROLS ROW ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mục: Thẻ ${if (flashcards.isNotEmpty()) currentIndex + 1 else 0} / ${flashcards.size}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // AI generate button
                Button(
                    onClick = { showAiTopicDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Tạo Thẻ")
                }

                FilledIconButton(onClick = { showAddCardDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm thẻ thủ công")
                }
            }
        }

        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bộ thẻ trống.", color = MaterialTheme.colorScheme.outline)
                    Text("Hãy chọn Thêm thẻ thủ công hoặc dùng AI tự tạo bộ câu hỏi!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                }
            }
        } else {
            val currentCard = flashcards[currentIndex]

            // Card ROTATION/FLIP Animation
            val angleAnim by animateFloatAsState(
                targetValue = if (rotatedStateFlip) 180f else 0f,
                animationSpec = tween(durationMillis = 500)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        rotationY = angleAnim
                        cameraDistance = 12f * density
                    }
                    .clickable { rotatedStateFlip = !rotatedStateFlip },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentCard.memorized) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Turn text back 180 deg if we are showing the back
                    val showingBack = angleAnim > 90f
                    if (showingBack) {
                        Column(
                            modifier = Modifier
                                .graphicsLayer { rotationY = 180f }
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "MẶT SAU (TRẢ LỜI):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 2.sp
                            )
                            Text(
                                currentCard.back,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "MẶT TRƯỚC (CÂU HỎI / KHÁI NIỆM):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 2.sp
                            )
                            Text(
                                currentCard.front,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Chạm để lật xem câu trả lời",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Bottom info label top right
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { onDeleteCard(currentCard) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa thẻ", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
                        IconButton(onClick = { onToggleMemorized(currentCard) }) {
                            Icon(
                                if (currentCard.memorized) Icons.Default.CheckCircle
                                else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "",
                                tint = if (currentCard.memorized) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // NAVIGATION ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        rotatedStateFlip = false
                        currentIndex = if (currentIndex > 0) currentIndex - 1 else flashcards.size - 1
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trở lại")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        rotatedStateFlip = false
                        currentIndex = if (currentIndex < flashcards.size - 1) currentIndex + 1 else 0
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tiếp theo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { showAddCardDialog = false },
            onAdd = { front, back ->
                onAddCard(front, back)
                showAddCardDialog = false
            }
        )
    }

    if (showAiTopicDialog) {
        AiGenerateFlashcardsDialog(
            aiLoading = aiLoading,
            aiResponse = aiResponse,
            onDismiss = { showAiTopicDialog = false },
            onGenerate = { topic -> onGenerateAi(topic) }
        )
    }
}

// --- DIALOG POPUPS CREATOR ---

@Composable
fun AddDeckDialog(
    subjectOptions: List<Subject>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<Subject?>(subjectOptions.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Bộ Thẻ Mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên bộ thẻ (e.g. Giải tích 1)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả ngắn gọn") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Gán vào môn học:", style = MaterialTheme.typography.labelLarge)
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
                    val subId = selectedSubject?.id ?: 0
                    if (title.isNotBlank()) {
                        onAdd(title, description, subId)
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Tạo Folder") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm thẻ mới bằng tay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Mặt trước (Khái niệm / Câu hỏi)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Mặt sau (Định nghĩa / Câu trả lời)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (front.isNotBlank() && back.isNotBlank()) onAdd(front, back) },
                enabled = front.isNotBlank() && back.isNotBlank()
            ) { Text("Lưu thẻ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AiGenerateFlashcardsDialog(
    aiLoading: Boolean,
    aiResponse: String,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var topic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trợ lý AI tạo Flashcards") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Nhập chủ đề kiến thức bất kỳ và AI EduSmart sẽ tự phân tách thành 5 câu hỏi Flashcard súc tích cho bạn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Chủ đề (Ví dụ: Các loại từ loại tiếng Anh)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !aiLoading
                )

                if (aiLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AI đang suy nghĩ và phân tích dữ liệu...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (aiResponse.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(aiResponse, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (topic.isNotBlank()) onGenerate(topic) },
                enabled = topic.isNotBlank() && !aiLoading
            ) {
                Text("Tạo flashcard bằng AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !aiLoading) { Text("Đóng") }
        }
    )
}
