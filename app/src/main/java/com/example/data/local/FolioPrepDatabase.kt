package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudyDeckEntity::class,
        McqQuestionEntity::class,
        SubjectiveCardEntity::class,
        QuizAttemptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FolioPrepDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var instance: FolioPrepDatabase? = null

        fun getInstance(context: Context): FolioPrepDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FolioPrepDatabase::class.java,
                    "folioprep_upsc_offline.db"
                ).build().also { instance = it }
            }
        }
    }
}
