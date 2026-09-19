package com.example.kaoyanfocus.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_categories", indices = [Index(value = ["name"], unique = true)])
data class StudyCategory(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val active: Boolean = true, val lastUsedAt: Long = 0)

@Entity(tableName = "study_sessions", indices = [Index("startDate"), Index("categoryId")])
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val minutes: Int,
    val points: Int,
    @ColumnInfo(defaultValue = "NULL") val categoryId: Long? = null,
    @ColumnInfo(defaultValue = "'学习'") val categoryName: String = "学习",
    @ColumnInfo(defaultValue = "''") val note: String = "",
    @ColumnInfo(defaultValue = "0") val durationSeconds: Long = minutes * 60L,
    @ColumnInfo(defaultValue = "0") val pauseCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val nightSeconds: Long = 0,
    @ColumnInfo(defaultValue = "0") val earlySeconds: Long = 0,
    @ColumnInfo(defaultValue = "''") val startDate: String = "",
    @ColumnInfo(defaultValue = "''") val themeKeyAtStart: String = "",
    @ColumnInfo(defaultValue = "0") val lateNightSeconds: Long = 0
)

@Entity(tableName = "point_ledger", indices = [Index(value = ["businessKey"], unique = true), Index("sourceDate")])
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val amount: Int,
    val type: String,
    val note: String,
    @ColumnInfo(defaultValue = "NULL") val businessKey: String? = null,
    @ColumnInfo(defaultValue = "''") val sourceDate: String = ""
)

@Entity(tableName = "point_rules", indices = [Index(value = ["effectiveDate"], unique = true)])
data class PointRuleVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveDate: String,
    val minutesPerPoint: Int = 60,
    val pointsPerBlock: Int = 1,
    val bonusMinutes: Int = 480,
    val bonusPoints: Int = 5,
    val capMinutes: Int = 720,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val cost: Int,
    val active: Boolean = true,
    @ColumnInfo(defaultValue = "'CONSUMABLE'") val productType: String = "CONSUMABLE",
    @ColumnInfo(defaultValue = "''") val entitlement: String = description,
    @ColumnInfo(defaultValue = "NULL") val themeKey: String? = null
)

@Entity(tableName = "redemptions", indices = [Index("productId")])
data class Redemption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val cost: Int,
    val redeemedAt: Long,
    @ColumnInfo(defaultValue = "''") val entitlement: String = "",
    @ColumnInfo(defaultValue = "'CONSUMABLE'") val productType: String = "CONSUMABLE",
    @ColumnInfo(defaultValue = "NULL") val usedAt: Long? = null
)

@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val points: Int,
    val recurrence: String = "DAILY",
    val weekdays: String = "1,2,3,4,5,6,7",
    val oneOffDate: String? = null,
    val active: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_tasks", indices = [Index(value = ["templateId", "taskDate"], unique = true), Index("taskDate")])
data class DailyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val taskDate: String,
    val title: String,
    val description: String,
    val points: Int,
    val completedAt: Long? = null
)

@Entity(tableName = "reward_codes", indices = [Index(value = ["codeHash"], unique = true)])
data class RewardCode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codeHash: String,
    val displayCode: String,
    val points: Int,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null,
    @ColumnInfo(defaultValue = "'POINTS'") val rewardType: String = "POINTS",
    @ColumnInfo(defaultValue = "''") val rewardPayload: String = ""
)

@Entity(tableName = "achievement_definitions")
data class AchievementDefinition(
    @PrimaryKey val key: String,
    val name: String,
    val description: String,
    val icon: String,
    val hidden: Boolean,
    val rewardPoints: Int,
    val sortOrder: Int
)

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlock(@PrimaryKey val achievementKey: String, val unlockedAt: Long, val rewardPoints: Int)

