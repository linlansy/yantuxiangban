package com.example.kaoyanfocus.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.example.kaoyanfocus.TimerSnapshot
import com.example.kaoyanfocus.TimerStore
import com.example.kaoyanfocus.LauncherIconManager
import com.example.kaoyanfocus.ThemeCatalog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private val Context.dataStore by preferencesDataStore("settings_v2")

object Dates {
    private val format = DateTimeFormatter.ISO_LOCAL_DATE
    fun today(): String = LocalDate.now().format(format)
    fun tomorrow(): String = LocalDate.now().plusDays(1).format(format)
    fun dateOf(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(format)
    fun localDate(value: String): LocalDate = LocalDate.parse(value, format)
}

data class AdminState(val configured: Boolean = false, val onboardingComplete: Boolean = false, val activeTheme: String = "default")

class AppRepository(private val context: Context, private val db: AppDatabase = AppDatabase.create(context)) {
    private val dao = db.dao()
    private val foodAiConfigStore = FoodAiConfigStore(context)
    private val foodRecognizer = QwenFoodRecognizer()
    private val pinHash = stringPreferencesKey("pin_hash")
    private val pinSalt = stringPreferencesKey("pin_salt")
    private val recoveryHash = stringPreferencesKey("recovery_hash")
    private val recoverySalt = stringPreferencesKey("recovery_salt")
    private val onboarding = booleanPreferencesKey("onboarding_complete")
    private val activeTheme = stringPreferencesKey("active_theme")
    private val seenStars = intPreferencesKey("seen_stars")
    private val lastPetWaveDate = stringPreferencesKey("last_pet_wave_date")

    val sessions = dao.sessions()
    val ledger = dao.ledger()
    val products = dao.activeProducts()
    val allProducts = dao.allProducts()
    val redemptions = dao.redemptions()
    val categories = dao.activeCategories()
    val allCategories = dao.allCategories()
    val balance = dao.balance()
    val latestRule = dao.latestRule()
    val taskTemplates = dao.taskTemplates()
    val rewardCodes = combine(dao.rewardCodes(), dao.petLetters()) { codes, letters ->
        val hiddenIds = letters.mapNotNullTo(mutableSetOf()) { it.rewardCodeId }
        codes.filter { it.id !in hiddenIds }
    }
    val achievements = dao.achievements()
    val unlocks = dao.unlocks()
    val restDays = dao.restDays()
    val reflections = dao.reflections()
    val petProfile = dao.petProfile()
    val petUnlocks = dao.petUnlocks()
    val petLetters = dao.petLetters()
    val foodRecords = dao.foodRecords()
    val petAffection = dao.affection()
    fun foodRecognitionCount(date: String = Dates.today()) = dao.recognitionCount(date)
    val encouragement = combine(sessions, reflections) { study, notes -> Encouragements.choose(study, notes) }
    val seenStarCount = context.dataStore.data.map { (it[seenStars] ?: 0).coerceIn(0, 100) }
    val petState = combine(sessions, petProfile, petUnlocks, petLetters) { study, profile, unlocked, letters ->
        val totalSeconds = study.sumOf(::effectiveSeconds)
        val xp = (totalSeconds / 60).toInt()
        val level = PetRules.levelForXp(xp)
        val stars = PetRules.starsFor(study)
        PetState(profile, totalSeconds.coerceAtMost(10 * 3600), xp, level, PetRules.nextLevelXp(level), unlocked, letters, stars,
            listOf(7, 30, 100).firstOrNull { it > stars }?.minus(stars))
    }

    suspend fun unlockedAchievementKeys(): Set<String> = dao.unlockSnapshot().mapTo(mutableSetOf()) { it.achievementKey }
    suspend fun achievementDefinitionsSnapshot(): List<AchievementDefinition> = dao.achievementSnapshot()
    suspend fun setSeenStarCount(count: Int) = context.dataStore.edit { it[seenStars] = count.coerceIn(0, 100) }
    suspend fun consumeDailyPetWave(): Boolean {
        val today = Dates.today()
        if (context.dataStore.data.first()[lastPetWaveDate] == today) return false
        context.dataStore.edit { it[lastPetWaveDate] = today }
        return true
    }

    fun foodAiConfig(): FoodAiConfig = foodAiConfigStore.effective()
    fun hasFoodAiOverride(): Boolean = foodAiConfigStore.hasOverride()

    suspend fun saveFoodAiConfig(apiKey: String, baseUrl: String, model: String) = withContext(Dispatchers.IO) {
        foodAiConfigStore.save(apiKey, baseUrl, model)
    }

    suspend fun clearFoodAiConfig() = withContext(Dispatchers.IO) { foodAiConfigStore.clear() }

    suspend fun testFoodAiConfig() = withContext(Dispatchers.IO) { foodRecognizer.test(foodAiConfigStore.effective()) }

    suspend fun prepareFood(uri: Uri, sourceType: String): PendingFoodRecognition = withContext(Dispatchers.IO) {
        val prepared = FoodImageStore.prepare(context, uri)
        val config = foodAiConfigStore.effective()
        if (!config.configured) return@withContext PendingFoodRecognition(prepared.thumbnailPath, sourceType, fallbackReason = "尚未配置AI，请手动选择食物")
        if (dao.recognitionCountSnapshot(Dates.today()) >= 10) return@withContext PendingFoodRecognition(prepared.thumbnailPath, sourceType, fallbackReason = "今日AI识别已达到10次，请手动选择食物")
        val now = System.currentTimeMillis()
        runCatching { foodRecognizer.recognize(prepared.uploadBytes, config) }
            .onSuccess { dao.insertRecognitionAttempt(FoodRecognitionAttempt(requestedAt = now, requestDate = Dates.today(), outcome = if (it.isFood) "SUCCESS" else "NOT_FOOD", model = config.model)) }
            .onFailure { dao.insertRecognitionAttempt(FoodRecognitionAttempt(requestedAt = now, requestDate = Dates.today(), outcome = "FAILED", model = config.model)) }
            .fold(
                onSuccess = { result ->
                    if (result.isFood) PendingFoodRecognition(prepared.thumbnailPath, sourceType, result)
                    else {
                        FoodImageStore.delete(prepared.thumbnailPath)
                        throw IllegalArgumentException("没有识别到食物，请换一张照片")
                    }
                },
                onFailure = { error -> PendingFoodRecognition(prepared.thumbnailPath, sourceType, fallbackReason = error.message ?: "识别失败，请手动选择食物") }
            )
    }

