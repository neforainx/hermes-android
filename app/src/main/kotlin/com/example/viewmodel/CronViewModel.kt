package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// --- ViewModel 2: CronViewModel ---
class CronViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val cronJobDao = db.cronJobDao()

    val jobs: StateFlow<List<CronJobEntity>> = cronJobDao.getAllJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // A background log of triggered cron jobs
    val cronTriggerLogs = mutableStateListOf<String>()

    init {
        // Pre-populate some cool default cron jobs
        viewModelScope.launch {
            cronJobDao.getAllJobs().first().let { currentJobs ->
                if (currentJobs.isEmpty()) {
                    cronJobDao.insertJob(
                        CronJobEntity(
                            id = "cron-1",
                            name = "Website Health Check",
                            expression = "*/15 * * * *",
                            prompt = "Check website status and alert if down",
                            isActive = true
                        )
                    )
                    cronJobDao.insertJob(
                        CronJobEntity(
                            id = "cron-2",
                            name = "Daily News Report",
                            expression = "0 8 * * *",
                            prompt = "Summarize tech news from HackerNews",
                            isActive = false
                        )
                    )
                    cronJobDao.insertJob(
                        CronJobEntity(
                            id = "cron-3",
                            name = "Database Backup",
                            expression = "0 0 * * *",
                            prompt = "Back up local SQLite state securely",
                            isActive = true
                        )
                    )
                }
            }
        }

        // Execute cron trigger events periodically
        viewModelScope.launch {
            launch {
                while (true) {
                    delay(15000) // check active cron jobs every 15s
                    val activeList = jobs.value.filter { it.isActive }
                    if (activeList.isNotEmpty()) {
                        val triggered = activeList.random()
                        cronJobDao.insertJob(triggered.copy(lastRun = System.currentTimeMillis()))
                        cronTriggerLogs.add(
                            0,
                            "[${System.currentTimeMillis() % 100000}] Triggered: ${triggered.name} - Running: ${triggered.prompt}"
                        )
                        if (cronTriggerLogs.size > 20) {
                            cronTriggerLogs.removeLastOrNull()
                        }
                    }
                }
            }
        }
    }

    fun addJob(name: String, expression: String, prompt: String) {
        viewModelScope.launch {
            val job = CronJobEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                expression = expression,
                prompt = prompt,
                isActive = true
            )
            cronJobDao.insertJob(job)
        }
    }

    fun toggleJob(job: CronJobEntity) {
        viewModelScope.launch {
            cronJobDao.updateJob(job.copy(isActive = !job.isActive))
        }
    }

    fun deleteJob(id: String) {
        viewModelScope.launch {
            cronJobDao.deleteJobById(id)
        }
    }
}