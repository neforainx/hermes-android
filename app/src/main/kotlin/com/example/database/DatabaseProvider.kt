package com.example.database

import android.content.Context
import androidx.room.Room

// --- Database Provider Singleton ---

object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hermes_db"
            )
            .fallbackToDestructiveMigration()
            .build()
            instance = db
            db
        }
    }
}