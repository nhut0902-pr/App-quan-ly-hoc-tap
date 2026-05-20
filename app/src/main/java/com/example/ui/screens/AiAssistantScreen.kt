package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(viewModel: StudyViewModel) {
    val chatHistory by viewModel.aiChatHistory.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    var questionInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Slide down chat to target bottom on load / replies
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val suggestionChips = listOf(
        "Tóm tắt khái niệm quang hợp sinh học",
        "Lập kế hoạch tự học tiếng Nhật trong 1 tháng",
        "Phương pháp ôn thi đạt điểm cao cho học sinh",
        "Giải thích giải thuật tìm kiếm nhị phân"
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Trợ lý Học thuật AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearChatHistory() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Dọn sạch hội thoại", tint = MaterialTheme.colorScheme.error)
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
            if (chatHistory.isEmpty()) {
                // --- INTRO WELCOME BOARD ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            "Hỏi đáp thông minh cùng AI EduSmart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            "Bạn có câu hỏi khoa học hay bài tập cần giải thích chi tiết súc tích? Hãy đặt câu hỏi cho tôi bên dưới.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Hoặc thử bấm chọn gợi ý nhanh:",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        // Suggestion prompts
                        FlowRowSuggestionsGrid(
                            suggestions = suggestionChips,
                            onSelect = {
                                questionInput = it
                                viewModel.askAi(it)
                            }
                        )
                    }
                }
            } else {
                // --- CHAT CONVERSATION AREA ---
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatHistory) { (prompt, response) ->
                        // User prompt bubble
                        ChatBubbleItem(
                            text = prompt.replace("Bạn: ", ""),
                            isUser = true
                        )

                        if (response.isNotEmpty()) {
                            // AI response bubble
                            ChatBubbleItem(
                                text = response,
                                isUser = false
                            )
                        } else if (aiLoading) {
                            // Loading state bubble
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AI đang phân tích phản hồi...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- BOTTOM SEND FIELD CONTAINER ---
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding() // avoid gestural navigation overlapping!
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text("Hỏi AI điều gì đó...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_question_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !aiLoading,
                        trailingIcon = {
                            if (questionInput.isNotEmpty()) {
                                IconButton(onClick = { questionInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa")
                                }
                            }
                        },
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (questionInput.isNotBlank()) {
                                viewModel.askAi(questionInput)
                                questionInput = ""
                            }
                        },
                        enabled = questionInput.isNotBlank() && !aiLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (questionInput.isNotBlank() && !aiLoading) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Gửi tin",
                            tint = if (questionInput.isNotBlank() && !aiLoading) Color.White
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRowSuggestionsGrid(suggestions: List<String>, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        suggestions.forEach { tag ->
            Card(
                modifier = Modifier
                    .clickable { onSelect(tag) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(0.7f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "BẠN:" else "EDUSMART AI:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
