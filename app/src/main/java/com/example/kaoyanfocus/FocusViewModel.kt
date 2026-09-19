package com.example.kaoyanfocus

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaoyanfocus.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class TimerUi(
    val active: Boolean = false,
    val running: Boolean = false,
    val elapsedSeconds: Long = 0,
    val categoryId: Long = 0,
    val categoryName: String = "学习",
    val note: String = "",
    val pauseCount: Int = 0,
    val pauseEggMask: Int = 0
)

data class PetMotionUi(
    val actionKey: String? = null,
    val token: Long = 0,
    val caption: String? = null,
    val priority: Int = 0,
    val foodPhotoPath: String? = null,
    val foodKey: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FocusViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    val sessions = repo.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ledger = repo.ledger.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products = repo.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allProducts = repo.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val redemptions = repo.redemptions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCategories = repo.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val balance = repo.balance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val rule = repo.latestRule.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val templates = repo.taskTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val codes = repo.rewardCodes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val achievements = repo.achievements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val unlocks = repo.unlocks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val restDays = repo.restDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val petState = repo.petState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetState())
    val foodRecords = repo.foodRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val petAffection = repo.petAffection.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val foodRecognitionCount = repo.foodRecognitionCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val seenStarCount = repo.seenStarCount.stateIn(viewModelScope, SharingStarted.Eagerly, -1)
    val encouragement = repo.encouragement.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "今天也向目标靠近一点。")
    val adminState = repo.adminState.stateIn(viewModelScope, SharingStarted.Eagerly, AdminState())

    private val _selectedDate = MutableStateFlow(Dates.today())
    val selectedDate = _selectedDate.asStateFlow()
    val selectedTasks = _selectedDate.flatMapLatest(repo::tasksFor).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedReflection = _selectedDate.flatMapLatest(repo::reflection).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _timer = MutableStateFlow(TimerUi())
    val timer = _timer.asStateFlow()
    private val _adminUnlocked = MutableStateFlow(false)
    val adminUnlocked = _adminUnlocked.asStateFlow()
    private val _recoveryCode = MutableStateFlow<String?>(null)
    val recoveryCode = _recoveryCode.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _petMotion = MutableStateFlow(PetMotionUi())
    val petMotion = _petMotion.asStateFlow()
    private var petMotionJob: Job? = null
    private var lastPetTapAt = 0L
    private val _pendingFood = MutableStateFlow<PendingFoodRecognition?>(null)
    val pendingFood = _pendingFood.asStateFlow()
    private val _foodLoading = MutableStateFlow(false)
    val foodLoading = _foodLoading.asStateFlow()
    private val _foodNotice = MutableStateFlow<String?>(null)
    val foodNotice = _foodNotice.asStateFlow()
    private val _cottonCandyEffectToken = MutableStateFlow(0L)
    val cottonCandyEffectToken = _cottonCandyEffectToken.asStateFlow()
    private val _foodAiConfig = MutableStateFlow(repo.foodAiConfig())
    val foodAiConfig = _foodAiConfig.asStateFlow()
    private val _foodAiOverride = MutableStateFlow(repo.hasFoodAiOverride())
    val foodAiOverride = _foodAiOverride.asStateFlow()
    private val _achievementQueue = MutableStateFlow<List<AchievementDefinition>>(emptyList())
    val achievementNotice = _achievementQueue.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { repo.seed(); repo.ensureTasks(Dates.today()); repo.syncLauncherIcon() }
        viewModelScope.launch {
            while (true) {
                refreshTimer()
                delay(1_000)
            }
        }
        viewModelScope.launch {
            var knownKeys = repo.unlockedAchievementKeys()
            repo.unlocks.collect { current ->
                val currentKeys = current.mapTo(mutableSetOf()) { it.achievementKey }
                val newKeys = currentKeys - knownKeys
                if (newKeys.isNotEmpty()) {
                    val definitions = repo.achievementDefinitionsSnapshot().associateBy { it.key }
                    val notices = current.filter { it.achievementKey in newKeys }
                        .sortedBy { it.unlockedAt }
                        .mapNotNull { definitions[it.achievementKey] }
                    if (notices.isNotEmpty()) _achievementQueue.update { it + notices }
                    if (notices.isNotEmpty() && petState.value.unlocks.any { it.itemKey == "action_celebrate" }) {
                        playPetMotion("action_celebrate", PetMotionTiming.CELEBRATE, priority = 3, caption = "新成就解锁！")
                    }
                }
                knownKeys = currentKeys
            }
        }
        viewModelScope.launch {
            var knownRewardLevel = 1
            petState.collect { current ->
                val permanentLevel = current.profile?.highestRewardLevel ?: 1
                if (permanentLevel > knownRewardLevel && current.unlocks.any { it.itemKey == "action_celebrate" }) {
                    playPetMotion("action_celebrate", PetMotionTiming.CELEBRATE, priority = 3, caption = "Lv.$permanentLevel 新奖励已解锁")
                }
                knownRewardLevel = maxOf(knownRewardLevel, permanentLevel)
            }
        }
    }

    private fun refreshTimer() {
        val value = TimerStore.load(getApplication())
        _timer.value = if (value == null) TimerUi() else TimerUi(true, value.running, value.elapsed(), value.categoryId, value.categoryName, value.note, value.pauseCount, value.pauseEggMask)
    }

    fun start(categoryName: String, note: String) = viewModelScope.launch {
        val category = repo.createOrUseCategory(categoryName)
        TimerStore.start(getApplication(), category.id, category.name, note, adminState.value.activeTheme)
        refreshTimer()
    }
    fun pause() {
        val paused = TimerStore.pause(getApplication())
        if (paused != null && petState.value.named) {
            val egg = when (paused.pauseCount) {
                1 -> 1 to "喝水"
                3 -> if (petState.value.unlocks.any { it.itemKey == "action_stretch" }) 2 to "伸懒腰" else null
                5 -> 4 to "趴在桌上陪你歇一会儿"
                10 -> 8 to "今天也许真的需要休息一下"
                else -> null
            }
            if (egg != null && paused.pauseEggMask and egg.first == 0) {
                TimerStore.markPauseEggShown(getApplication(), egg.first)
                _message.value = "${petState.value.profile?.name}正在${egg.second}"
                val action = when (paused.pauseCount) {
                    1 -> "action_drink"
                    3 -> "action_stretch".takeIf { petState.value.unlocks.any { unlock -> unlock.itemKey == it } }
                    5, 10 -> "action_lie"
                    else -> null
                }
                if (action != null) playPetMotion(action, PetMotionTiming.forAction(action), priority = 2,
                    caption = if (paused.pauseCount == 10) "今天也许真的需要休息一下" else null)
            }
        }
        refreshTimer()
    }
    fun resume() {
        TimerStore.resume(getApplication()); refreshTimer()
    }
    fun updateTimerContent(categoryName: String, note: String) = viewModelScope.launch {
        val category = repo.createOrUseCategory(categoryName)
        TimerStore.updateContent(getApplication(), category.id, category.name, note); refreshTimer()
    }
    fun finish(result: (Int, Boolean) -> Unit = { _, _ -> }) = viewModelScope.launch {
        val value = TimerStore.finalize(getApplication()) ?: return@launch
        val discarded = value.accumulatedSeconds < 60
        val points = if (discarded) 0 else repo.finishSession(value)
        TimerStore.clear(getApplication()); refreshTimer()
        if (!discarded && petState.value.unlocks.any { it.itemKey == "action_celebrate" }) {
            playPetMotion("action_celebrate", PetMotionTiming.CELEBRATE, priority = 3, caption = "今天的专注已收好")
        }
        result(points, discarded)
    }

    fun redeem(product: Product) = viewModelScope.launch { _message.value = repo.redeem(product) }
    fun useEntitlement(id: Long) = viewModelScope.launch { _message.value = if (repo.useEntitlement(id)) "权益已使用" else "权益不可用" }
    fun useRestCard(date: String) = viewModelScope.launch { _message.value = repo.useMakeupCard(date) }
    fun scheduleWeeklyRest(date: String) = viewModelScope.launch { _message.value = repo.scheduleWeeklyRest(date) }
    fun namePet(name: String) = viewModelScope.launch { _message.value = repo.namePet(name) }
    fun equipPetItem(item: PetUnlock) = viewModelScope.launch {
        if (item.itemType == "ACTION") {
            playPetAction(item)
            return@launch
        }
        _message.value = repo.equipPetItem(item)
        if (item.itemType in setOf("OUTFIT", "OUTFIT_MAIN", "ACCESSORY_EYE", "ACCESSORY_NECK", "ACCESSORY_HEAD") &&
            petState.value.unlocks.any { it.itemKey == "action_wave" }) {
            playPetMotion("action_wave", PetMotionTiming.WAVE, priority = 1)
        }
    }
    fun playPetAction(item: PetUnlock) {
        if (item.itemType != "ACTION") return
        val name = PetRules.rewards.firstOrNull { it.key == item.itemKey }?.name ?: "互动"
        playPetMotion(item.itemKey, PetMotionTiming.forAction(item.itemKey), priority = 1, caption = name)
    }
    fun setPetOnFocus(show: Boolean) = viewModelScope.launch { _message.value = repo.setPetOnFocus(show) }
    fun onPetPageVisible() = viewModelScope.launch {
        if (petState.value.named && petState.value.unlocks.any { it.itemKey == "action_wave" } && repo.consumeDailyPetWave()) {
            playPetMotion("action_wave", PetMotionTiming.WAVE, priority = 1, caption = "今天也一起加油")
        }
    }
    fun petTapped() {
        val now = SystemClock.elapsedRealtime()
        if (!petState.value.named || now - lastPetTapAt < 2_000 || _petMotion.value.priority > 0) return
        lastPetTapAt = now
        val reading = timer.value.running && petState.value.unlocks.any { it.itemKey == "action_read" }
        playPetMotion(if (reading) "action_pet_reading" else "action_pet", PetMotionTiming.TOUCH, priority = 1, caption = "摸摸收到啦")
    }
    fun recognizeFood(uri: Uri, sourceType: String) = viewModelScope.launch {
        if (_foodLoading.value) return@launch
        _foodLoading.value = true; _foodNotice.value = null
        _pendingFood.value?.photoPath?.let { repo.discardPendingFood(it) }
        runCatching { repo.prepareFood(uri, sourceType) }
            .onSuccess { _pendingFood.value = it; _foodNotice.value = it.fallbackReason ?: "识别完成，请确认食物" }
            .onFailure { _foodNotice.value = it.message ?: "图片处理失败" }
        _foodLoading.value = false
    }
    fun cancelPendingFood() = viewModelScope.launch {
        repo.discardPendingFood(_pendingFood.value?.photoPath); _pendingFood.value = null
    }
    fun confirmFood(selectedKey: String) = viewModelScope.launch {
        val pending = _pendingFood.value ?: return@launch
        val feedback = repo.confirmFood(pending, selectedKey)
        val definition = FoodCatalog.find(selectedKey)
        val unsafe = definition.unsafe
        _pendingFood.value = null
        _foodNotice.value = if (unsafe) "${definition.name}不适合小狗，它温柔地摇了摇头，照片已收藏" else "$feedback\n${definition.name}已经放进待喂食物"
        if (unsafe) playPetMotion("action_refuse", PetMotionTiming.REFUSE, priority = 2, caption = "这个不能吃哦")
    }
    fun feedPet(recordId: Long) = viewModelScope.launch {
        val record = foodRecords.value.firstOrNull { it.id == recordId }
        val result = repo.feedPet(recordId); _foodNotice.value = result
        if (result.startsWith("吃得")) playPetMotion(
            "action_eat",
            PetMotionTiming.EAT,
            priority = 3,
            caption = "好香呀！",
            foodPhotoPath = record?.photoPath,
            foodKey = record?.recognizedKey
        )
    }
    fun feedCinnamorollCottonCandy() = viewModelScope.launch {
        if (adminState.value.activeTheme != ThemeCatalog.CINNAMOROLL) {
            _foodNotice.value = "只有大耳狗主题可以送出这份棉花糖"; return@launch
        }
        val result = repo.feedCinnamorollCottonCandy()
        _foodNotice.value = result
        if (result.startsWith("棉花糖")) {
            _cottonCandyEffectToken.value = SystemClock.elapsedRealtime()
            playPetMotion("action_eat", PetMotionTiming.EAT, priority = 3, caption = "甜甜的，谢谢你！", foodKey = "cotton_candy")
        }
    }
    fun deleteFoodRecord(id: Long) = viewModelScope.launch { repo.deleteFoodRecord(id); _foodNotice.value = "食物记录已删除" }
    fun clearFoodNotice() { _foodNotice.value = null }
    fun saveFoodAiConfig(apiKey: String, baseUrl: String, model: String) = viewModelScope.launch {
        runCatching { repo.saveFoodAiConfig(apiKey, baseUrl, model) }
            .onSuccess { _foodAiConfig.value = repo.foodAiConfig(); _foodAiOverride.value = true; _message.value = "AI识别配置已加密保存" }
            .onFailure { _message.value = it.message ?: "保存失败" }
    }
    fun clearFoodAiConfig() = viewModelScope.launch {
        repo.clearFoodAiConfig(); _foodAiConfig.value = repo.foodAiConfig(); _foodAiOverride.value = false; _message.value = "已恢复项目默认AI配置"
    }
    fun testFoodAiConfig() = viewModelScope.launch {
        _message.value = runCatching { repo.testFoodAiConfig(); "通义千问连接成功，当前模型可用" }.getOrElse { it.message ?: "连接失败" }
    }
    private fun playPetMotion(actionKey: String, durationMillis: Long, priority: Int, caption: String? = null, foodPhotoPath: String? = null, foodKey: String? = null) {
        if (_petMotion.value.priority > priority) return
        petMotionJob?.cancel()
        val token = _petMotion.value.token + 1
        _petMotion.value = PetMotionUi(actionKey, token, caption, priority, foodPhotoPath, foodKey)
        petMotionJob = viewModelScope.launch {
            delay(durationMillis)
            if (_petMotion.value.token == token) _petMotion.value = PetMotionUi(token = token)
        }
    }
    fun clearMessage() { _message.value = null }
    fun dismissAchievementNotice() { _achievementQueue.update { it.drop(1) } }
    fun claimCode(value: String) = viewModelScope.launch { _message.value = repo.claimCode(value) }
    fun createCodes(custom: String?, count: Int, points: Int) = viewModelScope.launch {
        val created = repo.createCodes(custom, count, points)
        _message.value = if (created.isEmpty()) "兑换码已存在" else "已创建：\n${created.joinToString("\n")}"
    }
    fun toggleCode(item: RewardCode) = viewModelScope.launch { repo.toggleCode(item) }
    fun addProduct(name: String, description: String, cost: Int, permanent: Boolean) = viewModelScope.launch { repo.addProduct(name, description, cost, permanent) }
    fun toggleProduct(item: Product) = viewModelScope.launch { repo.toggleProduct(item) }
    fun addTask(title: String, description: String, points: Int, recurrence: String, weekdays: String, date: String?) =
        viewModelScope.launch { repo.addTaskTemplate(title, description, points, recurrence, weekdays, date) }
    fun toggleTaskTemplate(item: TaskTemplate) = viewModelScope.launch { repo.toggleTaskTemplate(item) }
    fun completeTask(id: Long) = viewModelScope.launch { _message.value = if (repo.completeTask(id)) "任务完成，积分已到账" else "只能完成今天尚未打卡的任务" }
    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch { repo.ensureTasks(date) }
    }
    fun saveReflection(note: String, summary: String, mood: Int) = viewModelScope.launch { repo.saveReflection(_selectedDate.value, note, summary, mood); _message.value = "每日总结已保存" }
    fun saveRule(minutes: Int, perBlock: Int, bonusMinutes: Int, bonusPoints: Int, capMinutes: Int) =
        viewModelScope.launch { repo.saveRule(minutes, perBlock, bonusMinutes, bonusPoints, capMinutes); _message.value = "新规则将在明天生效" }
    fun createCategory(name: String) = viewModelScope.launch {
        val category = repo.createOrUseCategory(name)
        _message.value = "已新增学习分类：${category.name}"
    }
    fun updateCategory(item: StudyCategory, name: String, active: Boolean) = viewModelScope.launch { repo.updateCategory(item, name, active) }
    fun deleteSession(id: Long) = viewModelScope.launch { repo.deleteSession(id); _message.value = "记录已删除并重新计算" }
    fun updateAchievementReward(item: AchievementDefinition, points: Int) = viewModelScope.launch { repo.updateAchievementReward(item, points) }
    fun addTestStudyDuration(date: String, startTime: String, hours: Int, minutes: Int, category: String) = viewModelScope.launch {
        _message.value = repo.addTestStudyDuration(date, startTime, hours, minutes, category)
    }

    fun configurePin(pin: String, finishOnboarding: Boolean = false) = viewModelScope.launch {
        runCatching {
            _recoveryCode.value = repo.setPin(pin)
            if (finishOnboarding) repo.completeOnboarding()
        }.onFailure { _message.value = "PIN 必须为6位数字" }
    }
    fun dismissRecoveryCode() { _recoveryCode.value = null }
    fun verifyPin(pin: String) = viewModelScope.launch {
        _adminUnlocked.value = repo.verifyPin(pin)
        if (!_adminUnlocked.value) _message.value = "PIN 不正确"
    }
    fun resetPin(recovery: String, newPin: String) = viewModelScope.launch {
        val code = repo.resetPinWithRecovery(recovery, newPin)
        if (code == null) _message.value = "恢复码不正确" else { _recoveryCode.value = code; _message.value = "PIN 已重设" }
    }
    fun lockAdmin() { _adminUnlocked.value = false }
    fun applyLauncherIconAfterLeavingApp() = LauncherIconManager.applyRememberedTheme(getApplication())
    fun completeOnboarding() = viewModelScope.launch { repo.completeOnboarding() }
    fun setTheme(key: String) = viewModelScope.launch {
        if (!repo.setTheme(key)) _message.value = "该主题尚未解锁"
    }
    fun claimConstellationReward(threshold: Int, title: String, points: Int) = viewModelScope.launch {
        if (!repo.claimConstellationReward(threshold, title, points)) _message.value = "奖励尚未解锁或已经领取"
    }
    fun setSeenStarCount(count: Int) = viewModelScope.launch { repo.setSeenStarCount(count) }
    fun resetProgress() = viewModelScope.launch { repo.resetProgress(); _message.value = "进度数据已清空" }
    fun resetConfiguration() = viewModelScope.launch { repo.resetConfiguration(); _message.value = "管理配置已恢复默认" }
    fun factoryReset() = viewModelScope.launch { repo.factoryReset(); _message.value = "已恢复出厂设置" }
    fun exportBackup(uri: Uri, password: String) = viewModelScope.launch {
        runCatching { repo.exportEncrypted(uri, password) }.onSuccess { _message.value = "加密备份已导出" }.onFailure { _message.value = it.message ?: "导出失败" }
    }
    fun importBackup(uri: Uri, password: String) = viewModelScope.launch {
        runCatching { repo.importEncrypted(uri, password) }.onSuccess { _message.value = "恢复成功，请重新打开 App 并设置 PIN" }.onFailure { _message.value = it.message ?: "恢复失败" }
    }
}
