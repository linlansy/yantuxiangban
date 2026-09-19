@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kaoyanfocus

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaoyanfocus.data.*
import androidx.core.content.FileProvider
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val CinnamorollCloudNotes = listOf(
    "今天的认真，会变成明天的底气。",
    "先把这一小段学完，就已经很棒了。",
    "云朵会记住你安静努力的样子。",
    "慢慢来，我们一起把目标走近。",
    "别忘了喝水，也别忘了相信自己。"
)

@Composable
fun FocusScreen(vm: FocusViewModel, activeTheme: String) {
    val timer by vm.timer.collectAsState(); val categories by vm.categories.collectAsState(); val balance by vm.balance.collectAsState()
    val encouragement by vm.encouragement.collectAsState()
    val petState by vm.petState.collectAsState(); val petMotion by vm.petMotion.collectAsState()
    var category by remember { mutableStateOf("学习") }; var note by remember { mutableStateOf("") }; var confirmEnd by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var finishResult by remember { mutableStateOf<String?>(null) }
    var cinnamorollCardIndex by remember { mutableIntStateOf((LocalDate.now().toEpochDay() % CinnamorollCloudNotes.size).toInt()) }
    LaunchedEffect(timer.active, timer.categoryName, timer.note) { if (timer.active) { category = timer.categoryName; note = timer.note } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 700.dp
        val isStarry = activeTheme == ThemeCatalog.STARRY
        val starryGold = Color(0xFFFFD66B)
        val starrySoftGold = Color(0xFFFFE9A8)
        val fieldColors = if (isStarry) OutlinedTextFieldDefaults.colors(
            focusedBorderColor = starryGold,
            unfocusedBorderColor = starrySoftGold,
            focusedLabelColor = starryGold,
            unfocusedLabelColor = starrySoftGold
        ) else OutlinedTextFieldDefaults.colors()
        val characterHeight = if (compact) 128.dp else 158.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("研途相伴", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(if (timer.active) "正在学习 · ${timer.categoryName}" else encouragement, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(
                    onClick = {},
                    label = { Text("$balance 分") },
                    leadingIcon = { Icon(Icons.Default.Star, null) },
                    colors = if (isStarry) AssistChipDefaults.assistChipColors(labelColor = starryGold, leadingIconContentColor = starryGold) else AssistChipDefaults.assistChipColors(),
                    border = if (isStarry) BorderStroke(1.dp, starryGold) else AssistChipDefaults.assistChipBorder(enabled = true)
                )
            }
            val character = when (activeTheme) {
                ThemeCatalog.CINNAMOROLL -> R.drawable.focus_character
                ThemeCatalog.STARRY -> R.drawable.starry_character
                else -> null
            }
            val focusPet = petState.profile?.takeIf { petState.named && it.showOnFocus }
            if (focusPet != null || character != null) Box(Modifier.fillMaxWidth().height(characterHeight), contentAlignment = Alignment.Center) {
                // Theme-only decoration: it is visible on the focus home page even before
                // the separately collectible room furniture has been unlocked.
                if (activeTheme == ThemeCatalog.CINNAMOROLL) {
                    Image(
                        painter = painterResource(R.drawable.furniture_cloud_cushion),
                        contentDescription = "云朵坐垫",
                        modifier = Modifier.align(Alignment.BottomCenter).size(if (compact) 112.dp else 132.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (focusPet != null) AnimatedPet(focusPet, petState.unlocks, petMotion, timer.running, Modifier.fillMaxSize(), vm::petTapped)
                else Image(painterResource(character!!), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                if (activeTheme == ThemeCatalog.CINNAMOROLL) Surface(
                    Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 2.dp).clickable {
                        cinnamorollCardIndex = (cinnamorollCardIndex + 1) % CinnamorollCloudNotes.size
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = .88f),
                    shadowElevation = 2.dp
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text("☁ 云朵加油站 · 点我换一句", color = Color(0xFF4E8FD1), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(CinnamorollCloudNotes[cinnamorollCardIndex], color = Color(0xFF356FA8), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            else Box(Modifier.fillMaxWidth().height(if (compact) 54.dp else 72.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoStories, null, Modifier.size(if (compact) 46.dp else 60.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
            }
            ElevatedCard(Modifier.fillMaxWidth().then(if (isStarry) Modifier.border(1.5.dp, starryGold, MaterialTheme.shapes.medium) else Modifier)) {
                Column(Modifier.fillMaxWidth().padding(vertical = if (compact) 14.dp else 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatDuration(timer.elapsedSeconds), style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(if (timer.running) "学习进行中" else if (timer.active) "已暂停 · 共暂停 ${timer.pauseCount} 次" else "准备好后开始计时", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { if (!timer.running) categoryMenuExpanded = !categoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !timer.running),
                        label = { Text("学习分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryMenuExpanded) },
                        enabled = !timer.running,
                        readOnly = true,
                        singleLine = true,
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                leadingIcon = { if (category == item.name) Icon(Icons.Default.Check, null) },
                                onClick = { category = item.name; categoryMenuExpanded = false }
                            )
                        }
                        if (categories.isNotEmpty()) HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("新增分类") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = { categoryMenuExpanded = false; newCategoryName = ""; showNewCategory = true }
                        )
                    }
                }
                OutlinedTextField(note, { if (!timer.running) note = it }, Modifier.fillMaxWidth(), label = { Text("本次备注（可选）") }, enabled = !timer.running, singleLine = compact, maxLines = if (compact) 1 else 2, colors = fieldColors)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        !timer.active -> Button({ vm.start(category, note) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Text("开始学习") }
                        timer.running -> { OutlinedButton(vm::pause, Modifier.weight(1f), border = if (isStarry) BorderStroke(1.5.dp, starryGold) else ButtonDefaults.outlinedButtonBorder(enabled = true), colors = if (isStarry) ButtonDefaults.outlinedButtonColors(contentColor = starryGold) else ButtonDefaults.outlinedButtonColors()) { Icon(Icons.Default.Pause, null); Text("暂停") }; Button({ confirmEnd = true }, Modifier.weight(1f)) { Icon(Icons.Default.Stop, null); Text("结束") } }
                        else -> { OutlinedButton({ vm.updateTimerContent(category, note); vm.resume() }, Modifier.weight(1f), border = if (isStarry) BorderStroke(1.5.dp, starryGold) else ButtonDefaults.outlinedButtonBorder(enabled = true), colors = if (isStarry) ButtonDefaults.outlinedButtonColors(contentColor = starryGold) else ButtonDefaults.outlinedButtonColors()) { Icon(Icons.Default.PlayArrow, null); Text("继续") }; Button({ vm.updateTimerContent(category, note); confirmEnd = true }, Modifier.weight(1f)) { Icon(Icons.Default.Stop, null); Text("结束") } }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("不足1分钟自动丢弃；锁屏和后台仍继续计时。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
    if (showNewCategory) AlertDialog(
        onDismissRequest = { showNewCategory = false },
        title = { Text("新增学习分类") },
        text = {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { if (it.length <= 20) newCategoryName = it },
                label = { Text("分类名称") },
                supportingText = { Text("最多20个字符") },
                singleLine = true
            )
        },
        dismissButton = { TextButton({ showNewCategory = false }) { Text("取消") } },
        confirmButton = {
            Button(
                onClick = {
                    category = newCategoryName.trim()
                    vm.createCategory(category)
                    showNewCategory = false
                },
                enabled = newCategoryName.isNotBlank()
            ) { Text("新增并选择") }
        }
    )
    if (confirmEnd) AlertDialog(onDismissRequest = { confirmEnd = false }, title = { Text("结束本次学习？") },
        text = { Text("分类：$category\n有效时长：${formatDuration(timer.elapsedSeconds)}\n结束后按当天累计时长结算积分。") },
        dismissButton = { TextButton({ confirmEnd = false }) { Text("继续学习") } },
        confirmButton = { Button({ confirmEnd = false; vm.finish { points, discarded -> finishResult = if (discarded) "不足1分钟，本次记录已丢弃。\n\n$encouragement" else "学习记录已保存，本次结算 ${if (points >= 0) "+$points" else points} 分。\n\n$encouragement" } }) { Text("确认结束") } })
    finishResult?.let { AlertDialog(onDismissRequest = { finishResult = null }, title = { Text("本次学习已结束") }, text = { Text(it) }, confirmButton = { Button({ finishResult = null }) { Text("收下进步") } }) }
}

@Composable
fun ShopScreen(vm: FocusViewModel, activeTheme: String) {
    val products by vm.products.collectAsState(); val balance by vm.balance.collectAsState(); val redemptions by vm.redemptions.collectAsState(); val admin by vm.adminState.collectAsState(); val unlocks by vm.unlocks.collectAsState()
    var tab by remember { mutableIntStateOf(0) }; var redeemTarget by remember { mutableStateOf<Product?>(null) }; var useTarget by remember { mutableStateOf<Redemption?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("积分商城", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("可用积分：$balance", color = MaterialTheme.colorScheme.primary) }
            when (activeTheme) {
                ThemeCatalog.CINNAMOROLL -> Image(painterResource(R.drawable.shop_character), null, Modifier.size(110.dp), contentScale = ContentScale.Fit)
                ThemeCatalog.STARRY -> Image(painterResource(R.drawable.starry_character), null, Modifier.size(110.dp), contentScale = ContentScale.Fit)
            }
        }
        PrimaryTabRow(tab) { Tab(tab == 0, { tab = 0 }, text = { Text("商品") }); Tab(tab == 1, { tab = 1 }, text = { Text("我的权益") }) }
        Spacer(Modifier.height(12.dp))
        if (tab == 0) LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products) { p -> ElevatedCard { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (p.themeKey != null) Icons.Default.Palette else Icons.Default.Redeem, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text(p.description, style = MaterialTheme.typography.bodySmall); Text(if (p.productType == "PERMANENT") "永久权益" else "可重复兑换券", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
                Button({ redeemTarget = p }, enabled = balance >= p.cost) { Text("${p.cost}分") }
            } } }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val ownedThemes = ThemeCatalog.owned(redemptions, unlocks)
            item { ElevatedCard { Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, null); Spacer(Modifier.width(12.dp))
                    Column { Text("已获得主题", fontWeight = FontWeight.Bold); Text("当前：${ThemeCatalog.nameOf(admin.activeTheme)}", style = MaterialTheme.typography.bodySmall) }
                }
                ownedThemes.forEach { theme ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = admin.activeTheme == theme.key, onClick = { vm.setTheme(theme.key) })
                        Column(Modifier.weight(1f)) { Text(theme.name); Text(theme.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            } } }
            items(redemptions) { r -> ElevatedCard { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.productName, fontWeight = FontWeight.Bold)
                    Text(r.entitlement.ifBlank { "已兑换权益" }, style = MaterialTheme.typography.bodySmall)
                    Text("获得时间：${formatMoment(r.redeemedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(when {
                        r.productType == "PERMANENT" -> "状态：永久有效"
                        r.usedAt == null -> "状态：未使用"
                        else -> "使用时间：${formatMoment(r.usedAt)}"
                    }, color = if (r.usedAt == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                if (r.productType == "CONSUMABLE" && r.usedAt == null && r.productName != "补签卡") OutlinedButton({ useTarget = r }) { Text("核销") }
                if (r.productName == "补签卡" && r.usedAt == null) Text("请到统计 → 日历使用", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            } } }
        }
    }
    redeemTarget?.let { p -> AlertDialog({ redeemTarget = null }, title = { Text("确认兑换") }, text = { Text("使用 ${p.cost} 积分兑换“${p.name}”？") }, dismissButton = { TextButton({ redeemTarget = null }) { Text("取消") } }, confirmButton = { Button({ vm.redeem(p); redeemTarget = null }) { Text("兑换") } }) }
    useTarget?.let { r -> AlertDialog({ useTarget = null }, title = { Text("确认使用权益") }, text = { Text("“${r.productName}”核销后不可恢复。") }, dismissButton = { TextButton({ useTarget = null }) { Text("取消") } }, confirmButton = { Button({ vm.useEntitlement(r.id); useTarget = null }) { Text("确认使用") } }) }
}

@Composable
fun StatsScreen(vm: FocusViewModel, activeTheme: String) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("学习成长", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("记录每一点进步", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (activeTheme) {
                ThemeCatalog.CINNAMOROLL -> Image(painterResource(if (tab == 3) R.drawable.achievement_character else R.drawable.stats_character), null, Modifier.size(100.dp), contentScale = ContentScale.Fit)
                ThemeCatalog.STARRY -> Image(painterResource(R.drawable.starry_character), null, Modifier.size(100.dp), contentScale = ContentScale.Fit)
            }
        }
        PrimaryTabRow(tab) { listOf("概览", "任务", "日历", "收藏", "宠物").forEachIndexed { i, name -> Tab(tab == i, { tab = i }, text = { Text(name) }) } }
        when (tab) { 0 -> OverviewSection(vm); 1 -> TasksSection(vm); 2 -> CalendarSection(vm); 3 -> CollectionSection(vm, activeTheme); else -> PetSection(vm) }
    }
}

@Composable
private fun CollectionSection(vm: FocusViewModel, activeTheme: String) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(tab) {
            listOf("成就", "星空").forEachIndexed { index, title ->
                Tab(tab == index, { tab = index }, text = { Text(title) })
            }
        }
        if (tab == 0) AchievementSection(vm) else StarsSection(vm, activeTheme)
    }
}

private enum class OverviewPeriod { DAY, WEEK, MONTH }

private val CategoryChartColors = listOf(
    Color(0xFF5B8C72), Color(0xFF7C76B8), Color(0xFFE1A85B), Color(0xFF5D91C8),
    Color(0xFFD97878), Color(0xFF70AAA2), Color(0xFFB77DA8), Color(0xFF9A8B70)
)

@Composable
private fun OverviewSection(vm: FocusViewModel) {
    val sessions by vm.sessions.collectAsState(); val ledger by vm.ledger.collectAsState(); val balance by vm.balance.collectAsState()
    var period by remember { mutableStateOf(OverviewPeriod.WEEK) }
    var ledgerPage by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val from = when (period) {
        OverviewPeriod.DAY -> today
        OverviewPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        OverviewPeriod.MONTH -> today.withDayOfMonth(1)
    }
    val filtered = sessions.filter {
        val date = Dates.localDate(it.startDate.ifBlank { Dates.dateOf(it.startedAt) })
        !date.isBefore(from) && !date.isAfter(today)
    }
    val seconds = filtered.sumOf { sessionSeconds(it) }
    val byCategory = filtered.groupBy { it.categoryName }.mapValues { e -> e.value.sumOf(::sessionSeconds) }.toList().sortedByDescending { it.second }
    val periodDays = generateSequence(from) { if (it < today) it.plusDays(1) else null }.toList()
    val byDay = periodDays.map { date -> date to filtered.filter { it.startDate.ifBlank { Dates.dateOf(it.startedAt) } == date.toString() }.sumOf(::sessionSeconds) }
    val maxDaySeconds = byDay.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val periodLedger = ledger.filter {
        runCatching {
            val date = Dates.localDate(it.sourceDate.ifBlank { Dates.dateOf(it.createdAt) })
            !date.isBefore(from) && !date.isAfter(today)
        }.getOrDefault(false)
    }
    val ledgerPageCount = maxOf(1, (periodLedger.size + 4) / 5)
    val safeLedgerPage = ledgerPage.coerceIn(0, ledgerPageCount - 1)
    val visibleLedger = periodLedger.drop(safeLedgerPage * 5).take(5)
    LaunchedEffect(period, ledgerPageCount) { ledgerPage = ledgerPage.coerceIn(0, ledgerPageCount - 1) }
    val earned = periodLedger.filter { it.amount > 0 }.sumOf { it.amount }; val spent = -periodLedger.filter { it.amount < 0 }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(period == OverviewPeriod.DAY, { period = OverviewPeriod.DAY }, label = { Text("本日") })
                FilterChip(period == OverviewPeriod.WEEK, { period = OverviewPeriod.WEEK }, label = { Text("本周") })
                FilterChip(period == OverviewPeriod.MONTH, { period = OverviewPeriod.MONTH }, label = { Text("本月") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("学习", formatShort(seconds), Modifier.weight(1f)); StatCard("余额", "$balance 分", Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("获得", "+$earned", Modifier.weight(1f)); StatCard("消费", "-$spent", Modifier.weight(1f)); StatCard("净变化", (earned - spent).toString(), Modifier.weight(1f)) }
            Spacer(Modifier.height(14.dp))
            Text("每日趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            byDay.forEach { (date, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${date.monthValue}/${date.dayOfMonth}", Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator({ value.toFloat() / maxDaySeconds }, Modifier.weight(1f))
                    Text(formatShort(value), Modifier.width(58.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("分类分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            CategoryPieChart(byCategory, seconds)
            Spacer(Modifier.height(10.dp))
        }
        itemsIndexed(byCategory) { index, pair -> Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(CategoryChartColors[index % CategoryChartColors.size], MaterialTheme.shapes.extraSmall))
                Spacer(Modifier.width(8.dp))
                Text(pair.first, Modifier.weight(1f)); Text(formatShort(pair.second))
            }
            LinearProgressIndicator(
                progress = { if (seconds == 0L) 0f else pair.second.toFloat() / seconds },
                modifier = Modifier.fillMaxWidth(),
                color = CategoryChartColors[index % CategoryChartColors.size]
            )
        } }
        item { Text("近期积分流水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(visibleLedger) { e ->
            val date = e.sourceDate.ifBlank { Dates.dateOf(e.createdAt) }
            ListItem(headlineContent = { Text(e.note) }, supportingContent = { Text("${e.type} · $date") }, trailingContent = { Text(if (e.amount > 0) "+${e.amount}" else e.amount.toString(), color = if (e.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) })
            HorizontalDivider()
        }
        if (periodLedger.isEmpty()) item { Text("当前时间段还没有积分流水。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (periodLedger.size > 5) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ ledgerPage = (safeLedgerPage - 1).coerceAtLeast(0) }, enabled = safeLedgerPage > 0) { Icon(Icons.Default.ChevronLeft, "上一页") }
                Text("第 ${safeLedgerPage + 1} / $ledgerPageCount 页", style = MaterialTheme.typography.labelMedium)
                IconButton({ ledgerPage = (safeLedgerPage + 1).coerceAtMost(ledgerPageCount - 1) }, enabled = safeLedgerPage < ledgerPageCount - 1) { Icon(Icons.Default.ChevronRight, "下一页") }
            }
        }
    }
}

@Composable
private fun CategoryPieChart(data: List<Pair<String, Long>>, total: Long) {
    if (total <= 0L || data.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().height(128.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("本时间段还没有学习分类数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Canvas(Modifier.size(150.dp)) {
            var startAngle = -90f
            data.forEachIndexed { index, pair ->
                val sweep = pair.second.toFloat() / total * 360f
                drawArc(
                    color = CategoryChartColors[index % CategoryChartColors.size],
                    startAngle = startAngle + .7f,
                    sweepAngle = (sweep - 1.4f).coerceAtLeast(.3f),
                    useCenter = true,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                )
                startAngle += sweep
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            data.take(6).forEachIndexed { index, pair ->
                val percent = (pair.second.toFloat() / total * 100f).roundToInt()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(CategoryChartColors[index % CategoryChartColors.size], MaterialTheme.shapes.extraSmall))
                    Spacer(Modifier.width(7.dp))
                    Text(pair.first, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    Text("$percent%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (data.size > 6) Text("其余 ${data.size - 6} 个分类见下方明细", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TasksSection(vm: FocusViewModel) {
    val tasks by vm.selectedTasks.collectAsState(); val selected by vm.selectedDate.collectAsState(); var confirm by remember { mutableStateOf<DailyTask?>(null) }
    LaunchedEffect(Unit) { vm.selectDate(Dates.today()) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("今日任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("$selected · 完成 " + tasks.count { it.completedAt != null } + "/" + tasks.size) }
        items(tasks) { task -> ElevatedCard(Modifier.fillMaxWidth().clickable(enabled = task.completedAt == null) { confirm = task }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (task.completedAt == null) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle, null, tint = if (task.completedAt == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.title, fontWeight = FontWeight.Bold); if (task.description.isNotBlank()) Text(task.description, style = MaterialTheme.typography.bodySmall) }; Text("+${task.points}")
        } } }
        if (tasks.isEmpty()) item { Text("今天还没有任务，可在设置 → 管理中添加。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    confirm?.let { task -> AlertDialog({ confirm = null }, title = { Text("确认完成任务？") }, text = { Text("完成后立即获得 ${task.points} 积分，并且不能撤销。") }, dismissButton = { TextButton({ confirm = null }) { Text("取消") } }, confirmButton = { Button({ vm.completeTask(task.id); confirm = null }) { Text("确认完成") } }) }
}

@Composable
private fun CalendarSection(vm: FocusViewModel) {
    var month by remember { mutableStateOf(YearMonth.now()) }; val selected by vm.selectedDate.collectAsState(); val sessions by vm.sessions.collectAsState(); val ledger by vm.ledger.collectAsState(); val tasks by vm.selectedTasks.collectAsState(); val reflection by vm.selectedReflection.collectAsState(); val redemptions by vm.redemptions.collectAsState(); val restDays by vm.restDays.collectAsState()
    var note by remember { mutableStateOf("") }; var summary by remember { mutableStateOf("") }; var mood by remember { mutableIntStateOf(3) }
    var deleteTarget by remember { mutableStateOf<StudySession?>(null) }; var confirmMakeup by remember { mutableStateOf(false) }; var confirmWeekly by remember { mutableStateOf(false) }
    LaunchedEffect(reflection, selected) { note = reflection?.note ?: ""; summary = reflection?.summary ?: ""; mood = reflection?.mood ?: 3 }
    val days = List(month.atDay(1).dayOfWeek.value - 1) { 0 } + (1..month.lengthOfMonth()).toList()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, null) }
                Text(month.year.toString() + "年" + month.monthValue + "月", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton({ month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, null) }
            }
            Row(Modifier.fillMaxWidth()) { listOf("一","二","三","四","五","六","日").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
            LazyVerticalGrid(GridCells.Fixed(7), Modifier.fillMaxWidth().height((((days.size + 6) / 7) * 52).dp), userScrollEnabled = false) {
                items(days.size) { index -> val day = days[index]; if (day == 0) Spacer(Modifier.size(44.dp)) else {
                    val date = month.atDay(day).toString(); val hasStudy = sessions.any { it.startDate.ifBlank { Dates.dateOf(it.startedAt) } == date }; val hasRest = restDays.any { it.date == date }
                    Surface(onClick = { vm.selectDate(date) }, color = if (selected == date) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(2.dp)) {
                        Column(Modifier.height(42.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(day.toString()); if (hasStudy) Text("•", color = MaterialTheme.colorScheme.primary) else if (hasRest) Text("☾", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, lineHeight = 10.sp) }
                    }
                } }
            }
            Spacer(Modifier.height(12.dp))
            val daySessions = sessions.filter { it.startDate.ifBlank { Dates.dateOf(it.startedAt) } == selected }
            val dayPoints = ledger.filter { it.sourceDate == selected }.sumOf { it.amount }
            val protected = restDays.any { it.date == selected }
            val unusedMakeupCards = redemptions.count { it.productName == "补签卡" && it.productType == "CONSUMABLE" && it.usedAt == null }
            val selectedDate = Dates.localDate(selected); val today = LocalDate.now(); val weekEnd = today.plusDays((7 - today.dayOfWeek.value).toLong())
            Text("$selected · 学习 " + formatShort(daySessions.sumOf(::sessionSeconds)) + " · 任务 " + tasks.count { it.completedAt != null } + "/" + tasks.size, fontWeight = FontWeight.Bold)
            ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp)); Text("当日积分变化", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(if (dayPoints >= 0) "+$dayPoints" else dayPoints.toString(), color = if (dayPoints >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            } }
            ElevatedCard { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (protected) "日期已保护" else if (selectedDate.isBefore(today)) "补签卡" else "本周主动休息", fontWeight = FontWeight.Bold)
                    Text(if (protected) "这一天不会打断连续学习" else if (selectedDate.isBefore(today)) "可用 $unusedMakeupCards 张 · 只能保护过去空白日" else "每个自然周可免费选择1天", style = MaterialTheme.typography.bodySmall)
                }
                if (!protected && selectedDate.isBefore(today)) Button(
                    onClick = { confirmMakeup = true }, enabled = unusedMakeupCards > 0 && daySessions.isEmpty()
                ) { Text("补签") }
                if (!protected && !selectedDate.isBefore(today)) Button(
                    onClick = { confirmWeekly = true }, enabled = !selectedDate.isAfter(weekEnd)
                ) { Text("设为休息") }
            } }
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("当天备注") }, singleLine = true)
            OutlinedTextField(summary, { summary = it }, Modifier.fillMaxWidth(), label = { Text("学习总结") }, minLines = 3)
            Text("心情"); Row { (1..5).forEach { value -> IconButton({ mood = value }) { Icon(if (value <= mood) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (value <= mood) Color(0xFFFF8EAA) else MaterialTheme.colorScheme.outline) } } }
            Button({ vm.saveReflection(note, summary, mood) }, Modifier.fillMaxWidth()) { Text("保存总结") }
            Text("当天学习记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            daySessions.forEach { s -> ListItem(headlineContent = { Text(s.categoryName) }, supportingContent = { Text(s.note.ifBlank { "无备注" }) }, trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) { Text(formatShort(sessionSeconds(s))); IconButton({ deleteTarget = s }) { Icon(Icons.Default.DeleteOutline, "删除并重算") } } }) }
        }
    }
    deleteTarget?.let { item -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text("删除这条学习记录？") },
        text = { Text("删除后会自动重算当天学习积分，并检查是否需要撤销相关成就。") },
        dismissButton = { TextButton({ deleteTarget = null }) { Text("取消") } },
        confirmButton = { Button({ vm.deleteSession(item.id); deleteTarget = null }) { Text("删除并重算") } }
    ) }
    if (confirmMakeup) AlertDialog(
        onDismissRequest = { confirmMakeup = false }, title = { Text("使用一张补签卡？") },
        text = { Text("将保护过去的 $selected，使这一天不打断连续学习。使用后不可退回。") },
        dismissButton = { TextButton({ confirmMakeup = false }) { Text("取消") } },
        confirmButton = { Button({ vm.useRestCard(selected); confirmMakeup = false }) { Text("确认补签") } }
    )
    if (confirmWeekly) AlertDialog(
        onDismissRequest = { confirmWeekly = false }, title = { Text("设为本周主动休息日？") },
        text = { Text("选择 $selected 后不可更换或退回；即使当天后来学习，本周额度也不会返还。") },
        dismissButton = { TextButton({ confirmWeekly = false }) { Text("取消") } },
        confirmButton = { Button({ vm.scheduleWeeklyRest(selected); confirmWeekly = false }) { Text("确认设置") } }
    )
}

@Composable
private fun AchievementSection(vm: FocusViewModel) {
    val definitions by vm.achievements.collectAsState(); val unlocked by vm.unlocks.collectAsState(); val map = unlocked.associateBy { it.achievementKey }
    val sortedDefinitions = definitions.sortedWith(compareBy<AchievementDefinition> {
        when { it.key in map -> 0; it.hidden -> 2; else -> 1 }
    }.thenBy { it.sortOrder })
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("成就徽章 " + unlocked.size + "/" + definitions.size, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(sortedDefinitions, key = { it.key }) { item -> val earned = map[item.key]; ElevatedCard { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = if (earned != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(52.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (earned == null && item.hidden) Icons.Default.QuestionMark else achievementIcon(item.icon), null) } }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                Text(if (earned == null && item.hidden) "???" else item.name, fontWeight = FontWeight.Bold)
                Text(if (earned == null && item.hidden) "隐藏成就，等待你发现" else item.description, style = MaterialTheme.typography.bodySmall)
                Text(
                    when {
                        earned == null && item.key == "total_100" -> "未解锁 · 奖励 ${item.rewardPoints} 分 + 神秘奖励"
                        earned == null -> "未解锁 · 奖励 ${item.rewardPoints} 分"
                        item.key == "total_100" -> "已解锁 · 奖励 ${item.rewardPoints} 分 + 大耳狗主题 · ${formatAchievementMoment(earned.unlockedAt)}"
                        else -> "已解锁 · ${formatAchievementMoment(earned.unlockedAt)}"
                    },
                    color = if (earned != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } } }
    }
}

@Composable
private fun PetSection(vm: FocusViewModel) {
    val state by vm.petState.collectAsState()
    val motion by vm.petMotion.collectAsState()
    val activeTheme by vm.adminState.collectAsState()
    val context = LocalContext.current
    var area by remember { mutableIntStateOf(0) }
    var naming by remember { mutableStateOf(false) }
    var petName by remember { mutableStateOf("") }
    LaunchedEffect(state.unlocked, state.named) {
        if (state.unlocked && !state.named) naming = true
        if (state.named) vm.onPetPageVisible()
    }
    if (!state.unlocked) {
        val progress = (state.incubationSeconds.toFloat() / (10 * 3600)).coerceIn(0f, 1f)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Egg, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(18.dp)); Text("孵化中", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("累计学习10小时后，一位新伙伴会来陪你。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp)); LinearProgressIndicator({ progress }, Modifier.fillMaxWidth())
            Text("${formatShort(state.incubationSeconds)} / 10小时", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(area) { listOf("房间", "喂食", "装扮", "来信").forEachIndexed { i, label -> Tab(area == i, { area = i }, text = { Text(label) }) } }
        when (area) {
            0 -> LazyColumn(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.fillMaxWidth().background(
                                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f), MaterialTheme.colorScheme.surface))
                            ).padding(18.dp)
                        ) {
                            Surface(Modifier.align(Alignment.TopEnd), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("Lv.${state.level}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                PetAvatar(state, motion, vm)
                                Text(state.profile?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("今天也会陪你一起学习", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    val actions = state.unlocks.filter { it.itemType == "ACTION" }.sortedBy { it.sourceLevel }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("互动动作", fontWeight = FontWeight.Bold)
                            Text("点击后会立即在上方房间播放", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (actions.isEmpty()) Text("升到2级后解锁第一个动作。", style = MaterialTheme.typography.bodySmall)
                            else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                actions.chunked(2).forEach { actionRow ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        actionRow.forEach { action ->
                                            val active = motion.actionKey == action.itemKey
                                            FilterChip(
                                                selected = active,
                                                onClick = { vm.playPetAction(action) },
                                                label = { Text(petRewardName(action.itemKey)) },
                                                leadingIcon = { Icon(if (active) Icons.Default.PlayCircle else Icons.Default.Animation, null) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (actionRow.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("成长进度", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("等级 ${state.level} · ${formatPetMinutes(state.xp)}", color = MaterialTheme.colorScheme.primary)
                        }
                        val next = state.nextLevelXp
                        if (next != null) {
                            LinearProgressIndicator({ ((state.xp - PetRules.cumulativeForLevel(state.level)).toFloat() / (next - PetRules.cumulativeForLevel(state.level))).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                            Text("距离下一级还需学习 ${formatPetMinutes(next - state.xp)}", style = MaterialTheme.typography.bodySmall)
                        } else Text("已达到最高等级 · 共学习500小时", style = MaterialTheme.typography.bodySmall)
                        PetRules.rewards.firstOrNull { it.level > (state.profile?.highestRewardLevel ?: state.level) }?.let { reward ->
                            Text("下一奖励：${reward.level}级解锁“${reward.name}”", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    } }
                }
                item {
                    val profile = state.profile
                    val statuses = listOfNotNull(
                        profile?.equippedOutfit?.takeIf { it.isNotBlank() }?.let { "服装·${petRewardName(it)}" },
                        profile?.equippedHeadAccessory?.takeIf { it.isNotBlank() }?.let { "头部·${petRewardName(it)}" }
                    )
                    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前状态", fontWeight = FontWeight.Bold)
                        Text(statuses.ifEmpty { listOf("使用默认外观，表情将随状态自动切换") }.joinToString("　"), style = MaterialTheme.typography.bodySmall)
                        val furniture = profile?.placedFurnitureCsv.orEmpty().split(',').filter { it.isNotBlank() }.map(::petRewardName)
                        Text("房间摆件：" + furniture.ifEmpty { listOf("暂无") }.joinToString("、"), style = MaterialTheme.typography.bodySmall)
                        val latest = state.unlocks.maxByOrNull { it.unlockedAt }
                        latest?.let { Text("最近解锁：${petRewardName(it.itemKey)} · ${formatMoment(it.unlockedAt)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
                    } }
                }
                item { OutlinedButton({ petName = state.profile?.name.orEmpty(); naming = true }) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(6.dp)); Text("免费改名") } }
            }
            1 -> PetFeedingSection(state, motion, vm, activeTheme.activeTheme)
            2 -> PetWardrobe(state, vm)
            3 -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.letters.isEmpty()) item { Text("还没有收到来信", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(state.letters, key = { it.id }) { letter -> ElevatedCard { Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(letter.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(formatMoment(letter.receivedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp)); Text(letter.body)
                    letter.displayCode?.let { code ->
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text("角落里的小秘密：$code", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                context.getSystemService(ClipboardManager::class.java)
                                    ?.setPrimaryClip(ClipData.newPlainText("来信兑换码", code))
                                Toast.makeText(context, "兑换码已复制", Toast.LENGTH_SHORT).show()
                            }) { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("复制") }
                        }
                    }
                } } }
            }
        }
    }
    if (naming) AlertDialog(onDismissRequest = {}, title = { Text(if (state.named) "给伙伴改名" else "给新伙伴起个名字") },
        text = { Column { Text("请输入1–12个字符。首次命名后，它才会开始在你单日学习8小时时写来信。"); OutlinedTextField(petName, { petName = it.take(12) }, label = { Text("名字") }, singleLine = true) } },
        dismissButton = if (state.named) {{ TextButton({ naming = false }) { Text("取消") } }} else null,
        confirmButton = { Button({ vm.namePet(petName); if (petName.trim().isNotEmpty()) naming = false }, enabled = petName.trim().isNotEmpty()) { Text("确认") } })
}

@Composable
private fun PetFeedingSection(state: PetState, motion: PetMotionUi, vm: FocusViewModel, activeTheme: String) {
    val context = LocalContext.current
    val records by vm.foodRecords.collectAsState()
    val affection by vm.petAffection.collectAsState()
    val recognitionCount by vm.foodRecognitionCount.collectAsState()
    val pending by vm.pendingFood.collectAsState()
    val loading by vm.foodLoading.collectAsState()
    val notice by vm.foodNotice.collectAsState()
    val cottonCandyToken by vm.cottonCandyEffectToken.collectAsState()
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedKey by remember(pending) { mutableStateOf(pending?.result?.key.orEmpty()) }
    var showCatalog by remember { mutableStateOf(false) }
    var previewPhotoPath by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) vm.recognizeFood(uri, "CAMERA")
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.recognizeFood(uri, "GALLERY")
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "food_camera").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri; cameraLauncher.launch(uri)
    }
    val todayFed = records.count { it.status == "FED" && it.feedDate == Dates.today() }
    val cottonCandyFedToday = records.any { it.status == "FED" && it.feedDate == Dates.today() && it.sourceType == "CINNAMOROLL_TREAT" }
    val lastFedAt = records.filter { it.status == "FED" }.maxOfOrNull { it.fedAt ?: 0L } ?: 0L
    val waitMillis = (2 * 60 * 60 * 1000L - (System.currentTimeMillis() - lastFedAt)).coerceAtLeast(0L)
    val canFeed = todayFed < 3 && waitMillis == 0L

    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        state.profile?.let { profile ->
                            PolishedPetFurniture(profile)
                            AnimatedPet(profile, state.unlocks, motion, false, Modifier.fillMaxSize(), vm::petTapped)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("♥ $affection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("亲密度", style = MaterialTheme.typography.labelSmall) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$todayFed / 3", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("今日喂食", style = MaterialTheme.typography.labelSmall) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$recognitionCount / 10", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("今日识别", style = MaterialTheme.typography.labelSmall) }
                    }
                    if (waitMillis > 0) Text("消化中：${formatFeedingWait(waitMillis)}后可再进食", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(::launchCamera, Modifier.weight(1f), enabled = !loading) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("拍照识别") }
                OutlinedButton({ galleryLauncher.launch("image/*") }, Modifier.weight(1f), enabled = !loading) { Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("从相册选") }
            }
            if (loading) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在压缩并识别食物…", style = MaterialTheme.typography.bodySmall) }
            notice?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        }
        if (activeTheme == ThemeCatalog.CINNAMOROLL) item {
            ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFEAF6FF))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FoodArtwork(foodArtworkResource("cotton_candy")!!, Modifier.size(58.dp))
                    Column(Modifier.weight(1f)) {
                        Text("大耳狗的云朵棉花糖", fontWeight = FontWeight.Bold, color = Color(0xFF356FA8))
                        Text("主题限定 · 喂食后会出现甜甜的特效", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D86B4))
                    }
                    Button({ vm.feedCinnamorollCottonCandy() }, enabled = canFeed && !cottonCandyFedToday) { Text(if (cottonCandyFedToday) "明日再送" else "送给它") }
                }
            }
        }
        pending?.let { item {
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("确认识别结果", fontWeight = FontWeight.Bold)
                val artwork = foodArtworkResource(selectedKey)
                if (artwork == null) {
                    FoodThumbnail(it.photoPath, Modifier.fillMaxWidth().height(170.dp))
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("原照片", style = MaterialTheme.typography.labelSmall)
                            FoodThumbnail(it.photoPath, Modifier.fillMaxWidth().height(150.dp))
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("宠物食物图", style = MaterialTheme.typography.labelSmall)
                            FoodArtwork(artwork, Modifier.fillMaxWidth().height(150.dp))
                        }
                    }
                }
                val result = it.result
                if (result != null) {
                    Text("AI识别：${result.name} · ${(result.confidence * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(result.candidates.take(4), key = { it }) { key -> FilterChip(selectedKey == key, { selectedKey = key }, label = { Text(FoodCatalog.find(key).name) }) }
                    }
                    Text(
                        if (selectedKey == result.key) "识别结果对吗？确认后会记录“识别正确”反馈。"
                        else "已选择“${FoodCatalog.find(selectedKey).name}”：确认后会记录纠正反馈，帮助你回看常见误判。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else Text(it.fallbackReason ?: "请选择食物", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton({ showCatalog = true }, Modifier.fillMaxWidth()) { Icon(Icons.Default.RestaurantMenu, null); Spacer(Modifier.width(6.dp)); Text(if (selectedKey.isBlank()) "从食物目录选择" else "已选择：${FoodCatalog.find(selectedKey).name}") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(vm::cancelPendingFood, Modifier.weight(1f)) { Text("取消") }
                    Button({ vm.confirmFood(selectedKey) }, Modifier.weight(1f), enabled = selectedKey.isNotBlank()) { Text(if (result == null) "确认保存" else "确认并反馈") }
                }
            } }
        } }
        item { Text("食物日记", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (records.isEmpty()) item { Text("还没有食物记录，拍一张今天的食物试试吧。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(records, key = { it.id }) { record ->
            ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val artwork = foodArtworkResource(record.recognizedKey)
                if (artwork != null) {
                    FoodArtwork(artwork, Modifier.size(72.dp).clickable { previewPhotoPath = record.photoPath })
                } else FoodThumbnail(record.photoPath, Modifier.size(72.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.recognizedName, fontWeight = FontWeight.Bold)
                    Text(formatMoment(record.confirmedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(when (record.status) { "FED" -> "已喂食 · 亲密度 +1"; "REFUSED" -> "不适合宠物 · 已温柔拒绝"; else -> "等待喂食" }, style = MaterialTheme.typography.bodySmall, color = if (record.status == "READY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (record.status == "READY") Button({ vm.feedPet(record.id) }, enabled = canFeed) { Text("喂给它") }
                    IconButton({ vm.deleteFoodRecord(record.id) }) { Icon(Icons.Default.DeleteOutline, "删除食物记录") }
                }
            } }
        }
    }
    CottonCandyCelebration(cottonCandyToken)
    }

    if (showCatalog) AlertDialog(
        onDismissRequest = { showCatalog = false },
        title = { Text("选择食物") },
        text = { LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(FoodCatalog.items, key = { it.key }) { food ->
                ListItem(
                    headlineContent = { Text(food.name) },
                    supportingContent = if (food.unsafe) {{ Text("宠物会温柔拒绝", color = MaterialTheme.colorScheme.error) }} else null,
                    leadingContent = {
                        foodArtworkResource(food.key)?.let { FoodArtwork(it, Modifier.size(44.dp)) }
                            ?: Icon(if (food.unsafe) Icons.Default.Block else Icons.Default.Restaurant, null)
                    },
                    modifier = Modifier.clickable { selectedKey = food.key; showCatalog = false }
                )
            }
        } },
        confirmButton = { TextButton({ showCatalog = false }) { Text("关闭") } }
    )

    previewPhotoPath?.let { path ->
        AlertDialog(
            onDismissRequest = { previewPhotoPath = null },
            title = { Text("拍摄的原照片") },
            text = { FoodThumbnail(path, Modifier.fillMaxWidth().height(320.dp)) },
            confirmButton = { TextButton({ previewPhotoPath = null }) { Text("关闭") } }
        )
    }
}

@Composable
private fun BoxScope.CottonCandyCelebration(token: Long) {
    if (token == 0L) return
    val progress = remember(token) { Animatable(0f) }
    LaunchedEffect(token) { progress.animateTo(1f, tween(1_700, easing = FastOutSlowInEasing)) }
    val p = progress.value
    if (p < 1f) {
        Canvas(Modifier.matchParentSize().graphicsLayer(alpha = (1f - p).coerceIn(0f, 1f))) {
            val center = Offset(size.width / 2, size.height / 2)
            val colors = listOf(Color(0xFFBCE7FF), Color.White, Color(0xFFFFE58C), Color(0xFFD9C6FF))
            repeat(16) { index ->
                val angle = index * (2f * PI.toFloat() / 16f)
                val radius = size.minDimension * (.12f + p * .48f)
                val point = Offset(center.x + kotlin.math.cos(angle) * radius, center.y + kotlin.math.sin(angle) * radius)
                drawCircle(colors[index % colors.size], size.minDimension * (.018f + (1f - p) * .025f), point)
            }
            drawCircle(Color(0xFFBCE7FF).copy(alpha = .32f), size.minDimension * (.15f + p * .32f), center)
        }
        Surface(Modifier.align(Alignment.TopCenter).graphicsLayer(alpha = (1f - p).coerceIn(0f, 1f), translationY = -p * 24f), color = Color.White.copy(alpha = .92f), shape = MaterialTheme.shapes.extraLarge) {
            Text("☁ 棉花糖时间！", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF4E8FD1), fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatFeedingWait(millis: Long): String {
    val minutes = (millis + 59_999L) / 60_000L
    return if (minutes >= 60) "${minutes / 60}小时${minutes % 60}分" else "${minutes}分钟"
}

@Composable
private fun FoodArtwork(@androidx.annotation.DrawableRes resource: Int, modifier: Modifier = Modifier) {
    Surface(modifier.clip(MaterialTheme.shapes.medium), color = MaterialTheme.colorScheme.surfaceVariant) {
        Image(painterResource(resource), null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun FoodThumbnail(path: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { path?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    Surface(modifier.clip(MaterialTheme.shapes.medium), color = MaterialTheme.colorScheme.surfaceVariant) {
        if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.ImageNotSupported, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.outline) }
    }
}

@Composable
private fun PetWardrobe(state: PetState, vm: FocusViewModel) {
    var category by remember { mutableIntStateOf(0) }
    val sections = listOf(
        "服装" to setOf("OUTFIT", "OUTFIT_MAIN"),
        "配饰" to setOf("ACCESSORY_HEAD"),
        "表情" to setOf("EXPRESSION"),
        "摆件" to setOf("FURNITURE")
    )
    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(category) {
            sections.forEachIndexed { index, section ->
                Tab(category == index, { category = index }, text = { Text(section.first) })
            }
        }
        val types = sections[category].second
        val hasItems = PetRules.rewards.any { it.type in types } || state.unlocks.any { it.itemType in types }
        if (hasItems) PetItemsList(state, types, vm)
        else Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("还没有获得这一类装扮。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PetAvatar(state: PetState, motion: PetMotionUi, vm: FocusViewModel) {
    val profile = state.profile ?: return
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        PolishedPetFurniture(profile)
        AnimatedPet(profile, state.unlocks, motion, studying = false, Modifier.fillMaxSize(), vm::petTapped)
    }
}

@Composable
private fun PetItemsList(state: PetState, types: Set<String>, vm: FocusViewModel) {
    val unlocked = state.unlocks.associateBy { it.itemKey }
    val standard = PetRules.rewards.filter { it.type in types }
    val rewards = standard + state.unlocks.filter { it.itemType in types && it.itemKey !in standard.map { reward -> reward.key } }
        .map { PetReward(0, it.itemKey, petRewardName(it.itemKey), it.itemType) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(rewards, key = { it.key }) { reward ->
            val item = unlocked[reward.key]
            val profile = state.profile
            val active = when (reward.type) {
                "EXPRESSION", "ACTION" -> false
                "OUTFIT", "OUTFIT_MAIN" -> profile?.equippedOutfit == reward.key
                "ACCESSORY_EYE" -> profile?.equippedEyeAccessory == reward.key
                "ACCESSORY_NECK" -> profile?.equippedNeckAccessory == reward.key
                "ACCESSORY_HEAD" -> profile?.equippedHeadAccessory == reward.key
                "FURNITURE" -> reward.key in profile?.placedFurnitureCsv.orEmpty().split(',')
                else -> false
            }
            ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (item == null) Icons.Default.Lock else if (active) Icons.Default.CheckCircle else if (reward.type == "ACTION") Icons.Default.Animation else Icons.Default.Checkroom, null, Modifier.size(38.dp), tint = if (item == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(if (item == null) "剪影 · ${reward.name}" else reward.name, fontWeight = FontWeight.Bold); Text(if (item == null) "${reward.level}级解锁" else if (active) "当前使用中" else "已永久获得", style = MaterialTheme.typography.bodySmall) }
                if (item != null) OutlinedButton({ vm.equipPetItem(item) }, enabled = reward.type != "EXPRESSION") { Text(if (reward.type == "EXPRESSION") "自动" else if (active) "取消" else if (reward.type == "FURNITURE") "摆放" else if (reward.type == "ACTION") "播放" else "使用") }
            } }
        }
    }
}

private data class ConstellationArtwork(
    val title: String,
    val detail: String,
    val unlockText: String,
    val image: Int,
    val threshold: Int,
    val rewardPoints: Int
)

private val constellationArtwork = listOf(
    ConstellationArtwork("北斗七星", "七颗星连成斗柄，像一枚在漫长备考路上指引方向的星标。", "7颗星解锁", R.drawable.constellation_big_dipper, 7, 1),
    ConstellationArtwork("双鱼座", "两条星光游鱼在银河中彼此呼应，记录你细水长流的坚持。", "30颗星解锁", R.drawable.constellation_pisces, 30, 5),
    ConstellationArtwork("华师大星门", "星点勾勒出通往目标院校的校门。每一颗被点亮的星，都是你真实走过的一小段路。", "100颗星解锁", R.drawable.constellation_ecnu_gate, 100, 20)
)

@Composable
private fun StarsSection(vm: FocusViewModel, activeTheme: String) {
    val state by vm.petState.collectAsState()
    val ledger by vm.ledger.collectAsState()
    val seenStars by vm.seenStarCount.collectAsState()
    val isStarryTheme = activeTheme == ThemeCatalog.STARRY
    var selected by remember { mutableStateOf<ConstellationArtwork?>(null) }
    var displayedStars by remember { mutableIntStateOf(0) }
    var newlyLitIndex by remember { mutableIntStateOf(-1) }
    val starPulse = remember { Animatable(0f) }
    val targetStars = state.stars.coerceIn(0, 100)
    LaunchedEffect(isStarryTheme, targetStars, seenStars >= 0) {
        if (!isStarryTheme || seenStars < 0) return@LaunchedEffect
        val start = seenStars.coerceIn(0, targetStars)
        if (seenStars > targetStars) vm.setSeenStarCount(targetStars)
        displayedStars = start
        val many = targetStars - start > 10
        while (displayedStars < targetStars) {
            newlyLitIndex = displayedStars
            displayedStars += 1
            starPulse.snapTo(0f)
            starPulse.animateTo(1f, tween(if (many) 120 else 420))
            starPulse.animateTo(0f, tween(if (many) 180 else 520))
            vm.setSeenStarCount(displayedStars)
        }
        newlyLitIndex = -1
    }
    val unlocked = buildList {
        if (state.stars >= 7) add(constellationArtwork[0])
        if (state.stars >= 30) add(constellationArtwork[1])
        if (state.stars >= 100) add(constellationArtwork[2])
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ElevatedCard { Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(if (isStarryTheme) "我的星空" else "已收集 ${state.stars} 颗星", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("星光会在这里慢慢聚集。", style = MaterialTheme.typography.bodySmall)
        } } }
        if (isStarryTheme) item { HundredStarSky(displayedStars, newlyLitIndex, starPulse.value) }
        items(unlocked, key = { it.title }) { art ->
            if (isStarryTheme) StarryConstellationCard(art) { selected = art }
            else ConstellationCard(art.title, art.detail)
        }
        if (!isStarryTheme) state.nextStarDistance?.let { item { Text("距离下一片隐藏星空还差 $it 颗星。", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
    }
    selected?.let { art ->
        val claimed = ledger.any { it.businessKey == "constellation_reward:${art.threshold}" }
        AlertDialog(
        onDismissRequest = { selected = null },
        title = { Text(art.title, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(art.image), art.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
            Text(art.detail)
            Text(art.unlockText, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (claimed) Text("已领取 +${art.rewardPoints} 积分", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        } },
        confirmButton = { TextButton({ selected = null }) { Text("收好") } },
        dismissButton = if (!claimed) {{ TextButton({ vm.claimConstellationReward(art.threshold, art.title, art.rewardPoints) }) { Text("领取奖励 +${art.rewardPoints}") } }} else null
    ) }
}

@Composable
private fun HundredStarSky(litCount: Int, newlyLitIndex: Int, pulse: Float) {
    val collectibleStars = remember {
        val random = Random(27401)
        List(100) {
            Triple(
                .035f + random.nextFloat() * .93f,
                .035f + random.nextFloat() * .93f,
                .58f + random.nextFloat() * .82f
            )
        }
    }
    val distantStars = remember {
        val random = Random(91357)
        List(320) {
            Triple(
                random.nextFloat(),
                random.nextFloat(),
                .20f + random.nextFloat() * .62f
            )
        }
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(.78f).clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Color(0xFF020611), Color(0xFF08152B), Color(0xFF17112E), Color(0xFF030713))))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            listOf(.02f to .88f, .20f to .70f, .42f to .52f, .65f to .33f, .88f to .15f, 1.04f to -.02f).forEachIndexed { index, point ->
                val center = Offset(size.width * point.first, size.height * point.second)
                val radius = size.width * (.27f + (index % 3) * .035f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(if (index % 2 == 0) Color(0x263D6CA6) else Color(0x202E4F82), Color.Transparent),
                        center,
                        radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            distantStars.forEachIndexed { index, star ->
                drawCircle(
                    Color.White.copy(alpha = .08f + (index % 7) * .032f),
                    star.third * density,
                    Offset(size.width * star.first, size.height * star.second)
                )
            }
            collectibleStars.forEachIndexed { index, star ->
                val center = Offset(size.width * star.first, size.height * star.second)
                val radius = star.third * density
                if (index < litCount) {
                    val isNew = index == newlyLitIndex
                    drawCircle(if (isNew) Color(0x66FFF0B5) else Color(0x1FFFF0B5), radius * (6f + if (isNew) pulse * 10f else 0f), center)
                    drawCircle(Color(0xFFFFE4A0), radius * 2.1f, center)
                    drawCircle(Color.White, radius * .8f, center)
                    if (index % 6 == 0 || isNew) {
                        val ray = radius * (4f + if (isNew) pulse * 5f else 0f)
                        drawLine(Color(0x99FFF1C2), center - Offset(ray, 0f), center + Offset(ray, 0f), .55f * density, StrokeCap.Round)
                        drawLine(Color(0x99FFF1C2), center - Offset(0f, ray), center + Offset(0f, ray), .55f * density, StrokeCap.Round)
                    }
                } else drawCircle(Color(0xFF90A7CD).copy(alpha = .12f), radius, center)
            }
        }
    }
}

@Composable
private fun StarryConstellationCard(art: ConstellationArtwork, onClick: () -> Unit) = ElevatedCard(
    Modifier.fillMaxWidth().clickable(onClick = onClick)
) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
    Image(painterResource(art.image), art.title, Modifier.size(76.dp).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(art.title, fontWeight = FontWeight.Bold); Text(art.unlockText, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall); Text("点击查看星图", style = MaterialTheme.typography.bodySmall) }
    Icon(Icons.Default.ChevronRight, null)
} }

@Composable private fun ConstellationCard(title: String, detail: String) = ElevatedCard { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Stars, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(detail, style = MaterialTheme.typography.bodySmall) } } }
private fun petRewardName(key: String) = PetRules.rewards.firstOrNull { it.key == key }?.name ?: when (key) { "outfit_moonlight", "outfit_starry" -> "星空限定礼服"; else -> key }

@Composable private fun StatCard(title: String, value: String, modifier: Modifier) = ElevatedCard(modifier.padding(vertical = 5.dp)) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) } }
private fun sessionSeconds(item: StudySession) = if (item.durationSeconds > 0) item.durationSeconds else item.minutes * 60L
private fun formatShort(seconds: Long): String = if (seconds < 3600) (seconds / 60).toString() + "分" else (seconds / 3600).toString() + "时" + (seconds / 60 % 60) + "分"
private fun formatPetMinutes(minutes: Int): String = when {
    minutes <= 0 -> "0小时"
    minutes % 60 == 0 -> "${minutes / 60}小时"
    else -> "${minutes / 60}小时${minutes % 60}分"
}
private val momentFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private fun formatMoment(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(momentFormatter)
private val achievementMomentFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分")
private fun formatAchievementMoment(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(achievementMomentFormatter)
private fun achievementIcon(name: String) = when (name) {
    "book" -> Icons.Default.MenuBook; "trophy" -> Icons.Default.EmojiEvents; "moon" -> Icons.Default.DarkMode; "pause" -> Icons.Default.PauseCircle
    "flame" -> Icons.Default.LocalFireDepartment; "clock" -> Icons.Default.Schedule; "medal" -> Icons.Default.MilitaryTech
    "gift" -> Icons.Default.CardGiftcard; "check" -> Icons.Default.TaskAlt; "sun" -> Icons.Default.LightMode; "crown" -> Icons.Default.WorkspacePremium; else -> Icons.Default.Star
}
