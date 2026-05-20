package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Subject::class,
        ScheduleEvent::class,
        Assignment::class,
        StudyGoal::class,
        StudySession::class,
        Note::class,
        FlashcardDeck::class,
        Flashcard::class,
        Grade::class,
        StudyDiary::class,
        StudyReminder::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getDatabase(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "studysmart_database"
                )
                .fallbackToDestructiveMigration() // Destructive migration for easy development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