    suspend fun discardPendingFood(path: String?) = withContext(Dispatchers.IO) { FoodImageStore.delete(path) }

    suspend fun confirmFood(pending: PendingFoodRecognition, selectedKey: String): String = db.withTransaction {
        val definition = FoodCatalog.find(selectedKey)
        val unsafe = definition.unsafe
        dao.insertFoodRecord(FoodRecord(
            photoPath = pending.photoPath,
            sourceType = pending.sourceType,
            recognizedKey = definition.key,
            recognizedName = definition.name,
            candidatesCsv = pending.result?.candidates.orEmpty().joinToString(","),
            confidence = pending.result?.confidence ?: 0f,
            safetyState = if (unsafe) "UNSAFE" else "SAFE",
            status = if (unsafe) "REFUSED" else "READY",
            aiProvider = pending.result?.provider ?: "MANUAL",
            aiModel = pending.result?.model.orEmpty()
        ))
        pending.result?.let { result ->
            val correct = result.key == definition.key
            dao.insertRecognitionFeedback(FoodRecognitionFeedback(
                suggestedKey = result.key,
                confirmedKey = definition.key,
                wasCorrect = correct,
                provider = result.provider,
                model = result.model
            ))
            if (correct) "已确认：AI 识别正确，反馈已保存" else "已纠正：AI 认成${result.name}，实际是${definition.name}；反馈已保存"
        } ?: "已保存${definition.name}，下次可继续拍照识别"
    }

    suspend fun feedPet(recordId: Long): String = db.withTransaction {
        val record = dao.foodRecord(recordId) ?: return@withTransaction "这份食物不存在"
        if (record.safetyState == "UNSAFE" || record.status == "REFUSED") return@withTransaction "这份食物不适合我，不过谢谢你记得我"
        if (record.status == "FED") return@withTransaction "这份食物已经吃过啦"
        val today = Dates.today()
        if (dao.fedCount(today) >= 3) return@withTransaction "今天已经吃得很满足啦，明天再喂我吧"
        val remaining = feedingCooldownRemaining(dao.lastFedAt())
        if (remaining > 0) return@withTransaction "刚吃完，要消化${formatFeedingRemaining(remaining)}后才能再吃哦"
        dao.updateFoodRecord(record.copy(status = "FED", fedAt = System.currentTimeMillis(), feedDate = today))
        "吃得好开心！亲密度 +1"
    }

    suspend fun feedCinnamorollCottonCandy(): String = db.withTransaction {
        val today = Dates.today()
        if (dao.cinnamorollTreatCount(today) >= 1) return@withTransaction "今天的云朵棉花糖已经送过啦，明天再给它一份惊喜吧"
        if (dao.fedCount(today) >= 3) return@withTransaction "今天已经吃得很满足啦，明天再喂我吧"
        val remaining = feedingCooldownRemaining(dao.lastFedAt())
        if (remaining > 0) return@withTransaction "刚吃完，要消化${formatFeedingRemaining(remaining)}后才能再吃哦"
        val now = System.currentTimeMillis()
        val id = dao.insertFoodRecord(FoodRecord(
            photoPath = null, sourceType = "CINNAMOROLL_TREAT", recognizedKey = "cotton_candy", recognizedName = "棉花糖",
            status = "READY", aiProvider = "CINNAMOROLL_THEME", capturedAt = now, confirmedAt = now
        ))
        dao.updateFoodRecord(FoodRecord(
            id = id, photoPath = null, sourceType = "CINNAMOROLL_TREAT", recognizedKey = "cotton_candy", recognizedName = "棉花糖",
            status = "FED", fedAt = now, feedDate = today, aiProvider = "CINNAMOROLL_THEME", capturedAt = now, confirmedAt = now
        ))
        "棉花糖好甜呀！亲密度 +1"
    }

    private fun feedingCooldownRemaining(lastFedAt: Long?): Long =
        lastFedAt?.let { (2 * 60 * 60 * 1000L - (System.currentTimeMillis() - it)).coerceAtLeast(0) } ?: 0L

    private fun formatFeedingRemaining(millis: Long): String {
        val minutes = (millis + 59_999) / 60_000
        return if (minutes >= 60) "${minutes / 60}小时${minutes % 60}分" else "${minutes}分钟"
    }

    suspend fun deleteFoodRecord(id: Long) {
        val record = dao.foodRecord(id) ?: return
        db.withTransaction { dao.deleteFoodRecord(id) }
        withContext(Dispatchers.IO) { FoodImageStore.delete(record.photoPath) }
    }
    suspend fun claimConstellationReward(threshold: Int, title: String, points: Int): Boolean {
        val expectedPoints = mapOf(7 to 1, 30 to 5, 100 to 20)[threshold] ?: return false
        if (points != expectedPoints) return false
        if (PetRules.starsFor(dao.sessionSnapshot()) < threshold) return false
        return dao.insertLedger(LedgerEntry(
            createdAt = System.currentTimeMillis(),
            amount = points,
            type = "星图奖励",
            note = "收藏星图：$title",
            businessKey = "constellation_reward:$threshold",
            sourceDate = Dates.today()
        )) != -1L
    }
    val adminState = context.dataStore.data.map {
        AdminState(it[pinHash] != null, it[onboarding] ?: false, it[activeTheme] ?: "default")
    }
    fun tasksFor(date: String) = dao.tasksFor(date)
    fun reflection(date: String) = dao.reflection(date)

