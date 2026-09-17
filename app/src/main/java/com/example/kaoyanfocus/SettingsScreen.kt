package com.example.kaoyanfocus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kaoyanfocus.data.*

@Composable
fun SettingsScreen(vm: FocusViewModel, activeTheme: String) {
    val unlocked by vm.adminUnlocked.collectAsState(); val redemptions by vm.redemptions.collectAsState(); val admin by vm.adminState.collectAsState(); val unlocks by vm.unlocks.collectAsState()
    val petState by vm.petState.collectAsState()
    var code by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var forgot by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("奖励与管理", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                when (activeTheme) {
                    ThemeCatalog.CINNAMOROLL -> Image(painterResource(R.drawable.settings_character), null, Modifier.size(105.dp), contentScale = ContentScale.Fit)
                    ThemeCatalog.STARRY -> Image(painterResource(R.drawable.starry_character), null, Modifier.size(105.dp), contentScale = ContentScale.Fit)
                }
            }
            Text("兑换码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(code, { code = it.uppercase() }, Modifier.weight(1f), label = { Text("输入兑换码") }, singleLine = true)
                Spacer(Modifier.width(8.dp)); Button({ vm.claimCode(code); code = "" }, enabled = code.isNotBlank()) { Text("兑换") }
            }
            Spacer(Modifier.height(12.dp))
            Text("外观", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ThemeCatalog.owned(redemptions, unlocks).forEach { theme ->
                ListItem(
                    headlineContent = { Text(theme.name) },
                    supportingContent = { Text(theme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_theme_palette), null) },
                    trailingContent = { RadioButton(admin.activeTheme == theme.key, { vm.setTheme(theme.key) }) }
                )
            }
            petState.profile?.takeIf { petState.named }?.let { profile ->
                ListItem(
                    headlineContent = { Text("专注页显示宠物") },
                    supportingContent = { Text("关闭后恢复当前主题原角色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Default.Pets, null) },
                    trailingContent = { Switch(profile.showOnFocus, { vm.setPetOnFocus(it) }) }
                )
            }
            HorizontalDivider()
            Text("管理设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (unlocked) item { AdminPanel(vm) } else item {
            ElevatedCard { Column(Modifier.padding(16.dp)) {
                Text("需要管理 PIN", fontWeight = FontWeight.Bold)
                Text("积分规则、任务、商品、兑换码、分类、备份和清空均受保护。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("6位管理 PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Button({ vm.verifyPin(pin) }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth()) { Text("解锁管理") }
                TextButton({ forgot = true }, Modifier.align(Alignment.End)) { Text("忘记 PIN") }
            } }
        }
    }
    if (forgot) RecoveryDialog(vm) { forgot = false }
}

@Composable
private fun RecoveryDialog(vm: FocusViewModel, dismiss: () -> Unit) {
    var recovery by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("使用恢复码重设 PIN") }, text = { Column {
        OutlinedTextField(recovery, { recovery = it.uppercase() }, label = { Text("恢复码") })
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("新的6位 PIN") }, visualTransformation = PasswordVisualTransformation())
    } }, dismissButton = { TextButton(dismiss) { Text("取消") } }, confirmButton = { Button({ vm.resetPin(recovery, pin); dismiss() }, enabled = recovery.isNotBlank() && pin.length == 6) { Text("重设") } })
}

@Composable
private fun AdminPanel(vm: FocusViewModel) {
    var section by remember { mutableIntStateOf(0) }
    val sections = listOf("规则", "任务", "商品", "兑换码", "分类", "成就", "测试", "AI识别", "数据")
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sections.size) { i ->
                val label = sections[i]
                FilterChip(section == i, { section = i }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(10.dp))
        when (section) {
            0 -> RuleAdmin(vm)
            1 -> TaskAdmin(vm)
            2 -> ProductAdmin(vm)
            3 -> CodeAdmin(vm)
            4 -> CategoryAdmin(vm)
            5 -> AchievementAdmin(vm)
            6 -> TestDataAdmin(vm)
            7 -> FoodAiAdmin(vm)
            else -> DataAdmin(vm)
        }
    }
}

