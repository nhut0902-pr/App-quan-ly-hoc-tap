package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val timerTimeRemaining by viewModel.timerTimeRemaining.collectAsState()
    val timerIsRunning by viewModel.timerIsRunning.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val selectedSubjectIdForTimer by viewModel.selectedSubjectIdForTimer.collectAsState()

    var showSubjectSelectorDialog by remember { mutableStateOf(false) }

    val formattedTime = remember(timerTimeRemaining) {
        val minutes = timerTimeRemaining / 60
        val seconds = timerTimeRemaining % 60
        "%02d:%02d".format(minutes, seconds)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Đồng hồ Pomodoro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Selected Subject indicator
            val currentTimerSubject = subjects.find { it.id == selectedSubjectIdForTimer }
            Button(
                onClick = { showSubjectSelectorDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTimerSubject != null) Color(currentTimerSubject.color)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentTimerSubject != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentTimerSubject?.name ?: "Nhấp chọn môn học để ghi nhận giờ")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer display circle
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle thread
                val strokeColor = MaterialTheme.colorScheme.surfaceVariant
                val activeThemeColor = if (timerMode == StudyViewModel.TimerMode.WORK) MaterialTheme.colorScheme.primary
                else Color(0xFF4CAF50)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = strokeColor,
                        style = Stroke(width = 10.dp.toPx())
                    )
                }

                // Dynamic Progress Arc
                val targetMaxSeconds = if (timerMode == StudyViewModel.TimerMode.WORK) 25 * 60f else 5 * 60f
                val sweepAngle = (timerTimeRemaining / targetMaxSeconds) * 360f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = activeThemeColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Time indicators
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (timerMode == StudyViewModel.TimerMode.WORK) "TẬP TRUNG" else "GIẢI LAO",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = activeThemeColor,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Phút : Giây",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Play/Pause and Reset buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RESET
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Đặt lại", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // PLAY/PAUSE Button
                val activeCircleColor = if (timerMode == StudyViewModel.TimerMode.WORK) MaterialTheme.colorScheme.primary
                else Color(0xFF4CAF50)

                IconButton(
                    onClick = {
                        if (timerIsRunning) viewModel.stopTimer()
                        else viewModel.startTimer()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(activeCircleColor, CircleShape)
                        .testTag("on_off_pomodoro_button")
                ) {
                    Icon(
                        if (timerIsRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Chạy/Tạm dừng",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // FAST COMPLETE WORK MOCK (For quick testing & checking stats)
                IconButton(
                    onClick = { viewModel.configureTimer(1) }, // configure to 1 minute to test quickly!
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = "Thử nhanh 1 phút", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Premade Duration selection
            Text("Chọn thời lượng học tập khác:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.configureTimer(mins) },
                        label = { Text("$mins Phút") }
                    )
                }
            }
        }
    }

    if (showSubjectSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showSubjectSelectorDialog = false },
            title = { Text("Chọn môn học ghi nhận giờ") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectSubjectForTimer(null)
                                showSubjectSelectorDialog = false
                            }
                            .padding(12.dp)
                    ) {
                        RadioButton(selected = selectedSubjectIdForTimer == null, onClick = { viewModel.selectSubjectForTimer(null); showSubjectSelectorDialog = false })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Không liên kết (Khác)")
                    }

                    subjects.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSubjectForTimer(sub.id)
                                    showSubjectSelectorDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedSubjectIdForTimer == sub.id,
                                onClick = {
                                    viewModel.selectSubjectForTimer(sub.id)
                                    showSubjectSelectorDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(sub.color)))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(sub.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSubjectSelectorDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}
