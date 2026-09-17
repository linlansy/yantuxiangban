package com.example.kaoyanfocus

import com.example.kaoyanfocus.data.AchievementUnlock
import com.example.kaoyanfocus.data.Redemption

data class ThemeOption(val key: String, val name: String, val description: String)

object ThemeCatalog {
    const val DEFAULT = "default"
    const val CINNAMOROLL = "cinnamoroll"
    const val STARRY = "starry"

    val default = ThemeOption(DEFAULT, "默认主题", "跟随研途相伴基础配色")
    val cinnamoroll = ThemeOption(CINNAMOROLL, "大耳狗主题", "累计100小时成就的隐藏主题奖励")
    val starry = ThemeOption(STARRY, "星空夜读主题", "宁静深蓝夜空与月光自习室")

    fun owned(redemptions: List<Redemption>, unlocks: List<AchievementUnlock>): List<ThemeOption> = buildList {
        add(default)
        val redemptionNames = redemptions.mapTo(mutableSetOf()) { it.productName }
        if ("total_100" in unlocks.mapTo(mutableSetOf()) { it.achievementKey } || "大耳狗主题" in redemptionNames) add(cinnamoroll)
        if ("星空夜读主题" in redemptionNames) add(starry)
    }

    fun nameOf(key: String): String = when (key) {
        CINNAMOROLL -> cinnamoroll.name
        STARRY -> starry.name
        else -> default.name
    }
}