@Entity(tableName = "daily_reflections")
data class DailyReflection(
    @PrimaryKey val date: String,
    val note: String = "",
    val summary: String = "",
    val mood: Int = 3,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rest_days", indices = [Index(value = ["redemptionId"], unique = true)])
data class RestDay(
    @PrimaryKey val date: String,
    @ColumnInfo(defaultValue = "NULL") val redemptionId: Long? = null,
    val usedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'LEGACY_CARD'") val sourceType: String = "LEGACY_CARD",
    @ColumnInfo(defaultValue = "''") val weekStart: String = ""
)

@Entity(tableName = "pet_profiles")
data class PetProfile(
    @PrimaryKey val id: Int = 1,
    val unlockedAt: Long,
    val name: String = "",
    val namedAt: Long? = null,
    val equippedExpression: String = "",
    val equippedOutfit: String = "",
    val equippedAction: String = "",
    val placedFurnitureCsv: String = "",
    @ColumnInfo(defaultValue = "''") val equippedEyeAccessory: String = "",
    @ColumnInfo(defaultValue = "''") val equippedNeckAccessory: String = "",
    @ColumnInfo(defaultValue = "''") val equippedHeadAccessory: String = "",
    @ColumnInfo(defaultValue = "1") val highestRewardLevel: Int = 1,
    @ColumnInfo(defaultValue = "1") val showOnFocus: Boolean = true
)

@Entity(tableName = "pet_unlocks")
data class PetUnlock(
    @PrimaryKey val itemKey: String,
    val itemType: String,
    val sourceLevel: Int,
    val unlockedAt: Long,
    @ColumnInfo(defaultValue = "'LEVEL'") val sourceType: String = "LEVEL"
)

@Entity(tableName = "pet_letters", indices = [Index(value = ["templateIndex"], unique = true), Index(value = ["nightKey"], unique = true)])
data class PetLetter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateIndex: Int,
    val title: String,
    val body: String,
    val nightKey: String,
    val receivedAt: Long,
    val rewardCodeId: Long? = null,
    val displayCode: String? = null
)

@Entity(tableName = "night_study_progress")
data class NightStudyProgress(
    @PrimaryKey val nightKey: String,
    val seconds: Long = 0,
    val letterIssued: Boolean = false
)

@Entity(tableName = "food_records", indices = [Index("capturedAt"), Index("feedDate"), Index("status")])
data class FoodRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String? = null,
    val sourceType: String = "GALLERY",
    val recognizedKey: String = "other",
    val recognizedName: String = "其他食物",
    val candidatesCsv: String = "",
    val confidence: Float = 0f,
    val safetyState: String = "SAFE",
    val status: String = "READY",
    val capturedAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long = System.currentTimeMillis(),
    val fedAt: Long? = null,
    val feedDate: String = "",
    val aiProvider: String = "MANUAL",
    val aiModel: String = ""
)

@Entity(tableName = "food_recognition_attempts", indices = [Index("requestDate")])
data class FoodRecognitionAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestedAt: Long = System.currentTimeMillis(),
    val requestDate: String,
    val outcome: String,
    val model: String = ""
)

/** Local-only feedback: it records whether the chosen food matched the AI suggestion. */
@Entity(tableName = "food_recognition_feedback", indices = [Index("createdAt"), Index("suggestedKey")])
data class FoodRecognitionFeedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val suggestedKey: String,
    val confirmedKey: String,
    val wasCorrect: Boolean,
    val provider: String = "",
    val model: String = ""
)

@Entity(tableName = "audit_log")
data class AuditEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val action: String,
    val detail: String
)

