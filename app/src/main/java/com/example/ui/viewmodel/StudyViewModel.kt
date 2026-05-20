package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class StudyViewModel(application: Application, private val repository: StudyRepository) : AndroidViewModel(application) {

    // --- ROOM REACTIVE FLOWS ---
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleEvents: StateFlow<List<ScheduleEvent>> = repository.allScheduleEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<Assignment>> = repository.allAssignments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<StudyGoal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studySessions: StateFlow<List<StudySession>> = repository.allStudySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decks: StateFlow<List<FlashcardDeck>> = repository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grades: StateFlow<List<Grade>> = repository.allGrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaries: StateFlow<List<StudyDiary>> = repository.allDiaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<StudyReminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- REALTIME REMINDER TRIGGER FLOW ---
    private val _activeReminderTriggered = MutableStateFlow<StudyReminder?>(null)
    val activeReminderTriggered: StateFlow<StudyReminder?> = _activeReminderTriggered.asStateFlow()

    fun dismissReminderTriggered() {
        _activeReminderTriggered.value = null
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                // Safely load current reminders snapshot
                val currentReminders = repository.allReminders.firstOrNull() ?: emptyList()
                val toTrigger = currentReminders.find { r ->
                    !r.isTriggered && now >= (r.targetTime - r.leadMinutes * 60 * 1000) && now <= (r.targetTime + 12 * 60 * 60 * 1000L)
                }
                if (toTrigger != null) {
                    _activeReminderTriggered.value = toTrigger
                    repository.updateReminder(toTrigger.copy(isTriggered = true))
                }
                delay(8000) // check every 8 seconds
            }
        }
    }

    // --- Dynamic Filtered States ---
    private val _selectedDeckId = MutableStateFlow<Int?>(null)
    val selectedDeckId: StateFlow<Int?> = _selectedDeckId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFlashcards: StateFlow<List<Flashcard>> = _selectedDeckId
        .flatMapLatest { deckId ->
            if (deckId != null) {
                repository.getFlashcardsForDeck(deckId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- POMODORO TIMER STATE ---
    enum class TimerMode { WORK, BREAK }

    private val _timerTimeRemaining = MutableStateFlow(25 * 60) // in seconds
    val timerTimeRemaining: StateFlow<Int> = _timerTimeRemaining.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.WORK)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _selectedSubjectIdForTimer = MutableStateFlow<Int?>(null)
    val selectedSubjectIdForTimer: StateFlow<Int?> = _selectedSubjectIdForTimer.asStateFlow()

    private var timerJob: Job? = null
    private var initialWorkDurationMinutes = 25

    // --- AI ASSISTANT STATE ---
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiChatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // User to Robot pairs
    val aiChatHistory: StateFlow<List<Pair<String, String>>> = _aiChatHistory.asStateFlow()

    // --- SUBJECT CRUD ---
    fun addSubject(name: String, teacher: String, color: Int, room: String, iconName: String = "School") {
        viewModelScope.launch {
            repository.insertSubject(Subject(name = name, teacher = teacher, color = color, room = room, iconName = iconName))
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    // --- SCHEDULE CRUD ---
    fun addScheduleEvent(subjectId: Int, dayOfWeek: Int, startTime: String, endTime: String, type: String) {
        viewModelScope.launch {
            repository.insertScheduleEvent(ScheduleEvent(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, type = type))
        }
    }

    fun deleteScheduleEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            repository.deleteScheduleEvent(event)
        }
    }

    // --- ASSIGNMENT CRUD ---
    fun addAssignment(subjectId: Int, title: String, description: String, dueDate: Long, priority: String) {
        viewModelScope.launch {
            repository.insertAssignment(Assignment(subjectId = subjectId, title = title, description = description, dueDate = dueDate, priority = priority))
        }
    }

    fun toggleAssignmentCompleted(assignment: Assignment) {
        viewModelScope.launch {
            repository.updateAssignment(assignment.copy(isCompleted = !assignment.isCompleted))
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.deleteAssignment(assignment)
        }
    }

    // --- STUDY GOALS CRUD ---
    fun addGoal(
        title: String,
        targetHours: Float,
        deadline: Long,
        type: String = "HOURS",
        targetScore: Float = 0f,
        targetSubjectName: String = "",
        courseCompleted: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertGoal(
                StudyGoal(
                    title = title,
                    targetHours = targetHours,
                    currentHours = 0f,
                    deadline = deadline,
                    isAchieved = false,
                    type = type,
                    targetScore = targetScore,
                    currentScore = 0f,
                    targetSubjectName = targetSubjectName,
                    courseCompleted = courseCompleted
                )
            )
        }
    }

    fun updateGoalHours(goal: StudyGoal, hoursToAdd: Float) {
        viewModelScope.launch {
            val newHours = goal.currentHours + hoursToAdd
            val isAchieved = newHours >= goal.targetHours
            repository.updateGoal(goal.copy(currentHours = newHours, isAchieved = isAchieved))
        }
    }

    fun toggleGoalCourseCompleted(goal: StudyGoal) {
        viewModelScope.launch {
            val completed = !goal.courseCompleted
            repository.updateGoal(goal.copy(courseCompleted = completed, isAchieved = completed))
        }
    }

    fun deleteGoal(goal: StudyGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // --- STUDY SESSIONS ---
    fun addStudySession(subjectId: Int, durationMinutes: Int, notes: String = "") {
        viewModelScope.launch {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = formatter.format(Date())
            repository.insertStudySession(
                StudySession(
                    subjectId = subjectId,
                    startTime = System.currentTimeMillis(),
                    durationMinutes = durationMinutes,
                    dateString = dateStr,
                    notes = notes
                )
            )

            // Auto-update goals progressing towards the study target!
            val hours = durationMinutes / 60f
            val activeGoals = goals.value.filter { it.type == "HOURS" && !it.isAchieved && it.deadline > System.currentTimeMillis() }
            activeGoals.forEach { goal ->
                updateGoalHours(goal, hours)
            }
        }
    }

    fun deleteStudySession(session: StudySession) {
        viewModelScope.launch {
            repository.deleteStudySession(session)
        }
    }

    // --- NOTES ---
    fun addNote(subjectId: Int, title: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(Note(subjectId = subjectId, title = title, content = content))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // --- STUDY DIARIES CRUD ---
    fun addStudyDiary(activities: String, knowledgeAcquired: String, difficulties: String, mood: String = "Bình thường", dateMs: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertDiary(
                StudyDiary(
                    activities = activities,
                    knowledgeAcquired = knowledgeAcquired,
                    difficulties = difficulties,
                    mood = mood,
                    date = dateMs
                )
            )
        }
    }

    fun deleteStudyDiary(diary: StudyDiary) {
        viewModelScope.launch {
            repository.deleteDiary(diary)
        }
    }

    // --- STUDY REMINDERS CRUD ---
    fun addReminder(title: String, type: String, targetTime: Long, leadMinutes: Int, description: String = "") {
        viewModelScope.launch {
            repository.insertReminder(
                StudyReminder(
                    title = title,
                    type = type,
                    targetTime = targetTime,
                    leadMinutes = leadMinutes,
                    isTriggered = false,
                    description = description
                )
            )
        }
    }

    fun toggleReminderTriggered(reminder: StudyReminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isTriggered = !reminder.isTriggered))
        }
    }

    fun deleteReminder(reminder: StudyReminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // --- FLASHCARD DECKS ---
    fun addDeck(title: String, description: String, subjectId: Int) {
        viewModelScope.launch {
            repository.insertDeck(FlashcardDeck(title = title, description = description, subjectId = subjectId))
        }
    }

    fun deleteDeck(deck: FlashcardDeck) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
            if (_selectedDeckId.value == deck.id) {
                _selectedDeckId.value = null
            }
        }
    }

    fun selectDeck(deckId: Int?) {
        _selectedDeckId.value = deckId
    }

    // --- FLASHCARDS CRUD ---
    fun addFlashcard(deckId: Int, front: String, back: String) {
        viewModelScope.launch {
            repository.insertFlashcard(Flashcard(deckId = deckId, front = front, back = back))
        }
    }

    fun toggleFlashcardMemorized(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.updateFlashcard(flashcard.copy(memorized = !flashcard.memorized))
        }
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.deleteFlashcard(flashcard)
        }
    }

    // --- GRADES ---
    fun addGrade(subjectId: Int, name: String, score: Float, weight: Float) {
        viewModelScope.launch {
            repository.insertGrade(Grade(subjectId = subjectId, name = name, score = score, weight = weight))

            // Auto-update SCORE goals for this subject!
            val subName = subjects.value.find { it.id == subjectId }?.name
            if (subName != null) {
                val matchingGoals = goals.value.filter { it.type == "SCORE" && it.targetSubjectName.equals(subName, ignoreCase = true) }
                if (matchingGoals.isNotEmpty()) {
                    val existingGrades = grades.value.filter { it.subjectId == subjectId }
                    val allGradesForSub = existingGrades + Grade(subjectId = subjectId, name = name, score = score, weight = weight)

                    var weightedSum = 0f
                    var weightSum = 0f
                    allGradesForSub.forEach { g ->
                        weightedSum += g.score * g.weight
                        weightSum += g.weight
                    }
                    val avgScore = if (weightSum > 0f) weightedSum / weightSum else score

                    matchingGoals.forEach { goal ->
                        val achieved = avgScore >= goal.targetScore
                        repository.updateGoal(goal.copy(currentScore = avgScore, isAchieved = achieved))
                    }
                }
            }
        }
    }

    fun deleteGrade(grade: Grade) {
        viewModelScope.launch {
            repository.deleteGrade(grade)
        }
    }

    // --- COMPUTE GPA CALCULATIONS ---
    fun getGPAStats(): GPAStats {
        val gradeList = grades.value
        if (gradeList.isEmpty()) return GPAStats(0f, 0f, "Chưa có điểm")

        // Subject-wise grouping
        val groupedBySubject = gradeList.groupBy { it.subjectId }
        var totalWeightedScoreSum = 0f
        var totalWeightSum = 0f

        groupedBySubject.forEach { (_, subjectGrades) ->
            var subWeightedScore = 0f
            var subWeightTotal = 0f
            subjectGrades.forEach { grade ->
                subWeightedScore += (grade.score * grade.weight)
                subWeightTotal += grade.weight
            }

            if (subWeightTotal > 0) {
                val finalSubjectGrade = subWeightedScore / subWeightTotal
                totalWeightedScoreSum += finalSubjectGrade
                totalWeightSum += 1.0f // Treat each subject equally in GPA overall
            }
        }

        if (totalWeightSum == 0f) return GPAStats(0f, 0f, "Chưa có điểm")

        val gpa10 = totalWeightedScoreSum / totalWeightSum
        val gpa4 = (gpa10 / 10f) * 4f

        val rank = when {
            gpa10 >= 9.0f -> "Xuất sắc"
            gpa10 >= 8.0f -> "Giỏi"
            gpa10 >= 6.5f -> "Khá"
            gpa10 >= 5.0f -> "Trung bình"
            else -> "Yếu"
        }

        return GPAStats(gpa10, gpa4, rank)
    }

    data class GPAStats(
        val gpa10: Float,
        val gpa4: Float,
        val rank: String
    )

    // --- POMODORO TIMER CORE CONTROLLER ---
    fun configureTimer(minutes: Int) {
        stopTimer()
        initialWorkDurationMinutes = minutes
        _timerTimeRemaining.value = minutes * 60
        _timerMode.value = TimerMode.WORK
    }

    fun startTimer() {
        if (_timerIsRunning.value) return
        _timerIsRunning.value = true

        timerJob = viewModelScope.launch {
            while (isActive && _timerTimeRemaining.value > 0) {
                delay(1000)
                _timerTimeRemaining.value -= 1
            }

            onTimerFinished()
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerIsRunning.value = false
    }

    fun resetTimer() {
        stopTimer()
        _timerTimeRemaining.value = initialWorkDurationMinutes * 60
        _timerMode.value = TimerMode.WORK
    }

    private fun onTimerFinished() {
        _timerIsRunning.value = false
        val completedMode = _timerMode.value
        
        if (completedMode == TimerMode.WORK) {
            // Auto add a study session representing the timed focus!
            val focusSubjectId = _selectedSubjectIdForTimer.value ?: 0
            addStudySession(
                subjectId = focusSubjectId,
                durationMinutes = initialWorkDurationMinutes,
                notes = "Hoàn thành phiên làm việc Pomodoro tập trung."
            )

            // Switch to break mode
            _timerMode.value = TimerMode.BREAK
            _timerTimeRemaining.value = 5 * 60 // 5 minutes break by default
            startTimer() // Auto start break timer
        } else {
            // Break finished, reset back to work
            _timerMode.value = TimerMode.WORK
            _timerTimeRemaining.value = initialWorkDurationMinutes * 60
        }
    }

    fun selectSubjectForTimer(subjectId: Int?) {
        _selectedSubjectIdForTimer.value = subjectId
    }

    // --- GEMINI AI ASSISTANT OPERATIONS ---
    fun askAi(question: String) {
        if (question.isBlank()) return

        val helperSystemPrompt = """
            Bạn là trợ lý học thuật thông minh "EduSmart AI Advisor". 
            Bạn hãy giúp học viên giải đáp các thắc mắc học tập, tóm tắt các khái niệm phức tạp thành ngôn ngữ dễ hiểu, 
            và đưa ra lời khuyên về phương pháp học tập hiệu quả. 
            Phản hồi bằng tiếng Việt và sử dụng các dấu gạch đầu dòng trực quan để nội dung dễ đọc.
        """.trimIndent()

        _aiChatHistory.value = _aiChatHistory.value + Pair("Bạn: $question", "")
        _aiLoading.value = true

        viewModelScope.launch {
            val response = repository.askGemini(question, helperSystemPrompt)
            _aiLoading.value = false
            _aiResponse.value = response

            // Update chat history last item
            val history = _aiChatHistory.value.toMutableList()
            if (history.isNotEmpty()) {
                val lastItem = history.last()
                history[history.size - 1] = Pair(lastItem.first, response)
                _aiChatHistory.value = history
            }
        }
    }

    fun generateAiFlashcardsForTopic(topic: String, targetDeckId: Int) {
        if (topic.isBlank()) return
        _aiLoading.value = true

        viewModelScope.launch {
            val cards = repository.generateFlashcards(topic)
            if (cards.isNotEmpty()) {
                cards.forEach { (front, back) ->
                    repository.insertFlashcard(Flashcard(deckId = targetDeckId, front = front, back = back))
                }
                _aiResponse.value = "Đã tự động tạo và lưu ${cards.size} Flashcard mới về chủ đề: \"$topic\" thành công!"
            } else {
                _aiResponse.value = "Không thể tạo được Flashcard. Vui lòng kiểm tra lại cấu hình GEMINI_API_KEY."
            }
            _aiLoading.value = false
        }
    }

    fun clearChatHistory() {
        _aiChatHistory.value = emptyList()
        _aiResponse.value = ""
    }
}
