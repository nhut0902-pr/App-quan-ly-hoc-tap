package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.RetrofitClient
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StudyRepository(private val studyDao: StudyDao) {

    // --- SUBJECTS ---
    val allSubjects: Flow<List<Subject>> = studyDao.getAllSubjects()
    suspend fun insertSubject(subject: Subject): Long = studyDao.insertSubject(subject)
    suspend fun deleteSubject(subject: Subject) = studyDao.deleteSubject(subject)
    suspend fun getSubjectById(id: Int): Subject? = studyDao.getSubjectById(id)

    // --- SCHEDULE EVENTS ---
    val allScheduleEvents: Flow<List<ScheduleEvent>> = studyDao.getAllScheduleEvents()
    suspend fun insertScheduleEvent(event: ScheduleEvent): Long = studyDao.insertScheduleEvent(event)
    suspend fun deleteScheduleEvent(event: ScheduleEvent) = studyDao.deleteScheduleEvent(event)

    // --- ASSIGNMENTS ---
    val allAssignments: Flow<List<Assignment>> = studyDao.getAllAssignments()
    suspend fun insertAssignment(assignment: Assignment): Long = studyDao.insertAssignment(assignment)
    suspend fun updateAssignment(assignment: Assignment) = studyDao.updateAssignment(assignment)
    suspend fun deleteAssignment(assignment: Assignment) = studyDao.deleteAssignment(assignment)

    // --- STUDY GOALS ---
    val allGoals: Flow<List<StudyGoal>> = studyDao.getAllGoals()
    suspend fun insertGoal(goal: StudyGoal): Long = studyDao.insertGoal(goal)
    suspend fun updateGoal(goal: StudyGoal) = studyDao.updateGoal(goal)
    suspend fun deleteGoal(goal: StudyGoal) = studyDao.deleteGoal(goal)

    // --- STUDY SESSIONS ---
    val allStudySessions: Flow<List<StudySession>> = studyDao.getAllStudySessions()
    suspend fun insertStudySession(session: StudySession): Long = studyDao.insertStudySession(session)
    suspend fun deleteStudySession(session: StudySession) = studyDao.deleteStudySession(session)

    // --- NOTES ---
    val allNotes: Flow<List<Note>> = studyDao.getAllNotes()
    suspend fun insertNote(note: Note): Long = studyDao.insertNote(note)
    suspend fun deleteNote(note: Note) = studyDao.deleteNote(note)

    // --- FLASHCARD DECKS ---
    val allDecks: Flow<List<FlashcardDeck>> = studyDao.getAllDecks()
    suspend fun insertDeck(deck: FlashcardDeck): Long = studyDao.insertDeck(deck)
    suspend fun deleteDeck(deck: FlashcardDeck) = studyDao.deleteDeck(deck)

    // --- FLASHCARDS ---
    fun getFlashcardsForDeck(deckId: Int): Flow<List<Flashcard>> = studyDao.getFlashcardsForDeck(deckId)
    suspend fun insertFlashcard(flashcard: Flashcard): Long = studyDao.insertFlashcard(flashcard)
    suspend fun updateFlashcard(flashcard: Flashcard) = studyDao.updateFlashcard(flashcard)
    suspend fun deleteFlashcard(flashcard: Flashcard) = studyDao.deleteFlashcard(flashcard)

    // --- GRADES ---
    val allGrades: Flow<List<Grade>> = studyDao.getAllGrades()
    suspend fun insertGrade(grade: Grade): Long = studyDao.insertGrade(grade)
    suspend fun deleteGrade(grade: Grade) = studyDao.deleteGrade(grade)

    // --- STUDY DIARIES ---
    val allDiaries: Flow<List<StudyDiary>> = studyDao.getAllDiaries()
    suspend fun insertDiary(diary: StudyDiary): Long = studyDao.insertDiary(diary)
    suspend fun deleteDiary(diary: StudyDiary) = studyDao.deleteDiary(diary)

    // --- REMINDERS ---
    val allReminders: Flow<List<StudyReminder>> = studyDao.getAllReminders()
    suspend fun insertReminder(reminder: StudyReminder): Long = studyDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: StudyReminder) = studyDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: StudyReminder) = studyDao.deleteReminder(reminder)

    // --- GEMINI AI ASSISTANT ---
    suspend fun askGemini(prompt: String, systemInstruction: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Lỗi: Vui lòng cấu hình GEMINI_API_KEY trong tệp .env hoặc Secrets panel của AI Studio trước khi sử dụng trợ lý AI."
        }

        val fullPrompt = if (systemInstruction.isNotEmpty()) {
            "$systemInstruction\n\nYêu cầu học viên:\n$prompt"
        } else {
            prompt
        }

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = fullPrompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Không nhận được phản hồi từ trợ lý AI học tập."
        } catch (e: Exception) {
            "Lỗi kết nối Trợ lý AI: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun generateFlashcards(topic: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext emptyList()
        }

        val prompt = """
            Tạo danh sách 5 flashcard học tập về chủ đề: "$topic".
            Mỗi flashcard gồm có 'front' (câu hỏi/khái niệm/ thuật ngữ ngắn gọn) và 'back' (câu trả lời/định nghĩa tóm tắt súc tích).
            Ngôn ngữ: Tiếng Việt.
            Trả về dưới định dạng JSON là một mảng các đối tượng chứa hai thuộc tính "front" và "back".
            Ví dụ định dạng trả về:
            [
              {"front": "Thuật ngữ 1", "back": "Định nghĩa 1"},
              {"front": "Thuật ngữ 2", "back": "Định nghĩa 2"}
            ]
            Hãy CHỈ TRẢ VỀ mã JSON hợp lệ cấu trúc mảng, không mô tả hay văn bản thừa bên ngoài.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.5f,
                responseMimeType = "application/json"
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse plain string response representation
                val list = mutableListOf<Pair<String, String>>()
                val jsonArray = JSONArray(jsonText.trim())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val front = obj.optString("front", "")
                    val back = obj.optString("back", "")
                    if (front.isNotEmpty() && back.isNotEmpty()) {
                        list.add(Pair(front, back))
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