@Composable
private fun FoodAiAdmin(vm: FocusViewModel) {
    val config by vm.foodAiConfig.collectAsState()
    val hasOverride by vm.foodAiOverride.collectAsState()
    val todayCount by vm.foodRecognitionCount.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var model by remember(config.model) { mutableStateOf(config.model) }
    ElevatedCard { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("通义千问食物识别", fontWeight = FontWeight.Bold)
        Text(
            "当前：${if (config.configured) "已配置" else "未配置"} · ${if (hasOverride) "使用本机加密配置" else "使用项目默认配置"} · 今日 $todayCount/10 次",
            style = MaterialTheme.typography.bodySmall,
            color = if (config.configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        OutlinedTextField(apiKey, { apiKey = it.trim() }, Modifier.fillMaxWidth(), label = { Text("新的API Key（保存后不再显示）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        OutlinedTextField(baseUrl, { baseUrl = it.trim() }, Modifier.fillMaxWidth(), label = { Text("兼容接口地址") }, singleLine = true)
        OutlinedTextField(model, { model = it.trim() }, Modifier.fillMaxWidth(), label = { Text("视觉模型") }, singleLine = true)
        Button({ vm.saveFoodAiConfig(apiKey, baseUrl, model); apiKey = "" }, Modifier.fillMaxWidth(), enabled = apiKey.isNotBlank() && baseUrl.startsWith("https://") && model.isNotBlank()) { Text("加密保存本机配置") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(vm::testFoodAiConfig, Modifier.weight(1f), enabled = config.configured) { Text("测试连接") }
            OutlinedButton(vm::clearFoodAiConfig, Modifier.weight(1f), enabled = hasOverride) { Text("恢复项目默认") }
        }
        Text("未配置时仍可手动选择食物。项目默认密钥可写为 QWEN_API_KEY 或 DASHSCOPE_API_KEY；连接测试会真实验证当前模型。密钥不会写入数据库、日志或备份。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }
}

@Composable
private fun RuleAdmin(vm: FocusViewModel) {
    val rule by vm.rule.collectAsState()
    var minutes by remember(rule) { mutableStateOf((rule?.minutesPerPoint ?: 60).toString()) }
    var perBlock by remember(rule) { mutableStateOf((rule?.pointsPerBlock ?: 1).toString()) }
    var bonusMinutes by remember(rule) { mutableStateOf((rule?.bonusMinutes ?: 480).toString()) }
    var bonus by remember(rule) { mutableStateOf((rule?.bonusPoints ?: 5).toString()) }
    var cap by remember(rule) { mutableStateOf((rule?.capMinutes ?: 720).toString()) }
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("学习积分规则", fontWeight = FontWeight.Bold); Text("保存后次日00:00生效。", style = MaterialTheme.typography.bodySmall)
        NumberField(minutes, { minutes = it }, "每个积分块的分钟数")
        NumberField(perBlock, { perBlock = it }, "每块获得积分")
        NumberField(bonusMinutes, { bonusMinutes = it }, "额外奖励门槛（分钟）")
        NumberField(bonus, { bonus = it }, "额外奖励积分")
        NumberField(cap, { cap = it }, "基础积分封顶（分钟）")
        Button({ vm.saveRule(minutes.toIntOrNull() ?: 60, perBlock.toIntOrNull() ?: 1, bonusMinutes.toIntOrNull() ?: 480, bonus.toIntOrNull() ?: 5, cap.toIntOrNull() ?: 720) }, Modifier.fillMaxWidth()) { Text("保存新版本") }
    } }
}

@Composable private fun NumberField(value: String, change: (String) -> Unit, label: String) =
    OutlinedTextField(value, { change(it.filter(Char::isDigit)) }, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)

@Composable
private fun TaskAdmin(vm: FocusViewModel) {
    val templates by vm.templates.collectAsState()
    var title by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; var points by remember { mutableStateOf("1") }; var recurrence by remember { mutableStateOf("DAILY") }; var weekdays by remember { mutableStateOf("1,2,3,4,5,6,7") }; var date by remember { mutableStateOf(Dates.today()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("新增任务模板", fontWeight = FontWeight.Bold)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("任务名称") })
            OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text("说明（可选）") })
            NumberField(points, { points = it }, "完成积分")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(recurrence == "DAILY", { recurrence = "DAILY" }, label = { Text("每天") })
                FilterChip(recurrence == "WEEKLY", { recurrence = "WEEKLY" }, label = { Text("选星期") })
                FilterChip(recurrence == "ONE_OFF", { recurrence = "ONE_OFF" }, label = { Text("指定日期") })
            }
            if (recurrence == "WEEKLY") OutlinedTextField(weekdays, { weekdays = it.filter { c -> c.isDigit() || c == ',' } }, Modifier.fillMaxWidth(), label = { Text("星期：1,2,3…7") })
            if (recurrence == "ONE_OFF") OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("日期 YYYY-MM-DD") })
            Button({ vm.addTask(title, desc, points.toIntOrNull() ?: 0, recurrence, weekdays, date.takeIf { recurrence == "ONE_OFF" }); title = ""; desc = "" }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("添加任务") }
        } }
        templates.forEach { item -> ListItem(headlineContent = { Text(item.title) }, supportingContent = { Text("${item.points}分 · ${if (item.active) "启用" else "停用"}") }, trailingContent = { TextButton({ vm.toggleTaskTemplate(item) }) { Text(if (item.active) "停用" else "启用") } }) }
    }
}

