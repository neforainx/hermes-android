package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

// --- ViewModel 4: PetViewModel ---
class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val petStateDao = db.petStateDao()

    val petState: StateFlow<PetStateEntity?> = petStateDao.getPetStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val busyState = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val state = petStateDao.getPetStateDirect()
            if (state == null) {
                petStateDao.insertOrUpdatePetState(PetStateEntity())
            }

            // Passive pet need reduction and level logic over time
            launch {
                while (true) {
                    delay(20000) // Every 20 seconds
                    val current = petStateDao.getPetStateDirect() ?: continue
                    if (current.hatchState != "egg") {
                        val newHunger = (current.hunger - 4).coerceAtLeast(0)
                        val newHappiness = (current.happiness - 3).coerceAtLeast(0)
                        petStateDao.insertOrUpdatePetState(
                            current.copy(
                                hunger = newHunger,
                                happiness = newHappiness
                            )
                        )
                    }
                }
            }

            // Roaming animation loop: switch states periodically
            launch {
                while (true) {
                    delay(5000)
                    val current = petStateDao.getPetStateDirect() ?: continue
                    if (current.hatchState != "egg" && current.hatchState != "hatching") {
                        val nextSprite = if (current.spriteState == "idle" && Math.random() > 0.4) {
                            "walking"
                        } else if (current.spriteState == "walking" && Math.random() > 0.6) {
                            "sleeping"
                        } else {
                            "idle"
                        }
                        petStateDao.insertOrUpdatePetState(current.copy(spriteState = nextSprite))
                    }
                }
            }
        }
    }

    fun feed() {
        viewModelScope.launch {
            val current = petStateDao.getPetStateDirect() ?: return@launch
            if (current.hatchState == "egg") return@launch

            busyState.value = "Feeding..."
            delay(1200)

            val addedXp = current.xp + 10
            val levelUp = addedXp >= 100
            val newLevel = if (levelUp) current.level + 1 else current.level
            val finalXp = if (levelUp) addedXp - 100 else addedXp

            petStateDao.insertOrUpdatePetState(
                current.copy(
                    hunger = (current.hunger + 25).coerceAtMost(100),
                    xp = finalXp,
                    level = newLevel,
                    spriteState = "idle"
                )
            )
            busyState.value = null
        }
    }

    fun play() {
        viewModelScope.launch {
            val current = petStateDao.getPetStateDirect() ?: return@launch
            if (current.hatchState == "egg") return@launch

            busyState.value = "Playing..."
            delay(1200)

            val addedXp = current.xp + 10
            val levelUp = addedXp >= 100
            val newLevel = if (levelUp) current.level + 1 else current.level
            val finalXp = if (levelUp) addedXp - 100 else addedXp

            petStateDao.insertOrUpdatePetState(
                current.copy(
                    happiness = (current.happiness + 20).coerceAtMost(100),
                    xp = finalXp,
                    level = newLevel,
                    spriteState = "idle"
                )
            )
            busyState.value = null
        }
    }

    fun hatch() {
        viewModelScope.launch {
            val current = petStateDao.getPetStateDirect() ?: return@launch
            if (current.hatchState != "egg") return@launch

            busyState.value = "Hatching..."
            petStateDao.insertOrUpdatePetState(current.copy(hatchState = "hatching"))
            delay(3000)

            petStateDao.insertOrUpdatePetState(
                current.copy(
                    hatchState = "baby",
                    name = "Hermie (Baby)",
                    hunger = 80,
                    happiness = 80,
                    spriteState = "idle"
                )
            )
            busyState.value = null
        }
    }

    fun resetPet() {
        viewModelScope.launch {
            petStateDao.insertOrUpdatePetState(PetStateEntity())
        }
    }
}