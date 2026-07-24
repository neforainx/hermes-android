package com.example

import android.app.Application
import com.example.database.AppDatabase
import com.example.database.DatabaseProvider

class HermesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Room database
        DatabaseProvider.getDatabase(this)
    }
}