    suspend fun seed() = db.withTransaction {
        if (dao.categoryByName("学习") == null) dao.insertCategory(StudyCategory(name = "学习", lastUsedAt = System.currentTimeMillis()))
        if (dao.ruleFor(Dates.today()) == null) dao.insertRule(PointRuleVersion(effectiveDate = "2000-01-01"))
        dao.deleteProductByName("大耳狗主题")
        val productSnapshot = dao.productSnapshot()
        val starry = productSnapshot.firstOrNull { it.name == "星空夜读主题" }
        val starryLatest = Product(
            id = starry?.id ?: 0,
            name = "星空夜读主题",
            description = "解锁深蓝星空、月光夜读角色和专属图标",
            cost = 30,
            active = true,
            productType = "PERMANENT",
            entitlement = "永久使用星空夜读主题",
            themeKey = ThemeCatalog.STARRY
        )
        if (starry == null) dao.insertProduct(starryLatest) else dao.updateProduct(starryLatest)
        val productNames = productSnapshot.mapTo(mutableSetOf()) { it.name }
        if ("奶茶一次" !in productNames) dao.insertProduct(Product(name = "奶茶一次", description = "奖励自己一杯喜欢的奶茶", cost = 10, entitlement = "奶茶一次"))
        if ("看电影一次" !in productNames) dao.insertProduct(Product(name = "看电影一次", description = "兑换一次轻松的电影时间", cost = 20, entitlement = "看电影一次"))
        if ("休息半天" !in productNames) dao.insertProduct(Product(name = "休息半天", description = "安心休息半天再出发", cost = 30, entitlement = "休息半天"))
        val oldRestCard = productSnapshot.firstOrNull { it.name == "休息卡" }
        val makeupCard = productSnapshot.firstOrNull { it.name == "补签卡" }
        val makeupCardLatest = Product(
            id = makeupCard?.id ?: oldRestCard?.id ?: 0,
            name = "补签卡",
            description = "保护一个过去的空白日，不打断连续学习",
            cost = 10,
            active = true,
            productType = "CONSUMABLE",
            entitlement = "补签保护日"
        )
        if (makeupCard == null && oldRestCard == null) dao.insertProduct(makeupCardLatest) else dao.insertProduct(makeupCardLatest)
        if (oldRestCard != null && makeupCard != null && oldRestCard.id != makeupCard.id) dao.deleteProductByName("休息卡")
        dao.redemptionSnapshot().filter { it.productName == "休息卡" && it.usedAt == null }.forEach {
            dao.updateRedemption(it.copy(productName = "补签卡", entitlement = "补签保护日"))
        }
        val existing = dao.achievementSnapshot().associateBy { it.key }
        defaultAchievements().forEach { latest ->
            // Keep the user's configured reward, while refreshing names,
            // descriptions and ordering shipped by a newer app version.
            dao.insertAchievement(latest.copy(rewardPoints = existing[latest.key]?.rewardPoints ?: latest.rewardPoints))
        }
        ensurePetUnlockedAndRewards()
    }

    suspend fun ensureTasks(date: String) {
        val weekday = Dates.localDate(date).dayOfWeek.value
        dao.activeTaskTemplates().forEach { template ->
            val matches = when (template.recurrence) {
                "ONE_OFF" -> template.oneOffDate == date
                "WEEKLY" -> template.weekdays.split(",").mapNotNull(String::toIntOrNull).contains(weekday)
                else -> true
            }
            if (matches) dao.insertDailyTask(DailyTask(templateId = template.id, taskDate = date, title = template.title, description = template.description, points = template.points))
        }
    }

    suspend fun createOrUseCategory(nameInput: String): StudyCategory {
        val name = nameInput.trim().ifEmpty { "学习" }
        val now = System.currentTimeMillis()
        val existing = dao.categoryByName(name)
        return if (existing != null) {
            val updated = existing.copy(active = true, lastUsedAt = now)
            dao.updateCategory(updated)
            updated
        } else {
            val id = dao.insertCategory(StudyCategory(name = name, lastUsedAt = now))
            StudyCategory(id, name, true, now)
        }
    }

    suspend fun finishSession(timer: TimerSnapshot, endedAt: Long = System.currentTimeMillis()): Int {
        if (timer.accumulatedSeconds < 60) return 0
        val date = Dates.dateOf(timer.startedAt)
        val category = createOrUseCategory(timer.categoryName)
        val resolvedTheme = timer.themeKeyAtStart.ifBlank {
            context.dataStore.data.first()[activeTheme] ?: ThemeCatalog.DEFAULT
        }
        var awarded = 0
        db.withTransaction {
            dao.insertSession(StudySession(startedAt = timer.startedAt, endedAt = endedAt,
                minutes = (timer.accumulatedSeconds / 60).toInt(), points = 0,
                categoryId = category.id, categoryName = category.name, note = timer.note.trim(),
                durationSeconds = timer.accumulatedSeconds, pauseCount = timer.pauseCount,
                nightSeconds = timer.nightSeconds, earlySeconds = timer.earlySeconds, startDate = date,
                themeKeyAtStart = resolvedTheme,
                lateNightSeconds = TimerStore.decodeNightBuckets(timer.lateNightBuckets).values.sum()))
            awarded = recalculateStudyPoints(date)
            evaluateAchievements()
            ensurePetUnlockedAndRewards()
            processDailyLetter(timer, date)
        }
        return awarded
    }

    private suspend fun recalculateStudyPoints(date: String): Int {
        val rule = dao.ruleFor(date) ?: PointRuleVersion(effectiveDate = "2000-01-01")
        val totalMinutes = dao.sessionSnapshot().filter { normalizedDate(it) == date }.sumOf { effectiveSeconds(it) } / 60
        val capped = minOf(totalMinutes.toInt(), rule.capMinutes)
        val base = (capped / rule.minutesPerPoint) * rule.pointsPerBlock
        val bonus = if (totalMinutes >= rule.bonusMinutes) rule.bonusPoints else 0
        val old = dao.ledgerSnapshot().filter { it.businessKey == "study:$date:base" || it.businessKey == "study:$date:bonus" }.sumOf { it.amount }
        dao.upsertLedger(LedgerEntry(createdAt = System.currentTimeMillis(), amount = base, type = "学习", note = "$date 学习积分", businessKey = "study:$date:base", sourceDate = date))
        dao.upsertLedger(LedgerEntry(createdAt = System.currentTimeMillis(), amount = bonus, type = "八小时奖励", note = if (bonus > 0) "$date 满8小时奖励" else "$date 尚未达到8小时", businessKey = "study:$date:bonus", sourceDate = date))
        return base + bonus - old
    }