@Dao
interface AppDao {
    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC") fun sessions(): Flow<List<StudySession>>
    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC") suspend fun sessionSnapshot(): List<StudySession>
    @Query("SELECT * FROM point_ledger ORDER BY createdAt DESC") fun ledger(): Flow<List<LedgerEntry>>
    @Query("SELECT * FROM point_ledger ORDER BY createdAt DESC") suspend fun ledgerSnapshot(): List<LedgerEntry>
    @Query("SELECT COALESCE(SUM(amount),0) FROM point_ledger") fun balance(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount),0) FROM point_ledger") suspend fun currentBalance(): Int
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY cost") fun activeProducts(): Flow<List<Product>>
    @Query("SELECT * FROM products ORDER BY id") fun allProducts(): Flow<List<Product>>
    @Query("SELECT * FROM products ORDER BY id") suspend fun productSnapshot(): List<Product>
    @Query("SELECT * FROM redemptions ORDER BY redeemedAt DESC") fun redemptions(): Flow<List<Redemption>>
    @Query("SELECT * FROM redemptions ORDER BY redeemedAt DESC") suspend fun redemptionSnapshot(): List<Redemption>
    @Query("SELECT * FROM study_categories WHERE active = 1 ORDER BY lastUsedAt DESC, name") fun activeCategories(): Flow<List<StudyCategory>>
    @Query("SELECT * FROM study_categories ORDER BY active DESC, lastUsedAt DESC, name") fun allCategories(): Flow<List<StudyCategory>>
    @Query("SELECT * FROM study_categories WHERE name = :name LIMIT 1") suspend fun categoryByName(name: String): StudyCategory?
    @Query("SELECT * FROM point_rules WHERE effectiveDate <= :date ORDER BY effectiveDate DESC LIMIT 1") suspend fun ruleFor(date: String): PointRuleVersion?
    @Query("SELECT * FROM point_rules ORDER BY effectiveDate DESC LIMIT 1") fun latestRule(): Flow<PointRuleVersion?>
    @Query("SELECT * FROM task_templates ORDER BY active DESC, id") fun taskTemplates(): Flow<List<TaskTemplate>>
    @Query("SELECT * FROM task_templates WHERE active = 1") suspend fun activeTaskTemplates(): List<TaskTemplate>
    @Query("SELECT * FROM daily_tasks WHERE taskDate = :date ORDER BY id") fun tasksFor(date: String): Flow<List<DailyTask>>
    @Query("SELECT * FROM daily_tasks ORDER BY taskDate DESC, id DESC") suspend fun allDailyTasks(): List<DailyTask>
    @Query("SELECT * FROM reward_codes ORDER BY createdAt DESC") fun rewardCodes(): Flow<List<RewardCode>>
    @Query("SELECT * FROM reward_codes WHERE codeHash = :hash LIMIT 1") suspend fun rewardCode(hash: String): RewardCode?
    @Query("SELECT * FROM achievement_definitions ORDER BY sortOrder") fun achievements(): Flow<List<AchievementDefinition>>
    @Query("SELECT * FROM achievement_definitions ORDER BY sortOrder") suspend fun achievementSnapshot(): List<AchievementDefinition>
    @Query("SELECT * FROM achievement_unlocks") fun unlocks(): Flow<List<AchievementUnlock>>
    @Query("SELECT * FROM achievement_unlocks") suspend fun unlockSnapshot(): List<AchievementUnlock>
    @Query("SELECT * FROM daily_reflections WHERE date = :date LIMIT 1") fun reflection(date: String): Flow<DailyReflection?>
    @Query("SELECT * FROM daily_reflections ORDER BY date DESC") fun reflections(): Flow<List<DailyReflection>>
    @Query("SELECT * FROM rest_days ORDER BY date DESC") fun restDays(): Flow<List<RestDay>>
    @Query("SELECT * FROM rest_days ORDER BY date DESC") suspend fun restDaySnapshot(): List<RestDay>
    @Query("SELECT * FROM rest_days WHERE date = :date LIMIT 1") suspend fun restDay(date: String): RestDay?
    @Query("SELECT * FROM pet_profiles WHERE id = 1 LIMIT 1") fun petProfile(): Flow<PetProfile?>
    @Query("SELECT * FROM pet_profiles WHERE id = 1 LIMIT 1") suspend fun petProfileSnapshot(): PetProfile?
    @Query("SELECT * FROM pet_unlocks ORDER BY sourceLevel, itemKey") fun petUnlocks(): Flow<List<PetUnlock>>
    @Query("SELECT * FROM pet_unlocks ORDER BY sourceLevel, itemKey") suspend fun petUnlockSnapshot(): List<PetUnlock>
    @Query("SELECT * FROM pet_letters ORDER BY receivedAt DESC") fun petLetters(): Flow<List<PetLetter>>
    @Query("SELECT * FROM pet_letters ORDER BY templateIndex") suspend fun petLetterSnapshot(): List<PetLetter>
    @Query("SELECT * FROM night_study_progress WHERE nightKey = :key LIMIT 1") suspend fun nightProgress(key: String): NightStudyProgress?
    @Query("SELECT * FROM food_records ORDER BY capturedAt DESC") fun foodRecords(): Flow<List<FoodRecord>>
    @Query("SELECT * FROM food_records ORDER BY capturedAt DESC") suspend fun foodRecordSnapshot(): List<FoodRecord>
    @Query("SELECT * FROM food_records WHERE id = :id LIMIT 1") suspend fun foodRecord(id: Long): FoodRecord?
    @Query("SELECT COUNT(*) FROM food_records WHERE status = 'FED'") fun affection(): Flow<Int>
    @Query("SELECT COUNT(*) FROM food_records WHERE status = 'FED' AND feedDate = :date") suspend fun fedCount(date: String): Int
    @Query("SELECT COUNT(*) FROM food_records WHERE status = 'FED' AND feedDate = :date AND sourceType = 'CINNAMOROLL_TREAT'") suspend fun cinnamorollTreatCount(date: String): Int
    @Query("SELECT MAX(fedAt) FROM food_records WHERE status = 'FED'") suspend fun lastFedAt(): Long?
    @Query("SELECT COUNT(*) FROM food_recognition_attempts WHERE requestDate = :date") fun recognitionCount(date: String): Flow<Int>
    @Query("SELECT COUNT(*) FROM food_recognition_attempts WHERE requestDate = :date") suspend fun recognitionCountSnapshot(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSession(item: StudySession): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLedger(item: LedgerEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLedger(item: LedgerEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProduct(item: Product): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRedemption(item: Redemption): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategory(item: StudyCategory): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRule(item: PointRuleVersion): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTaskTemplate(item: TaskTemplate): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertDailyTask(item: DailyTask): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRewardCode(item: RewardCode): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAchievement(item: AchievementDefinition)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUnlock(item: AchievementUnlock)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReflection(item: DailyReflection)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRestDay(item: RestDay): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPetProfile(item: PetProfile)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPetUnlock(item: PetUnlock): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPetLetter(item: PetLetter): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNightProgress(item: NightStudyProgress)
    @Insert suspend fun insertFoodRecord(item: FoodRecord): Long
    @Insert suspend fun insertRecognitionAttempt(item: FoodRecognitionAttempt): Long
    @Insert suspend fun insertRecognitionFeedback(item: FoodRecognitionFeedback): Long
    @Insert suspend fun insertAudit(item: AuditEntry)
    @Update suspend fun updateProduct(item: Product)
    @Update suspend fun updateCategory(item: StudyCategory)
    @Update suspend fun updateTaskTemplate(item: TaskTemplate)
    @Update suspend fun updateDailyTask(item: DailyTask)
    @Update suspend fun updateRewardCode(item: RewardCode)
    @Update suspend fun updateRedemption(item: Redemption)
    @Update suspend fun updateFoodRecord(item: FoodRecord)
    @Query("SELECT * FROM redemptions WHERE id = :id") suspend fun redemptionById(id: Long): Redemption?
    @Query("SELECT * FROM daily_tasks WHERE id = :id") suspend fun dailyTaskById(id: Long): DailyTask?
    @Query("SELECT * FROM study_sessions WHERE id = :id") suspend fun sessionById(id: Long): StudySession?
    @Query("DELETE FROM study_sessions WHERE id = :id") suspend fun deleteSession(id: Long)
    @Query("DELETE FROM achievement_unlocks WHERE achievementKey = :key") suspend fun deleteUnlock(key: String)
    @Query("DELETE FROM point_ledger WHERE businessKey = :key") suspend fun deleteLedgerByKey(key: String)
    @Query("DELETE FROM study_sessions") suspend fun clearSessions()
    @Query("DELETE FROM point_ledger") suspend fun clearLedger()
    @Query("DELETE FROM redemptions") suspend fun clearRedemptions()
    @Query("DELETE FROM daily_tasks") suspend fun clearDailyTasks()
    @Query("DELETE FROM achievement_unlocks") suspend fun clearUnlocks()
    @Query("DELETE FROM daily_reflections") suspend fun clearReflections()
    @Query("DELETE FROM rest_days") suspend fun clearRestDays()
    @Query("DELETE FROM pet_profiles") suspend fun clearPetProfiles()
    @Query("DELETE FROM pet_unlocks") suspend fun clearPetUnlocks()
    @Query("DELETE FROM pet_letters") suspend fun clearPetLetters()
    @Query("DELETE FROM night_study_progress") suspend fun clearNightProgress()
    @Query("DELETE FROM audit_log") suspend fun clearAudit()
    @Query("DELETE FROM study_categories") suspend fun clearCategories()
    @Query("DELETE FROM point_rules") suspend fun clearRules()
    @Query("DELETE FROM task_templates") suspend fun clearTaskTemplates()
    @Query("DELETE FROM products") suspend fun clearProducts()
    @Query("DELETE FROM products WHERE name = :name") suspend fun deleteProductByName(name: String)
    @Query("DELETE FROM reward_codes") suspend fun clearRewardCodes()
    @Query("DELETE FROM food_records") suspend fun clearFoodRecords()
    @Query("DELETE FROM food_recognition_attempts") suspend fun clearRecognitionAttempts()
    @Query("DELETE FROM food_records WHERE id = :id") suspend fun deleteFoodRecord(id: Long)
}

@Database(
    entities = [StudyCategory::class, StudySession::class, LedgerEntry::class, PointRuleVersion::class,
        Product::class, Redemption::class, TaskTemplate::class, DailyTask::class, RewardCode::class,
        AchievementDefinition::class, AchievementUnlock::class, DailyReflection::class, RestDay::class, AuditEntry::class,
        PetProfile::class, PetUnlock::class, PetLetter::class, NightStudyProgress::class,
        FoodRecord::class, FoodRecognitionAttempt::class, FoodRecognitionFeedback::class],
    version = 8, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        @Volatile private var instance: AppDatabase? = null
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN categoryId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN categoryName TEXT NOT NULL DEFAULT '学习'")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN pauseCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN nightSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN earlySeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN startDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE study_sessions SET durationSeconds = minutes * 60")
                db.execSQL("ALTER TABLE point_ledger ADD COLUMN businessKey TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE point_ledger ADD COLUMN sourceDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN productType TEXT NOT NULL DEFAULT 'CONSUMABLE'")
                db.execSQL("ALTER TABLE products ADD COLUMN entitlement TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN themeKey TEXT DEFAULT NULL")
                db.execSQL("UPDATE products SET entitlement = description")
                db.execSQL("ALTER TABLE redemptions ADD COLUMN entitlement TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE redemptions ADD COLUMN productType TEXT NOT NULL DEFAULT 'CONSUMABLE'")
                db.execSQL("ALTER TABLE redemptions ADD COLUMN usedAt INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sessions_startDate ON study_sessions(startDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sessions_categoryId ON study_sessions(categoryId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_point_ledger_businessKey ON point_ledger(businessKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_point_ledger_sourceDate ON point_ledger(sourceDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_redemptions_productId ON redemptions(productId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS study_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL, lastUsedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_study_categories_name ON study_categories(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS point_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, effectiveDate TEXT NOT NULL, minutesPerPoint INTEGER NOT NULL, pointsPerBlock INTEGER NOT NULL, bonusMinutes INTEGER NOT NULL, bonusPoints INTEGER NOT NULL, capMinutes INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_point_rules_effectiveDate ON point_rules(effectiveDate)")
                db.execSQL("CREATE TABLE IF NOT EXISTS task_templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, points INTEGER NOT NULL, recurrence TEXT NOT NULL, weekdays TEXT NOT NULL, oneOffDate TEXT, active INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateId INTEGER NOT NULL, taskDate TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, points INTEGER NOT NULL, completedAt INTEGER)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_tasks_templateId_taskDate ON daily_tasks(templateId, taskDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_tasks_taskDate ON daily_tasks(taskDate)")
                db.execSQL("CREATE TABLE IF NOT EXISTS reward_codes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, codeHash TEXT NOT NULL, displayCode TEXT NOT NULL, points INTEGER NOT NULL, active INTEGER NOT NULL, createdAt INTEGER NOT NULL, usedAt INTEGER)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reward_codes_codeHash ON reward_codes(codeHash)")
                db.execSQL("CREATE TABLE IF NOT EXISTS achievement_definitions (`key` TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, icon TEXT NOT NULL, hidden INTEGER NOT NULL, rewardPoints INTEGER NOT NULL, sortOrder INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS achievement_unlocks (achievementKey TEXT NOT NULL PRIMARY KEY, unlockedAt INTEGER NOT NULL, rewardPoints INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_reflections (`date` TEXT NOT NULL PRIMARY KEY, note TEXT NOT NULL, summary TEXT NOT NULL, mood INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, action TEXT NOT NULL, detail TEXT NOT NULL)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS rest_days (`date` TEXT NOT NULL PRIMARY KEY, redemptionId INTEGER NOT NULL, usedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_rest_days_redemptionId ON rest_days(redemptionId)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN themeKeyAtStart TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN lateNightSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reward_codes ADD COLUMN rewardType TEXT NOT NULL DEFAULT 'POINTS'")
                db.execSQL("ALTER TABLE reward_codes ADD COLUMN rewardPayload TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE rest_days_new (`date` TEXT NOT NULL PRIMARY KEY, redemptionId INTEGER DEFAULT NULL, usedAt INTEGER NOT NULL, sourceType TEXT NOT NULL DEFAULT 'LEGACY_CARD', weekStart TEXT NOT NULL DEFAULT '')")
                db.execSQL("INSERT INTO rest_days_new (`date`, redemptionId, usedAt, sourceType, weekStart) SELECT `date`, redemptionId, usedAt, 'LEGACY_CARD', '' FROM rest_days")
                db.execSQL("DROP TABLE rest_days")
                db.execSQL("ALTER TABLE rest_days_new RENAME TO rest_days")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_rest_days_redemptionId ON rest_days(redemptionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS pet_profiles (id INTEGER NOT NULL PRIMARY KEY, unlockedAt INTEGER NOT NULL, name TEXT NOT NULL, namedAt INTEGER, equippedExpression TEXT NOT NULL, equippedOutfit TEXT NOT NULL, equippedAction TEXT NOT NULL, placedFurnitureCsv TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS pet_unlocks (itemKey TEXT NOT NULL PRIMARY KEY, itemType TEXT NOT NULL, sourceLevel INTEGER NOT NULL, unlockedAt INTEGER NOT NULL, sourceType TEXT NOT NULL DEFAULT 'LEVEL')")
                db.execSQL("CREATE TABLE IF NOT EXISTS pet_letters (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateIndex INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, nightKey TEXT NOT NULL, receivedAt INTEGER NOT NULL, rewardCodeId INTEGER, displayCode TEXT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pet_letters_templateIndex ON pet_letters(templateIndex)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pet_letters_nightKey ON pet_letters(nightKey)")
                db.execSQL("CREATE TABLE IF NOT EXISTS night_study_progress (nightKey TEXT NOT NULL PRIMARY KEY, seconds INTEGER NOT NULL, letterIssued INTEGER NOT NULL)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pet_profiles ADD COLUMN equippedEyeAccessory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pet_profiles ADD COLUMN equippedNeckAccessory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pet_profiles ADD COLUMN equippedHeadAccessory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pet_profiles ADD COLUMN highestRewardLevel INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE pet_profiles ADD COLUMN showOnFocus INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE pet_profiles SET equippedEyeAccessory = 'outfit_glasses', equippedOutfit = '' WHERE equippedOutfit = 'outfit_glasses'")
                db.execSQL("UPDATE pet_profiles SET equippedNeckAccessory = 'outfit_target_scarf', equippedOutfit = '' WHERE equippedOutfit = 'outfit_target_scarf'")
                db.execSQL("UPDATE pet_unlocks SET itemKey = 'action_stretch' WHERE itemKey = 'action_spin'")
                db.execSQL("UPDATE pet_unlocks SET itemType = 'OUTFIT_MAIN' WHERE itemKey IN ('outfit_school','outfit_star_pajamas','outfit_library')")
                db.execSQL("UPDATE pet_unlocks SET itemType = 'ACCESSORY_EYE' WHERE itemKey = 'outfit_glasses'")
                db.execSQL("UPDATE pet_unlocks SET itemType = 'ACCESSORY_NECK' WHERE itemKey = 'outfit_target_scarf'")
                db.execSQL("UPDATE pet_unlocks SET itemKey = 'outfit_starry', itemType = 'OUTFIT_MAIN' WHERE itemKey = 'outfit_moonlight'")
                db.execSQL("UPDATE reward_codes SET rewardPayload = 'outfit_starry' WHERE rewardType = 'PET_ITEM' AND rewardPayload IN ('', 'outfit_moonlight')")
                db.execSQL("UPDATE pet_profiles SET highestRewardLevel = COALESCE((SELECT MAX(sourceLevel) FROM pet_unlocks WHERE sourceType = 'LEVEL'), 1)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE pet_unlocks SET itemKey = 'furniture_cloud_cushion', itemType = 'FURNITURE' WHERE itemKey = 'outfit_glasses'")
                db.execSQL("UPDATE pet_unlocks SET itemKey = 'furniture_goal_trophy', itemType = 'FURNITURE' WHERE itemKey = 'outfit_target_scarf'")
                db.execSQL("UPDATE pet_profiles SET equippedEyeAccessory = '', equippedNeckAccessory = ''")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS food_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, photoPath TEXT, sourceType TEXT NOT NULL, recognizedKey TEXT NOT NULL, recognizedName TEXT NOT NULL, candidatesCsv TEXT NOT NULL, confidence REAL NOT NULL, safetyState TEXT NOT NULL, status TEXT NOT NULL, capturedAt INTEGER NOT NULL, confirmedAt INTEGER NOT NULL, fedAt INTEGER, feedDate TEXT NOT NULL, aiProvider TEXT NOT NULL, aiModel TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_records_capturedAt ON food_records(capturedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_records_feedDate ON food_records(feedDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_records_status ON food_records(status)")
                db.execSQL("CREATE TABLE IF NOT EXISTS food_recognition_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, requestedAt INTEGER NOT NULL, requestDate TEXT NOT NULL, outcome TEXT NOT NULL, model TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_recognition_attempts_requestDate ON food_recognition_attempts(requestDate)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS food_recognition_feedback (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, suggestedKey TEXT NOT NULL, confirmedKey TEXT NOT NULL, wasCorrect INTEGER NOT NULL, provider TEXT NOT NULL, model TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_recognition_feedback_createdAt ON food_recognition_feedback(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_food_recognition_feedback_suggestedKey ON food_recognition_feedback(suggestedKey)")
            }
        }
        fun create(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "focus.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build().also { instance = it }
        }
        fun closeInstance() { instance?.close(); instance = null }
    }
}