@Composable
private fun ProductAdmin(vm: FocusViewModel) {
    val products by vm.allProducts.collectAsState(); var name by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; var cost by remember { mutableStateOf("") }; var permanent by remember { mutableStateOf(false) }
    Column {
        ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("新增商品", fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("商品名称") })
            OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text("兑换后的权益") })
            NumberField(cost, { cost = it }, "积分价格")
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(permanent, { permanent = it }); Text("永久一次性权益") }
            Button({ vm.addProduct(name, desc, cost.toIntOrNull() ?: 1, permanent); name = ""; desc = ""; cost = "" }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("添加商品") }
        } }
        products.forEach { p -> ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${p.cost}分 · ${if (p.active) "已上架" else "已下架"}") }, trailingContent = { TextButton({ vm.toggleProduct(p) }) { Text(if (p.active) "下架" else "上架") } }) }
    }
}

@Composable
private fun CodeAdmin(vm: FocusViewModel) {
    val codes by vm.codes.collectAsState(); var custom by remember { mutableStateOf("") }; var count by remember { mutableStateOf("1") }; var points by remember { mutableStateOf("1") }
    Column {
        ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("创建兑换码", fontWeight = FontWeight.Bold)
            OutlinedTextField(custom, { custom = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text("自定义码（留空则随机）") })
            NumberField(count, { count = it }, "随机生成数量（1–50）"); NumberField(points, { points = it }, "每个码的积分")
            Button({ vm.createCodes(custom.ifBlank { null }, count.toIntOrNull() ?: 1, points.toIntOrNull() ?: 1); custom = "" }, Modifier.fillMaxWidth()) { Text("生成兑换码") }
        } }
        codes.take(30).forEach { c -> ListItem(headlineContent = { Text(c.displayCode) }, supportingContent = { Text("${c.points}分 · " + if (c.usedAt != null) "已使用" else if (c.active) "可用" else "已停用") }, trailingContent = { if (c.usedAt == null) TextButton({ vm.toggleCode(c) }) { Text(if (c.active) "停用" else "启用") } }) }
    }
}

@Composable
private fun CategoryAdmin(vm: FocusViewModel) {
    val categories by vm.allCategories.collectAsState()
    Column { categories.forEach { item -> var name by remember(item.id, item.name) { mutableStateOf(item.name) }; ElevatedCard(Modifier.padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("分类名称") }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ vm.updateCategory(item, name, !item.active) }) { Text(if (item.active) "停用" else "启用") }; Button({ vm.updateCategory(item, name, item.active) }) { Text("保存") } }
    } } } }
}