    suspend fun completeTask(id: Long): Boolean = db.withTransaction {
        val task = dao.dailyTaskById(id) ?: return@withTransaction false
        if (task.taskDate != Dates.today() || task.completedAt != null) return@withTransaction false
        val now = System.currentTimeMillis()
        dao.updateDailyTask(task.copy(completedAt = now))
        dao.insertLedger(LedgerEntry(createdAt = now, amount = task.points, type = "任务", note = "完成任务：${task.title}", businessKey = "task:${task.id}", sourceDate = task.taskDate))
        evaluateAchievements(); true
    }

    suspend fun addTaskTemplate(title: String, description: String, points: Int, recurrence: String, weekdays: String, oneOffDate: String?) {
        val id = dao.insertTaskTemplate(TaskTemplate(title = title.trim(), description = description.trim(), points = points.coerceAtLeast(0), recurrence = recurrence, weekdays = weekdays, oneOffDate = oneOffDate))
        ensureTasks(Dates.today())
        dao.insertAudit(AuditEntry(action = "新增任务", detail = "$id $title"))
    }
    suspend fun toggleTaskTemplate(item: TaskTemplate) = dao.updateTaskTemplate(item.copy(active = !item.active, updatedAt = System.currentTimeMillis()))

    suspend fun redeem(product: Product): String = db.withTransaction {
        if (!product.active) return@withTransaction "商品已下架"
        if (product.productType == "PERMANENT" && dao.redemptionSnapshot().any { it.productId == product.id }) return@withTransaction "已经永久拥有"
        if (dao.currentBalance() < product.cost) return@withTransaction "积分不足"
        val now = System.currentTimeMillis()
        dao.insertLedger(LedgerEntry(createdAt = now, amount = -product.cost, type = "商城消费", note = "兑换：${product.name}", businessKey = "redeem:$now:${product.id}", sourceDate = Dates.today()))
        dao.insertRedemption(Redemption(productId = product.id, productName = product.name, cost = product.cost, redeemedAt = now, entitlement = product.entitlement, productType = product.productType))
        evaluateAchievements(); "兑换成功"
    }

    suspend fun useEntitlement(id: Long): Boolean {
        val item = dao.redemptionById(id) ?: return false
        if (item.productType != "CONSUMABLE" || item.usedAt != null) return false
        dao.updateRedemption(item.copy(usedAt = System.currentTimeMillis())); return true
    }

    suspend fun useMakeupCard(date: String): String = db.withTransaction {
        val target = runCatching { Dates.localDate(date) }.getOrNull() ?: return@withTransaction "日期无效"
        if (!target.isBefore(LocalDate.now())) return@withTransaction "补签卡只能用于过去的日期"
        if (dao.restDay(date) != null) return@withTransaction "这一天已经受到保护"
        if (dao.sessionSnapshot().any { normalizedDate(it) == date }) return@withTransaction "这一天已有学习记录，不需要补签"
        val card = dao.redemptionSnapshot().filter { it.productName == "补签卡" && it.productType == "CONSUMABLE" && it.usedAt == null }.minByOrNull { it.redeemedAt }
            ?: return@withTransaction "没有可用的补签卡，请先到商城购买"
        val now = System.currentTimeMillis()
        dao.updateRedemption(card.copy(usedAt = now))
        dao.insertRestDay(RestDay(date, card.id, now, "MAKEUP_CARD"))
        dao.insertAudit(AuditEntry(action = "使用补签卡", detail = date))
        evaluateAchievements()
        "补签成功，$date 不会打断连续学习"
    }

    suspend fun scheduleWeeklyRest(date: String): String = db.withTransaction {
        val target = runCatching { Dates.localDate(date) }.getOrNull() ?: return@withTransaction "日期无效"
        val today = LocalDate.now()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekEnd = weekStart.plusDays(6)
        if (target.isBefore(today) || target.isAfter(weekEnd)) return@withTransaction "只能选择今天或本周未来日期"
        if (dao.restDaySnapshot().any { it.sourceType == "WEEKLY_FREE" && it.weekStart == weekStart.toString() }) return@withTransaction "本周的主动休息日已经使用"
        if (dao.restDay(date) != null) return@withTransaction "这一天已经受到保护"
        val now = System.currentTimeMillis()
        dao.insertRestDay(RestDay(date, null, now, "WEEKLY_FREE", weekStart.toString()))
        dao.insertAudit(AuditEntry(action = "设置主动休息日", detail = date))
        "已将 $date 设为本周主动休息日，确认后不可更换"
    }
    suspend fun addProduct(name: String, description: String, cost: Int, permanent: Boolean) =
        dao.insertProduct(Product(name = name.trim(), description = description.trim(), cost = cost.coerceAtLeast(1), productType = if (permanent) "PERMANENT" else "CONSUMABLE", entitlement = description.trim()))
    suspend fun toggleProduct(product: Product) = dao.updateProduct(product.copy(active = !product.active))

