package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user", "hermes", "tool"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallName: String? = null,
    val toolCallStatus: String? = null // "running", "success", "failed"
)

@Entity(tableName = "cron_jobs")
data class CronJobEntity(
    @PrimaryKey val id: String,
    val name: String,
    val expression: String,
    val prompt: String,
    val isActive: Boolean,
    val lastRun: Long = 0L
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val isInstalled: Boolean
)

@Entity(tableName = "pet_state")
data class PetStateEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Hermie",
    val hatchState: String = "egg", // "egg", "hatching", "baby", "adult"
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 100, // 0 - 100
    val happiness: Int = 100, // 0 - 100
    val spriteState: String = "idle" // "idle", "walking", "sleeping"
)

// --- DAOs ---

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface CronJobDao {
    @Query("SELECT * FROM cron_jobs ORDER BY name ASC")
    fun getAllJobs(): Flow<List<CronJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: CronJobEntity)

    @Update
    suspend fun updateJob(job: CronJobEntity)

    @Query("DELETE FROM cron_jobs WHERE id = :id")
    suspend fun deleteJobById(id: String)
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<SkillEntity>)

    @Query("UPDATE skills SET isInstalled = :isInstalled WHERE id = :id")
    suspend fun setSkillInstalled(id: String, isInstalled: Boolean)
}

@Dao
interface PetStateDao {
    @Query("SELECT * FROM pet_state WHERE id = 1")
    fun getPetStateFlow(): Flow<PetStateEntity?>

    @Query("SELECT * FROM pet_state WHERE id = 1")
    suspend fun getPetStateDirect(): PetStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePetState(state: PetStateEntity)
}

// --- Database ---

@Database(
    entities = [MessageEntity::class, CronJobEntity::class, SkillEntity::class, PetStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun cronJobDao(): CronJobDao
    abstract fun skillDao(): SkillDao
    abstract fun petStateDao(): PetStateDao
}
