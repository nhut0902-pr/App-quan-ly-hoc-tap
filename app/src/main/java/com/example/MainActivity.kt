package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.StudyDatabase
import com.example.data.repository.StudyRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room Database, DAO and Repository
    val database = StudyDatabase.getDatabase(applicationContext)
    val dao = database.studyDao()
    val repository = StudyRepository(dao)

    setContent {
      MyApplicationTheme {
        // Simple ViewModel injection directly
        val viewModel = StudyViewModel(application, repository)
        var selectedItemIndex by remember { mutableStateOf(0) }

        val activeAlarm by viewModel.activeReminderTriggered.collectAsState()
        if (activeAlarm != null) {
          AlertDialog(
            onDismissRequest = { viewModel.dismissReminderTriggered() },
            title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nhắc Nhở Học Tập!", fontWeight = FontWeight.Bold)
              }
            },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeAlarm!!.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (activeAlarm!!.description.isNotEmpty()) {
                  Text(activeAlarm!!.description, style = MaterialTheme.typography.bodyMedium)
                }
                Text("⚠️ Hãy chuẩn bị sẵn sàng bài vở và nhiệm vụ sắp tới nhé!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              }
            },
            confirmButton = {
              Button(onClick = { viewModel.dismissReminderTriggered() }) {
                Text("Đã hoàn tất")
              }
            }
          )
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            NavigationBar(
              modifier = Modifier.testTag("app_navigation_bar")
            ) {
              val items = listOf(
                NavigationItem("T.Quan", Icons.Default.Dashboard, Icons.Outlined.Dashboard, 0),
                NavigationItem("Lịch học", Icons.Default.CalendarMonth, Icons.Outlined.CalendarMonth, 1),
                NavigationItem("Pomodoro", Icons.Default.Timelapse, Icons.Outlined.Timelapse, 2),
                NavigationItem("Thẻ nhớ", Icons.Default.FolderSpecial, Icons.Outlined.FolderSpecial, 3),
                NavigationItem("Tiện ích+", Icons.Default.GridView, Icons.Outlined.GridView, 4)
              )

              items.forEach { item ->
                NavigationBarItem(
                  icon = {
                    Icon(
                      imageVector = if (selectedItemIndex == item.index) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.title
                    )
                  },
                  label = { Text(item.title) },
                  selected = selectedItemIndex == item.index,
                  onClick = { selectedItemIndex = item.index },
                  modifier = Modifier.testTag("navigation_item_${item.index}")
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (selectedItemIndex) {
              0 -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTimer = { selectedItemIndex = 2 },
                onNavigateToAssignments = { selectedItemIndex = 4 } // redirects to utility workspace tab
              )
              1 -> TimetableScreen(viewModel = viewModel)
              2 -> PomodoroScreen(viewModel = viewModel)
              3 -> FlashcardScreen(viewModel = viewModel)
              4 -> WorkspaceTabsScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}

data class NavigationItem(
  val title: String,
  val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  val index: Int
)

// --- WORKSPACE UTILITY TABS CODES (Tab 4 combining other features) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTabsScreen(viewModel: StudyViewModel) {
  var activeSubTabState by remember { mutableStateOf(0) } // 0: Assignments, 1: Notes/Grades, 2: Diary, 3: Reminders, 4: AI Advisor

  Column(modifier = Modifier.fillMaxSize()) {
    ScrollableTabRow(
      selectedTabIndex = activeSubTabState,
      containerColor = MaterialTheme.colorScheme.background,
      modifier = Modifier.testTag("workspace_tabs"),
      edgePadding = 16.dp
    ) {
      Tab(
        selected = activeSubTabState == 0,
        onClick = { activeSubTabState = 0 },
        text = { Text("Bài tập & Mục tiêu", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = activeSubTabState == 1,
        onClick = { activeSubTabState = 1 },
        text = { Text("Sổ & GPA", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = activeSubTabState == 2,
        onClick = { activeSubTabState = 2 },
        text = { Text("Nhật ký học", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = activeSubTabState == 3,
        onClick = { activeSubTabState = 3 },
        text = { Text("Trợ lý Nhắc nhở", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = activeSubTabState == 4,
        onClick = { activeSubTabState = 4 },
        text = { Text("Trợ lý AI", fontWeight = FontWeight.Bold) }
      )
    }

    Box(modifier = Modifier.weight(1f)) {
      when (activeSubTabState) {
        0 -> AssignmentsScreen(viewModel = viewModel)
        1 -> NotesGradesScreen(viewModel = viewModel)
        2 -> StudyDiaryScreen(viewModel = viewModel)
        3 -> ReminderScreen(viewModel = viewModel)
        4 -> AiAssistantScreen(viewModel = viewModel)
      }
    }
  }
}
