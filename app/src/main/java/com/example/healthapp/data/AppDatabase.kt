package com.example.healthapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WeightEntry::class, CalorieEntry::class, ExerciseEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
