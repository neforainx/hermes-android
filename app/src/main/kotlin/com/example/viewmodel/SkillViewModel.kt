package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mutableStateListOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

// --- ViewModel 3: SkillViewModel ---
class SkillViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val skillDao = db.skillDao()

    val skills: StateFlow<List<SkillEntity>> = skillDao.getAllSkills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            skillDao.getAllSkills().first().let { currentSkills ->
                val currentMap = currentSkills.associateBy { it.id }
                val defaultSkills = listOf(
                    SkillEntity("skill-vision", "Vision Multi-Modal Analyzer", "Vision", "Analyze raw canvas frames, UI designs, and local images with multimodal models.", currentMap["skill-vision"]?.isInstalled ?: true),
                    SkillEntity("skill-image-gen", "Creative Image Generator", "Creative", "Generate high-fidelity assets, UI designs, and illustrations using DALL-E 3 and Midjourney.", currentMap["skill-image-gen"]?.isInstalled ?: true),
                    SkillEntity("skill-scraper", "Puppeteer Web Scraper", "Scraper", "Crawl dynamic websites, extract clean markdown content, and bypass script blockers.", currentMap["skill-scraper"]?.isInstalled ?: true),
                    SkillEntity("skill-video-gen", "Sora Video Synthesizer", "Creative", "Generate custom video clips, animated UI transitions, and promotional reels.", currentMap["skill-video-gen"]?.isInstalled ?: true),
                    SkillEntity("skill-spotify", "Spotify Music Controller", "Music", "Control your playlists, tracks, and players dynamically.", currentMap["skill-spotify"]?.isInstalled ?: false),
                    SkillEntity("skill-hass", "Home Assistant (IoT)", "IoT", "Inspect and control smart light switches, climate controllers, and security systems.", currentMap["skill-hass"]?.isInstalled ?: false),
                    SkillEntity("skill-web", "Wikipedia Search Engine", "Information", "Directly access online encyclopedias to source background evidence.", currentMap["skill-web"]?.isInstalled ?: true),
                    SkillEntity("skill-kanban", "Kanban Board Dispatcher", "Productivity", "Organize tasks visually in columns managed fully by the agent.", currentMap["skill-kanban"]?.isInstalled ?: false),
                    SkillEntity("skill-spanner", "Cloud Spanner Database", "Database", "Directly query and store long-term structured records.", currentMap["skill-spanner"]?.isInstalled ?: false),
                    SkillEntity("skill-disk-cleanup", "Disk Cleanup Utility", "System", "Free up sandbox cache and clean dangling log and build artifacts.", currentMap["skill-disk-cleanup"]?.isInstalled ?: false),
                    SkillEntity("skill-google-meet", "Google Meet & Calendar", "Office", "Schedule, list, and join online video conferences and sync calendars.", currentMap["skill-google-meet"]?.isInstalled ?: false),
                    SkillEntity("skill-gateways", "Multi-Platform Gateways", "Network", "Connect custom chat platforms (Telegram, Discord, Slack) to Hermes Daemon.", currentMap["skill-gateways"]?.isInstalled ?: false),
                    SkillEntity("skill-strike-freedom", "Strike Freedom Cockpit", "Robotics", "Advanced robot/drone control cockpit interface and telemetry analysis.", currentMap["skill-strike-freedom"]?.isInstalled ?: false),
                    SkillEntity("skill-achievements", "Hermes Achievements System", "Progression", "Gamified achievements progression tracker for coding and automation tasks.", currentMap["skill-achievements"]?.isInstalled ?: false),
                    SkillEntity("skill-observability", "OpenTelemetry Tracer", "Diagnostics", "Visualizer for system-wide span traces and agent decision DAGs.", currentMap["skill-observability"]?.isInstalled ?: false)
                )
                skillDao.insertSkills(defaultSkills)
            }
        }
    }

    fun toggleSkill(skill: SkillEntity) {
        viewModelScope.launch {
            skillDao.setSkillInstalled(skill.id, !skill.isInstalled)
        }
    }
}