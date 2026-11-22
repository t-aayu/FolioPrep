package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudyDeckEntity::class,
        McqQuestionEntity::class,
        SubjectiveCardEntity::class,
        QuizAttemptEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FolioPrepDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var instance: FolioPrepDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE mcq_questions ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Medium'"
                )
            }
        }

        fun getInstance(context: Context): FolioPrepDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FolioPrepDatabase::class.java,
                    "folioprep_upsc_offline.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