    suspend fun createCodes(custom: String?, count: Int, points: Int): List<String> {
        val values = if (!custom.isNullOrBlank()) listOf(normalizeCode(custom)) else List(count.coerceIn(1, 50)) { randomCode() }
        val saved = mutableListOf<String>()
        values.distinct().forEach { code ->
            val id = dao.insertRewardCode(RewardCode(codeHash = codeHash(code), displayCode = code, points = points.coerceAtLeast(1)))
            if (id > 0) saved += code
        }
        return saved
    }
    suspend fun toggleCode(item: RewardCode) { if (item.usedAt == null) dao.updateRewardCode(item.copy(active = !item.active)) }
    suspend fun claimCode(input: String): String = db.withTransaction {
        val code = dao.rewardCode(codeHash(normalizeCode(input))) ?: return@withTransaction "兑换码不存在"
        if (!code.active) return@withTransaction "兑换码已停用"
        if (code.usedAt != null) return@withTransaction "兑换码已使用"
        val now = System.currentTimeMillis()
        dao.updateRewardCode(code.copy(usedAt = now))
        when (code.rewardType) {
            "MAKEUP_CARD" -> dao.insertRedemption(Redemption(productId = 0, productName = "补签卡", cost = 0, redeemedAt = now, entitlement = "来信隐藏兑换码奖励", productType = "CONSUMABLE"))
            "PET_ITEM" -> {
                val itemKey = code.rewardPayload.ifBlank { "outfit_starry" }
                val itemType = PetRules.rewards.firstOrNull { it.key == itemKey }?.type ?: "OUTFIT_MAIN"
                dao.insertPetUnlock(PetUnlock(itemKey, itemType, 0, now, "LETTER_CODE"))
            }
            else -> dao.insertLedger(LedgerEntry(createdAt = now, amount = code.points, type = "兑换码", note = "兑换码奖励", businessKey = "code:${code.id}", sourceDate = Dates.today()))
        }
        when (code.rewardType) {
            "MAKEUP_CARD" -> "兑换成功，获得1张补签卡"
            "PET_ITEM" -> "兑换成功，获得限定宠物物品"
            else -> "兑换成功，获得 ${code.points} 积分"
        }
    }

    suspend fun saveRule(minutes: Int, perBlock: Int, bonusMinutes: Int, bonusPoints: Int, capMinutes: Int) {
        dao.insertRule(PointRuleVersion(effectiveDate = Dates.tomorrow(), minutesPerPoint = minutes.coerceAtLeast(1), pointsPerBlock = perBlock.coerceAtLeast(0), bonusMinutes = bonusMinutes.coerceAtLeast(1), bonusPoints = bonusPoints.coerceAtLeast(0), capMinutes = capMinutes.coerceAtLeast(minutes)))
        dao.insertAudit(AuditEntry(action = "修改积分规则", detail = "次日生效"))
    }
    suspend fun saveReflection(date: String, note: String, summary: String, mood: Int) =
        dao.insertReflection(DailyReflection(date, note.trim(), summary.trim(), mood.coerceIn(1, 5)))
    suspend fun updateCategory(item: StudyCategory, name: String, active: Boolean) = dao.updateCategory(item.copy(name = name.trim(), active = active))
    suspend fun deleteSession(id: Long) {
        val session = dao.sessionById(id) ?: return
        dao.deleteSession(id)
        val date = normalizedDate(session)
        recalculateStudyPoints(date)
        dao.insertAudit(AuditEntry(action = "删除学习记录", detail = "记录$id，日期$date"))
        evaluateAchievements()
        syncLauncherIcon()
    }

