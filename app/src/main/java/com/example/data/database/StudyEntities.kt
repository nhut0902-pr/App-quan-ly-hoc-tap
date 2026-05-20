package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val teacher: String = "",
    val color: Int = 0xFF4CAF50.toInt(), // Hex color int representation
    val iconName: String = "School",
    val room: String = ""
)

@Entity(tableName = "schedule_events")
data class ScheduleEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val dayOfWeek: Int, // 1 = Thứ Hai, 2 = Thứ Ba, ..., 7 = Chủ Nhật
    val startTime: String, // e.g. "08:00"
    val endTime: String, // e.g. "10:00"
    val type: String = "Lý thuyết" // "Lý thuyết", "Thực hành", "Tự học", etc.
)

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val title: String,
    val description: String = "",
    val dueDate: Long, // timestamp
    val priority: String = "Trung bình", // "Cao", "Trung bình", "Thấp"
    val isCompleted: Boolean = false
)

@Entity(tableName = "study_goals")
data class StudyGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetHours: Float,
    val currentHours: Float = 0f,
    val deadline: Long, // timestamp
    val isAchieved: Boolean = false,
    // New fields for multi-type study goals:
    val type: String = "HOURS", // "HOURS" (Số giờ học), "SCORE" (Điểm số), "COURSE" (Hoàn thành khóa học)
    val targetScore: Float = 0f,
    val currentScore: Float = 0f,
    val targetSubjectName: String = "", // Tên môn học / Khóa học hợp lệ
    val courseCompleted: Boolean = false
)

@Entity(tableName = "study_diaries")
data class StudyDiary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(), // UTC timestamp
    val activities: String, // Các hoạt động học tập hàng ngày
    val knowledgeAcquired: String, // Những kiến thức mới thu được
    val difficulties: String, // Những khó khăn gặp phải
    val mood: String = "Bình thường" // Trạng thái: ví dụ "Vui vẻ", "Tập trung", "Mệt mỏi", "Bình thường"
)

@Entity(tableName = "study_reminders")
data class StudyReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "LỊCH HỌC" or "DEADLINE BÀI TẬP" or "KHÁC"
    val targetTime: Long, // timestamp khi sự kiện diễn ra
    val leadMinutes: Int, // Nhắc trước bao nhiêu phút (Ví dụ: 15, 30, 60...)
    val isTriggered: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val startTime: Long,
    val durationMinutes: Int, // Minute duration
    val dateString: String, // "YYYY-MM-DD" for aggregation
    val notes: String = ""
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcard_decks")
data class FlashcardDeck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val subjectId: Int = 0
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: Int,
    val front: String,
    val back: String,
    val memorized: Boolean = false
)

@Entity(tableName = "grades")
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val name: String, // "Giữa kỳ", "Báo cáo", "Cuối kỳ", "Điểm quá trình"...
    val score: Float, // Score value e.g. 8.5
    val weight: Float = 1.0f // Weight, e.g. 0.3 for 30%
)
