package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- SUBJECTS ---
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)

    // --- SCHEDULE EVENTS ---
    @Query("SELECT * FROM schedule_events ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllScheduleEvents(): Flow<List<ScheduleEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEvent(event: ScheduleEvent): Long

    @Delete
    suspend fun deleteScheduleEvent(event: ScheduleEvent)

    // --- ASSIGNMENTS ---
    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)

    // --- STUDY GOALS ---
    @Query("SELECT * FROM study_goals ORDER BY deadline ASC")
    fun getAllGoals(): Flow<List<StudyGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: StudyGoal): Long

    @Update
    suspend fun updateGoal(goal: StudyGoal)

    @Delete
    suspend fun deleteGoal(goal: StudyGoal)

    // --- STUDY SESSIONS ---
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllStudySessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession): Long

    @Delete
    suspend fun deleteStudySession(session: StudySession)

    // --- NOTES ---
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)

    // --- FLASHCARD DECKS ---
    @Query("SELECT * FROM flashcard_decks ORDER BY title ASC")
    fun getAllDecks(): Flow<List<FlashcardDeck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeck): Long

    @Delete
    suspend fun deleteDeck(deck: FlashcardDeck)

    // --- FLASHCARDS ---
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    fun getFlashcardsForDeck(deckId: Int): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    // --- GRADES ---
    @Query("SELECT * FROM grades")
    fun getAllGrades(): Flow<List<Grade>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: Grade): Long

    @Delete
    suspend fun deleteGrade(grade: Grade)

    // --- STUDY DIARIES ---
    @Query("SELECT * FROM study_diaries ORDER BY date DESC")
    fun getAllDiaries(): Flow<List<StudyDiary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: StudyDiary): Long

    @Delete
    suspend fun deleteDiary(diary: StudyDiary)

    // --- REMINDERS ---
    @Query("SELECT * FROM study_reminders ORDER BY targetTime ASC")
    fun getAllReminders(): Flow<List<StudyReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: StudyReminder): Long

    @Update
    suspend fun updateReminder(reminder: StudyReminder)

    @Delete
    suspend fun deleteReminder(reminder: StudyReminder)
}