@Composable
private fun AchievementAdmin(vm: FocusViewModel) {
    val definitions by vm.achievements.collectAsState(); val unlocks by vm.unlocks.collectAsState(); val unlocked = unlocks.map { it.achievementKey }.toSet()
    Column { definitions.forEach { item -> var reward by remember(item.key, item.rewardPoints) { mutableStateOf(item.rewardPoints.toString()) }; ListItem(
        headlineContent = { Text(item.name) }, supportingContent = { Text(if (item.key in unlocked) "已解锁，奖励已固定" else "尚未解锁") },
        trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(reward, { reward = it.filter(Char::isDigit) }, Modifier.width(80.dp), label = { Text("积分") }, enabled = item.key !in unlocked); IconButton({ vm.updateAchievementReward(item, reward.toIntOrNull() ?: 0) }, enabled = item.key !in unlocked) { Icon(Icons.Default.Save, null) } } }
    ) } }
}

@Composable
private fun TestDataAdmin(vm: FocusViewModel) {
    var date by remember { mutableStateOf(Dates.today()) }
    var startTime by remember { mutableStateOf("12:00") }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("测试学习") }
    var confirm by remember { mutableStateOf(false) }
    val totalMinutes = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("补录学习时长（测试）", fontWeight = FontWeight.Bold)
        Text("会按所选开始时间和当前主题真实写入记录，可测试早起鸟、夜猫子、单日8小时来信、星星和累计时长成就。", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(date, { date = it.take(10) }, Modifier.fillMaxWidth(), label = { Text("日期（YYYY-MM-DD）") }, singleLine = true)
        OutlinedTextField(startTime, { startTime = it.filter { ch -> ch.isDigit() || ch == ':' }.take(5) }, Modifier.fillMaxWidth(), label = { Text("开始时间（HH:mm）") }, singleLine = true)
        OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("学习分类") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), label = { Text("小时") }, singleLine = true)
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("分钟") }, singleLine = true)
        }
        Button({ confirm = true }, Modifier.fillMaxWidth(), enabled = totalMinutes > 0) { Text("补录并触发检查") }
    } }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("确认写入测试记录？") },
        text = { Text("将在 $date $startTime 补录 ${hours.ifBlank { "0" }}小时${minutes.ifBlank { "0" }}分钟，并记录当前主题。该记录会影响积分、统计、星星、来信和成就，但可以之后在日历中删除。") },
        dismissButton = { TextButton({ confirm = false }) { Text("取消") } },
        confirmButton = { Button({ vm.addTestStudyDuration(date, startTime, hours.toIntOrNull() ?: 0, minutes.toIntOrNull() ?: 0, category); confirm = false }) { Text("确认补录") } }
    )
}

@Composable
private fun DataAdmin(vm: FocusViewModel) {
    var password by remember { mutableStateOf("") }; var resetChoice by remember { mutableStateOf<Int?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if (uri != null) vm.exportBackup(uri, password) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.importBackup(uri, password) }
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("加密备份", fontWeight = FontWeight.Bold)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("独立备份密码（至少8位）") }, visualTransformation = PasswordVisualTransformation())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ exportLauncher.launch("研途相伴-${Dates.today()}.ytb") }, enabled = password.length >= 8, modifier = Modifier.weight(1f)) { Text("导出") }
            OutlinedButton({ importLauncher.launch(arrayOf("*/*")) }, enabled = password.length >= 8, modifier = Modifier.weight(1f)) { Text("导入") }
        }
        HorizontalDivider(); Text("数据清空", fontWeight = FontWeight.Bold)
        OutlinedButton({ resetChoice = 0 }, Modifier.fillMaxWidth()) { Text("清空进度数据") }
        OutlinedButton({ resetChoice = 1 }, Modifier.fillMaxWidth()) { Text("恢复默认配置") }
        Button({ resetChoice = 2 }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("恢复出厂设置") }
    } }
    resetChoice?.let { choice -> AlertDialog({ resetChoice = null }, title = { Text("确认清空？") }, text = { Text(when (choice) { 0 -> "将清除学习、总结、任务完成、积分、兑换和成就。"; 1 -> "将恢复分类、规则、任务模板、商品和兑换码默认配置。"; else -> "将删除全部数据、主题和 PIN，且无法撤销。" }) }, dismissButton = { TextButton({ resetChoice = null }) { Text("取消") } }, confirmButton = { Button({ when (choice) { 0 -> vm.resetProgress(); 1 -> vm.resetConfiguration(); else -> vm.factoryReset() }; resetChoice = null }) { Text("确认执行") } }) }
}
