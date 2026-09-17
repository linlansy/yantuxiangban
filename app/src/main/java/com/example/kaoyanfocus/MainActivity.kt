package com.example.kaoyanfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    private val model by viewModels<FocusViewModel>()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                model.lockAdmin()
                // Dynamic launcher icons require toggling activity aliases.
                // Do it only after the user has already left the app so theme
                // selection never pushes the visible activity back to Home.
                model.applyLauncherIconAfterLeavingApp()
            }
        })
        setContent { StudyApp(model) }
    }
}

private val CinnaColors = lightColorScheme(
    primary = Color(0xFF4F8FD8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF), onPrimaryContainer = Color(0xFF123A67),
    secondary = Color(0xFFFF8EAA), secondaryContainer = Color(0xFFFFDCE5),
    background = Color(0xFFF8FBFF), surface = Color(0xF2FFFFFF),
    surfaceVariant = Color(0xFFEAF3FF), outline = Color(0xFFAEC7E6)
)

private val StarryColors = darkColorScheme(
    primary = Color(0xFFAEB8FF), onPrimary = Color(0xFF172050),
    primaryContainer = Color(0xFF354184), onPrimaryContainer = Color(0xFFE0E4FF),
    secondary = Color(0xFFFFD87A), secondaryContainer = Color(0xFF5B4714),
    background = Color(0xFF11183C), surface = Color(0xED1D2754),
    surfaceVariant = Color(0xFF303B72), outline = Color(0xFF8D9AE0)
)

@Composable
private fun StudyApp(vm: FocusViewModel) {
    val admin by vm.adminState.collectAsState()
    val recovery by vm.recoveryCode.collectAsState()
    val message by vm.message.collectAsState()
    val achievement by vm.achievementNotice.collectAsState()
    val activeTheme = admin.activeTheme
    val colors = when (activeTheme) {
        ThemeCatalog.CINNAMOROLL -> CinnaColors
        ThemeCatalog.STARRY -> StarryColors
        else -> lightColorScheme(primary = Color(0xFF386A49))
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
            Box(Modifier.fillMaxSize()) {
                when (activeTheme) {
                    ThemeCatalog.CINNAMOROLL -> Image(painterResource(R.drawable.theme_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .23f)
                    ThemeCatalog.STARRY -> Image(painterResource(R.drawable.starry_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .92f)
                }
                if (!admin.onboardingComplete) OnboardingScreen(vm) else MainNavigation(vm, activeTheme)
            }
            if (recovery != null) AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Key, null) },
            title = { Text("请保存恢复码") },
            text = { Column { Text("忘记 PIN 时只能使用它重设。请截图或抄写："); Spacer(Modifier.height(12.dp)); Text(recovery!!, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } },
            confirmButton = { Button(onClick = vm::dismissRecoveryCode) { Text("我已保存") } }
        )
            if (recovery == null && achievement != null) AlertDialog(
            onDismissRequest = vm::dismissAchievementNotice,
            icon = { Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("解锁新成就！") },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(achievement!!.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(achievement!!.description)
                if (achievement!!.key == "total_100") {
                    Spacer(Modifier.height(12.dp))
                    Text("神秘奖励揭晓", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("恭喜获得隐藏主题：大耳狗主题", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (achievement!!.key == "total_10") {
                    Spacer(Modifier.height(12.dp))
                    Text("宠物模块已孵化完成！", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("前往统计 → 宠物，给新伙伴起个名字吧。", color = MaterialTheme.colorScheme.primary)
                }
                if (achievement!!.rewardPoints > 0) Text("奖励 +${achievement!!.rewardPoints} 积分", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } },
            confirmButton = { Button(onClick = vm::dismissAchievementNotice) { Text("太棒了") } }
        )
            if (recovery == null && achievement == null && message != null) AlertDialog(
            onDismissRequest = vm::clearMessage,
            text = { Text(message!!) },
            confirmButton = { TextButton(onClick = vm::clearMessage) { Text("知道了") } }
            )
        }
    }
}

@Composable
private fun MainNavigation(vm: FocusViewModel, activeTheme: String) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "focus"
    val isCuteTheme = activeTheme != ThemeCatalog.DEFAULT
    val items = listOf(
        AppNavItem("focus", "专注", Icons.Default.PlayCircle, Icons.Default.Favorite, Color(0xFFFF7FA5)),
        AppNavItem("shop", "商城", Icons.Default.ShoppingBag, Icons.Default.CardGiftcard, Color(0xFF58A5E8)),
        AppNavItem("stats", "统计", Icons.Default.BarChart, Icons.Default.AutoAwesome, Color(0xFF9478D6)),
        AppNavItem("settings", "设置", Icons.Default.Settings, Icons.Default.SettingsSuggest, Color(0xFFF2A93B))
    )
    Scaffold(containerColor = Color.Transparent, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
            items.forEach { item ->
                val selectedColor = if (isCuteTheme) item.cuteColor else MaterialTheme.colorScheme.primary
                NavigationBarItem(selected = route == item.route, onClick = {
                    nav.navigate(item.route) { popUpTo("focus") { saveState = true }; launchSingleTop = true; restoreState = true }
                }, icon = { Icon(if (isCuteTheme) item.cuteIcon else item.defaultIcon, null, Modifier.size(if (isCuteTheme) 28.dp else 24.dp)) }, label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        indicatorColor = selectedColor.copy(alpha = if (isCuteTheme) .20f else .14f),
                        unselectedIconColor = if (isCuteTheme) item.cuteColor.copy(alpha = .68f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ))
            }
        }
    }) { padding ->
        NavHost(nav, "focus", Modifier.padding(padding)) {
            composable("focus") { FocusScreen(vm, activeTheme) }
            composable("shop") { ShopScreen(vm, activeTheme) }
            composable("stats") { StatsScreen(vm, activeTheme) }
            composable("settings") { SettingsScreen(vm, activeTheme) }
        }
    }
}

private data class AppNavItem(
    val route: String,
    val label: String,
    val defaultIcon: ImageVector,
    val cuteIcon: ImageVector,
    val cuteColor: Color
)

@Composable
private fun OnboardingScreen(vm: FocusViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AutoStories, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("欢迎使用研途相伴", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("自由记录学习，用积分兑换属于你的奖励。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
        ElevatedCard { Column(Modifier.padding(18.dp)) {
            Text("首次设置", fontWeight = FontWeight.Bold)
            Text("学习每满1小时获得1分；满8小时额外加5分，基础积分最多计算12小时。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("设置6位管理 PIN") }, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(6) }, label = { Text("再次输入 PIN") }, visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.configurePin(pin, true) }, enabled = pin.length == 6 && pin == confirm, modifier = Modifier.fillMaxWidth()) { Text("开始使用") }
        } }
    }
}

fun formatDuration(seconds: Long): String = "%02d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