    suspend fun addTestStudyDuration(date: String, startTime: String, hours: Int, minutes: Int, categoryName: String): String {
        val target = runCatching { Dates.localDate(date) }.getOrNull() ?: return "日期格式应为 YYYY-MM-DD"
        if (target.isAfter(LocalDate.now())) return "不能补录未来日期"
        val time = runCatching { LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: return "开始时间格式应为 HH:mm"
        val totalMinutes = (hours.coerceAtLeast(0) * 60L + minutes.coerceIn(0, 59)).coerceAtMost(1000L * 60)
        if (totalMinutes < 1) return "学习时长至少为1分钟"
        val category = createOrUseCategory(categoryName.ifBlank { "测试学习" })
        val startedAt = target.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endedAt = startedAt + totalMinutes * 60_000
        val interval = TimerStore.analyzeInterval(startedAt, endedAt)
        val themeAtStart = context.dataStore.data.first()[activeTheme] ?: ThemeCatalog.DEFAULT
        val simulatedTimer = TimerSnapshot(
            startedAt = startedAt,
            accumulatedSeconds = totalMinutes * 60,
            categoryId = category.id,
            categoryName = category.name,
            note = "管理端补录测试时长",
            nightSeconds = interval.nightSeconds,
            earlySeconds = interval.earlySeconds,
            themeKeyAtStart = themeAtStart,
            lateNightBuckets = TimerStore.encodeNightBuckets(interval.lateNightBuckets)
        )
        db.withTransaction {
            dao.insertSession(StudySession(
                startedAt = startedAt,
                endedAt = endedAt,
                minutes = totalMinutes.toInt(),
                points = 0,
                categoryId = category.id,
                categoryName = category.name,
                note = "管理端补录测试时长",
                durationSeconds = totalMinutes * 60,
                nightSeconds = interval.nightSeconds,
                earlySeconds = interval.earlySeconds,
                startDate = date,
                themeKeyAtStart = themeAtStart,
                lateNightSeconds = interval.lateNightBuckets.values.sum()
            ))
            recalculateStudyPoints(date)
            dao.insertAudit(AuditEntry(action = "补录测试学习", detail = "$date $totalMinutes 分钟"))
            evaluateAchievements()
            ensurePetUnlockedAndRewards()
            processDailyLetter(simulatedTimer, date, ignoreNamedAt = true)
        }
        return "已补录 $date $startTime 开始的 $totalMinutes 分钟学习（${ThemeCatalog.nameOf(themeAtStart)}）"
    }
    suspend fun updateAchievementReward(item: AchievementDefinition, points: Int) {
        if (dao.unlockSnapshot().none { it.achievementKey == item.key }) dao.insertAchievement(item.copy(rewardPoints = points.coerceAtLeast(0)))
    }

    suspend fun namePet(input: String): String = db.withTransaction {
        val name = input.trim()
        val length = name.codePointCount(0, name.length)
        if (length !in 1..12) return@withTransaction "宠物名字需要1–12个字符"
        val profile = dao.petProfileSnapshot() ?: return@withTransaction "累计学习10小时后才能给宠物命名"
        val now = System.currentTimeMillis()
        dao.insertPetProfile(profile.copy(name = name, namedAt = profile.namedAt ?: now))
        if (profile.name.isBlank()) "命名成功，$name 会从现在开始陪你学习" else "宠物已改名为 $name"
    }

    suspend fun equipPetItem(item: PetUnlock): String = db.withTransaction {
        val profile = dao.petProfileSnapshot() ?: return@withTransaction "宠物尚未解锁"
        val updated = when (item.itemType) {
            "EXPRESSION" -> profile
            "OUTFIT", "OUTFIT_MAIN" -> profile.copy(equippedOutfit = if (profile.equippedOutfit == item.itemKey) "" else item.itemKey)
            "ACCESSORY_EYE" -> profile.copy(equippedEyeAccessory = if (profile.equippedEyeAccessory == item.itemKey) "" else item.itemKey)
            "ACCESSORY_NECK" -> profile.copy(equippedNeckAccessory = if (profile.equippedNeckAccessory == item.itemKey) "" else item.itemKey)
            "ACCESSORY_HEAD" -> profile.copy(equippedHeadAccessory = if (profile.equippedHeadAccessory == item.itemKey) "" else item.itemKey)
            "ACTION" -> profile
            "FURNITURE" -> {
                val placed = profile.placedFurnitureCsv.split(',').filter { it.isNotBlank() }.toMutableSet()
                if (!placed.add(item.itemKey)) placed.remove(item.itemKey)
                profile.copy(placedFurnitureCsv = placed.joinToString(","))
            }
            else -> profile
        }
        dao.insertPetProfile(updated)
        when {
            item.itemType == "EXPRESSION" -> "表情会根据学习状态自动出现"
            item.itemType == "ACTION" -> "正在播放${item.itemKey.let { key -> PetRules.rewards.firstOrNull { it.key == key }?.name ?: "宠物动作" }}"
            updated == profile -> "宠物状态没有变化"
            else -> "宠物房间已更新"
        }
    }

    suspend fun setPetOnFocus(show: Boolean): String = db.withTransaction {
        val profile = dao.petProfileSnapshot() ?: return@withTransaction "累计学习10小时后才能显示宠物"
        dao.insertPetProfile(profile.copy(showOnFocus = show))
        if (show) "已在专注页显示宠物" else "已恢复主题原角色"
    }

    private suspend fun ensurePetUnlockedAndRewards() {
        val totalSeconds = dao.sessionSnapshot().sumOf(::effectiveSeconds)
        var profile = dao.petProfileSnapshot()
        if (profile == null && totalSeconds >= 10 * 3600) {
            profile = PetProfile(unlockedAt = System.currentTimeMillis())
            dao.insertPetProfile(profile)
            dao.insertAudit(AuditEntry(action = "解锁宠物模块", detail = "累计学习10小时"))
        }
        if (profile != null) {
            val xp = (totalSeconds / 60).toInt()
            val level = PetRules.levelForXp(xp)
            val now = System.currentTimeMillis()
            PetRules.rewards.filter { it.level <= level }.forEach {
                dao.insertPetUnlock(PetUnlock(it.key, it.type, it.level, now))
            }
            if (level > profile.highestRewardLevel) {
                dao.insertPetProfile(profile.copy(highestRewardLevel = level))
            }
        }
    }

    private suspend fun processDailyLetter(timer: TimerSnapshot, date: String, ignoreNamedAt: Boolean = false) {
        val profile = dao.petProfileSnapshot() ?: return
        val namedAt = profile.namedAt ?: return
        if (!ignoreNamedAt && timer.startedAt < namedAt) return
        val daySeconds = dao.sessionSnapshot()
            .filter { normalizedDate(it) == date }
            .sumOf(::effectiveSeconds)
        if (daySeconds < 8 * 3600) return

        val received = dao.petLetterSnapshot().mapTo(mutableSetOf()) { it.templateIndex }
        val progressKey = "day:$date"
        val old = dao.nightProgress(progressKey) ?: NightStudyProgress(progressKey)
        if (old.letterIssued || received.size >= 24) return

        val index = (1..24).first { it !in received }
        val template = letterTemplate(index)
        val now = System.currentTimeMillis()
        var codeId: Long? = null
        var displayCode: String? = null
        if (index == 3 || index == 7 || index == 12 || index == 16 || index == 20 || index == 24) {
            displayCode = randomCode(4)
            val rewardType = when (index) { 7, 20 -> "MAKEUP_CARD"; 12, 24 -> "PET_ITEM"; else -> "POINTS" }
            codeId = dao.insertRewardCode(RewardCode(
                codeHash = codeHash(normalizeCode(displayCode)), displayCode = displayCode,
                points = if (index == 3) 3 else if (index == 16) 5 else 0, rewardType = rewardType,
                rewardPayload = when (index) {
                    12 -> "outfit_starry"
                    24 -> "furniture_cloud_cushion"
                    else -> ""
                }
            ))
        }
        dao.insertPetLetter(PetLetter(templateIndex = index, title = template.first,
            body = "${template.second}\n\n——${profile.name}", nightKey = progressKey,
            receivedAt = now, rewardCodeId = codeId, displayCode = displayCode))
        dao.insertNightProgress(old.copy(seconds = daySeconds, letterIssued = true))
    }

    private fun letterTemplate(index: Int): Pair<String, String> = listOf(
        "星星替你记住" to "今天已经走了很长一段路，星星都替你记住了。收好今天的认真，也记得让眼睛休息。",
        "风知道" to "房间很安静，可是风知道你又往前走了一大段。不用急，我会陪着你。",
        "月光口袋" to "我把今天的光装进了小口袋。口袋里还有一份小奖励，等你发现。",
        "慢一点也可以" to "有时候书页会变得很重，慢一点也可以。你没有停在原地，这就很了不起。",
        "今天的书页" to "今天翻过的每一页，都在为未来铺路。我替你把这份安静的努力收好了。",
        "你已经走很远" to "回头看看，你已经走了很远。还没到终点也没关系，今天也请好好休息。",
        "给明天留一盏灯" to "我给明天留了一盏小灯，也给你藏了一张小卡片。需要的时候，它会替你守住坚持。",
        "安静的勇气" to "没有掌声的夜里，坚持也是一种安静的勇气。我看见了，你也要相信自己。",
        "云朵休息站" to "如果觉得累了，就在云朵休息站停一会儿。休息好以后，我们再一起出发。",
        "离目标更近" to "今天的努力很小，又很重要。它让你离目标更近了一点，这就值得庆祝。",
        "别忘了喝水" to "我在桌角给你留了一杯水。别忘了喝几口，再把肩膀轻轻放松。",
        "我会一直陪你" to "谢谢你让我陪你走过这么多认真的日子。我会一直陪你，直到你走进想去的地方。这份限定衣服是我们的约定。",
        "窗边的晨光" to "今天的晨光落在书页上，像给你盖了一个小小的印章。别着急，我们把眼前这一页读好。",
        "把烦恼放一边" to "如果有很多事情挤在心里，就先把它们放到桌边。现在只需要陪我完成这一小段专注。",
        "认真也会开花" to "你看不见的那些积累，正在慢慢长出根来。认真从来不会白费，只是需要一点时间。",
        "小小补给站" to "我准备了一份小补给，留给愿意继续往前走的你。累的时候，记得也可以向我靠一靠。",
        "雨天的书桌" to "外面下不下雨都没关系，书桌前这一盏灯会一直亮着。今天的你已经做得很好。",
        "给未来的明信片" to "我替现在的你寄出一张明信片：上面写着‘我正在认真靠近想去的地方’。",
        "晚风的节奏" to "晚风会把嘈杂带走一点点。跟着自己的节奏读下去，不必和任何人比较。",
        "口袋里的勇气" to "遇到难题时，就从口袋里摸一摸勇气。它一直都在，只是有时需要你先相信它。",
        "一页一页" to "目标很远的时候，就只看下一页。每翻过一页，我们就已经比刚才更靠近一点。",
        "给努力一个拥抱" to "今天的你值得一个轻轻的拥抱。不是因为做得完美，而是因为你没有放弃认真。",
        "云朵坐垫" to "我把最软的一朵云留给你。想休息时就坐一会儿，休息好以后，我们再一起出发。",
        "下一次见面" to "等你再次打开这封信时，也许又完成了好多页。无论走到哪里，我都会为你摇摇尾巴。"
    )[index - 1]

    private suspend fun evaluateAchievements() {
        val sessions = dao.sessionSnapshot()
        val tasks = dao.allDailyTasks()
        val redemptions = dao.redemptionSnapshot()
        val definitions = dao.achievementSnapshot()
        val unlocked = dao.unlockSnapshot().associateBy { it.achievementKey }
        val secondsByDate = sessions.groupBy(::normalizedDate).mapValues { e -> e.value.sumOf(::effectiveSeconds) }
        val learnedDates = (secondsByDate.filterValues { it >= 60 }.keys + dao.restDaySnapshot().filter { !Dates.localDate(it.date).isAfter(LocalDate.now()) }.map { it.date }).map(Dates::localDate).distinct().sorted()
        val longestStreak = longestStreak(learnedDates)
        val qualified = buildSet {
            if (sessions.any { effectiveSeconds(it) >= 60 }) add("first_session")
            if (secondsByDate.values.any { it >= 12 * 3600 }) add("study_maniac")
            if (sessions.groupBy(::normalizedDate).values.any { list -> list.sumOf { it.nightSeconds } >= 3600 }) add("night_owl")
            if (sessions.any { it.pauseCount >= 5 }) add("pause_king")
            if (longestStreak >= 7) add("streak_7")
            if (sessions.sumOf(::effectiveSeconds) >= 10 * 3600) add("total_10")
            if (sessions.sumOf(::effectiveSeconds) >= 100 * 3600) add("total_100")
            if (redemptions.isNotEmpty()) add("first_redeem")
            if (tasks.count { it.completedAt != null } >= 10) add("task_master")
            if (sessions.groupBy(::normalizedDate).values.any { list -> list.sumOf { it.earlySeconds } >= 3600 }) add("early_bird")
            if (secondsByDate.values.any { it >= 8 * 3600 }) add("day_8")
            if (longestStreak >= 30) add("streak_30")
        }
        definitions.forEach { definition ->
            val current = unlocked[definition.key]
            if (definition.key in qualified && current == null) {
                val now = System.currentTimeMillis()
                dao.insertUnlock(AchievementUnlock(definition.key, now, definition.rewardPoints))
                dao.upsertLedger(LedgerEntry(createdAt = now, amount = definition.rewardPoints, type = "成就", note = "解锁成就：${definition.name}", businessKey = "achievement:${definition.key}", sourceDate = Dates.today()))
            } else if (definition.key !in qualified && current != null) {
                dao.deleteUnlock(definition.key)
                dao.upsertLedger(LedgerEntry(createdAt = System.currentTimeMillis(), amount = 0, type = "成就撤销", note = "因历史纠错撤销：${definition.name}", businessKey = "achievement:${definition.key}", sourceDate = Dates.today()))
                dao.insertAudit(AuditEntry(action = "撤销成就", detail = definition.name))
            }
        }
    }

    suspend fun setPin(value: String): String {
        require(value.matches(Regex("\\d{6}")))
        val salt = randomHex(16); val recovery = randomCode(4); val recoverySaltValue = randomHex(16)
        context.dataStore.edit {
            it[pinSalt] = salt; it[pinHash] = secureHash("$salt:$value")
            it[recoverySalt] = recoverySaltValue; it[recoveryHash] = secureHash("$recoverySaltValue:$recovery")
        }
        return recovery
    }
    suspend fun verifyPin(value: String): Boolean {
        val p = context.dataStore.data.first(); val salt = p[pinSalt] ?: return false
        return constantEquals(p[pinHash] ?: return false, secureHash("$salt:$value"))
    }
    suspend fun resetPinWithRecovery(recovery: String, newPin: String): String? {
        val p = context.dataStore.data.first(); val salt = p[recoverySalt] ?: return null
        if (!constantEquals(p[recoveryHash] ?: return null, secureHash("$salt:${normalizeCode(recovery)}"))) return null
        return setPin(newPin)
    }
    suspend fun completeOnboarding() = context.dataStore.edit { it[onboarding] = true }
    private suspend fun ownedThemeKeys(): Set<String> = ThemeCatalog.owned(dao.redemptionSnapshot(), dao.unlockSnapshot()).mapTo(mutableSetOf()) { it.key }

    suspend fun setTheme(key: String): Boolean {
        if (key !in ownedThemeKeys()) return false
        context.dataStore.edit { it[activeTheme] = key }
        // Changing activity-alias while this screen is visible sends some
        // phones back to Home. Update the UI and notification now; the
        // launcher alias is applied from MainActivity.onStop instead.
        LauncherIconManager.rememberTheme(context, key)
        return true
    }

    suspend fun syncLauncherIcon() {
        val saved = context.dataStore.data.first()[activeTheme] ?: ThemeCatalog.DEFAULT
        val key = saved.takeIf { it in ownedThemeKeys() } ?: ThemeCatalog.DEFAULT
        if (key != saved) context.dataStore.edit { it[activeTheme] = key }
        LauncherIconManager.rememberTheme(context, key)
    }

    suspend fun resetProgress() {
        db.withTransaction {
            dao.clearSessions(); dao.clearLedger(); dao.clearRedemptions(); dao.clearDailyTasks(); dao.clearUnlocks(); dao.clearReflections(); dao.clearRestDays(); dao.clearAudit()
            dao.clearPetProfiles(); dao.clearPetUnlocks(); dao.clearPetLetters(); dao.clearNightProgress()
            dao.clearFoodRecords(); dao.clearRecognitionAttempts()
        }
        withContext(Dispatchers.IO) { FoodImageStore.clear(context) }
        setSeenStarCount(0)
        setTheme(ThemeCatalog.DEFAULT)
    }
    suspend fun resetConfiguration() = db.withTransaction {
        dao.clearCategories(); dao.clearRules(); dao.clearTaskTemplates(); dao.clearProducts(); dao.clearRewardCodes(); seed()
    }
    suspend fun factoryReset() {
        db.clearAllTables()
        context.dataStore.edit { it.clear() }
        TimerStore.clear(context)
        LauncherIconManager.applyTheme(context, ThemeCatalog.DEFAULT)
        withContext(Dispatchers.IO) { FoodImageStore.clear(context); foodAiConfigStore.clear() }
        seed()
    }

    suspend fun exportEncrypted(uri: Uri, password: String) {
        require(password.length >= 8)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        val raw = context.getDatabasePath("focus.db").readBytes()
        val salt = SecureRandom().generateSeed(16); val iv = SecureRandom().generateSeed(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        context.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
            out.write("YTBD2.0".toByteArray()); out.write(salt); out.write(iv); out.write(cipher.doFinal(raw))
        }
    }
    suspend fun importEncrypted(uri: Uri, password: String) {
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        require(bytes.size > 44 && String(bytes.copyOfRange(0, 7)) == "YTBD2.0") { "不是有效的研途相伴备份" }
        val salt = bytes.copyOfRange(7, 23); val iv = bytes.copyOfRange(23, 35)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        val raw = cipher.doFinal(bytes.copyOfRange(35, bytes.size))
        require(String(raw.copyOfRange(0, 15)) == "SQLite format 3") { "备份内容损坏" }
        AppDatabase.closeInstance()
        context.getDatabasePath("focus.db").writeBytes(raw)
        context.getDatabasePath("focus.db-wal").delete(); context.getDatabasePath("focus.db-shm").delete()
        context.dataStore.edit { it.remove(pinHash); it.remove(pinSalt); it.remove(recoveryHash); it.remove(recoverySalt); it.remove(seenStars); it[onboarding] = false }
    }

    private fun effectiveSeconds(item: StudySession) = if (item.durationSeconds > 0) item.durationSeconds else item.minutes * 60L
    private fun normalizedDate(item: StudySession) = item.startDate.ifBlank { Dates.dateOf(item.startedAt) }
    private fun longestStreak(values: List<LocalDate>): Int {
        if (values.isEmpty()) return 0
        var best = 1; var current = 1
        values.distinct().sorted().zipWithNext().forEach { (a, b) ->
            current = if (ChronoUnit.DAYS.between(a, b) == 1L) current + 1 else 1; best = maxOf(best, current)
        }
        return best
    }
    private fun defaultAchievements() = listOf(
        AchievementDefinition("first_session", "初次启程", "完成第一次有效学习", "book", false, 1, 1),
        AchievementDefinition("study_maniac", "学习狂魔", "单日学习达到12小时", "trophy", false, 5, 2),
        AchievementDefinition("night_owl", "夜猫子", "同一天00:00–04:00学习1小时", "moon", true, 2, 3),
        AchievementDefinition("pause_king", "暂停大王", "单次学习暂停5次", "pause", true, 1, 4),
        AchievementDefinition("streak_7", "七日坚持", "连续7天学习", "flame", false, 3, 5),
        AchievementDefinition("total_10", "累计10小时", "累计学习10小时", "clock", false, 2, 6),
        AchievementDefinition("total_100", "累计100小时", "累计学习100小时", "medal", false, 10, 7),
        AchievementDefinition("first_redeem", "首次兑换", "首次兑换商城商品", "gift", false, 1, 8),
        AchievementDefinition("task_master", "任务达人", "累计完成10个任务", "check", false, 3, 9),
        AchievementDefinition("early_bird", "早起鸟", "同一天05:00–09:00学习1小时", "sun", true, 2, 10),
        AchievementDefinition("day_8", "八小时挑战", "单日学习达到8小时", "star", false, 3, 11),
        AchievementDefinition("streak_30", "三十日坚持", "连续30天学习", "crown", false, 10, 12)
    )
    private fun normalizeCode(value: String) = value.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
    private fun codeHash(value: String) = secureHash("reward:$value")
    private fun randomCode(groups: Int = 3): String = List(groups) { randomHex(2).uppercase() }.joinToString("-")
    private fun randomHex(bytes: Int): String = SecureRandom().generateSeed(bytes).joinToString("") { "%02x".format(it) }
    private fun secureHash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun constantEquals(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 150_000, 256)).encoded
        return SecretKeySpec(bytes, "AES")
    }
}
