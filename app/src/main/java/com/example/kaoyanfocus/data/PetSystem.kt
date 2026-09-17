package com.example.kaoyanfocus.data

import com.example.kaoyanfocus.ThemeCatalog
import java.time.LocalDate

data class PetReward(val level: Int, val key: String, val name: String, val type: String)

object PetRules {
    /**
     * Total valid study minutes required for each level. Level 1 is the
     * 10-hour incubation boundary and level 20 is exactly 500 total hours.
     */
    private val cumulativeMinutes = listOf(
        600,    // Lv.1   - 10 h
        900,    // Lv.2   - 15 h
        1_320,  // Lv.3   - 22 h
        1_800,  // Lv.4   - 30 h
        2_400,  // Lv.5   - 40 h
        3_120,  // Lv.6   - 52 h
        3_960,  // Lv.7   - 66 h
        4_920,  // Lv.8   - 82 h
        6_000,  // Lv.9   - 100 h
        7_200,  // Lv.10  - 120 h
        8_520,  // Lv.11  - 142 h
        9_960,  // Lv.12  - 166 h
        11_640, // Lv.13  - 194 h
        13_560, // Lv.14  - 226 h
        15_720, // Lv.15  - 262 h
        18_120, // Lv.16  - 302 h
        20_760, // Lv.17  - 346 h
        23_640, // Lv.18  - 394 h
        26_760, // Lv.19  - 446 h
        30_000  // Lv.20  - 500 h
    )

    val rewards = listOf(
        PetReward(2, "action_wave", "挥手", "ACTION"),
        PetReward(3, "expression_happy", "开心", "EXPRESSION"),
        PetReward(4, "furniture_books", "书堆", "FURNITURE"),
        PetReward(5, "outfit_school", "蓝色校服", "OUTFIT_MAIN"),
        PetReward(6, "expression_focus", "专注", "EXPRESSION"),
        PetReward(7, "furniture_lamp", "台灯", "FURNITURE"),
        PetReward(8, "action_read", "看书", "ACTION"),
        PetReward(9, "furniture_cloud_cushion", "云朵坐垫", "FURNITURE"),
        PetReward(10, "furniture_clock", "时钟", "FURNITURE"),
        PetReward(11, "expression_sleepy", "困倦", "EXPRESSION"),
        PetReward(12, "outfit_star_pajamas", "星星睡衣", "OUTFIT_MAIN"),
        PetReward(13, "action_stretch", "伸懒腰", "ACTION"),
        PetReward(14, "furniture_plant", "盆栽", "FURNITURE"),
        PetReward(15, "expression_proud", "骄傲", "EXPRESSION"),
        PetReward(16, "outfit_library", "图书馆马甲", "OUTFIT_MAIN"),
        PetReward(17, "furniture_star_globe", "星空球", "FURNITURE"),
        PetReward(18, "action_celebrate", "庆祝", "ACTION"),
        PetReward(19, "furniture_goal_trophy", "目标奖杯", "FURNITURE"),
        PetReward(20, "furniture_target_board", "目标板", "FURNITURE")
    )

    fun cumulativeForLevel(level: Int): Int = cumulativeMinutes[(level.coerceIn(1, 20) - 1)]
    fun levelForXp(xp: Int): Int = (20 downTo 1).firstOrNull { xp >= cumulativeForLevel(it) } ?: 1
    fun nextLevelXp(level: Int): Int? = if (level >= 20) null else cumulativeForLevel(level + 1)
    fun starsFor(sessions: List<StudySession>): Int =
        (sessions.asSequence()
            .filter { it.themeKeyAtStart == ThemeCatalog.STARRY }
            .sumOf { if (it.durationSeconds > 0) it.durationSeconds else it.minutes * 60L } / 3600).toInt()
}

data class PetState(
    val profile: PetProfile? = null,
    val incubationSeconds: Long = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val nextLevelXp: Int? = 360,
    val unlocks: List<PetUnlock> = emptyList(),
    val letters: List<PetLetter> = emptyList(),
    val stars: Int = 0,
    val nextStarDistance: Int? = 7
) {
    val unlocked get() = profile != null
    val named get() = !profile?.name.isNullOrBlank()
}

object Encouragements {
    private val lines = mapOf(
        "RESTART" to listOf("重新开始也是前进，今天先学一小会儿吧。", "不用追赶昨天，从今天的第一页开始。", "停下过也没关系，你随时都能再出发。", "先给自己十分钟，状态会慢慢回来。", "今天的进度条，等你亲手点亮。"),
        "REST" to listOf("你已经很努力了，今天记得给自己留点呼吸。", "慢一点没关系，稳稳地走会更远。", "喝口水、伸个懒腰，再决定下一步。", "休息不是落后，是在为下一段路蓄力。", "今天的目标可以小一点，但请对自己温柔一点。"),
        "SPRINT" to listOf("状态正好，带着这份节奏再向目标靠近一点。", "你的专注正在发光，也别忘了按时休息。", "冲刺时也要稳住步子，每一页都算数。", "最近的你很有力量，继续保持清醒和节奏。", "离目标更近了，今天也把专注放在最重要的事上吧。"),
        "STEADY" to listOf("一点一点积累，你正在稳稳地靠近目标。", "今天的一步也很重要，按自己的节奏来。", "平凡的坚持会在某天连成一条很长的路。", "不用每天都超常发挥，稳定就很了不起。", "书页会记住你的耐心，今天也继续吧。")
    )

    fun choose(sessions: List<StudySession>, reflections: List<DailyReflection>, today: LocalDate = LocalDate.now()): String {
        val dates = (0L..2L).map { today.minusDays(it).toString() }
        val recent = sessions.filter { it.startDate.ifBlank { Dates.dateOf(it.startedAt) } in dates }
        fun secondsOf(date: String) = recent.filter { it.startDate.ifBlank { Dates.dateOf(it.startedAt) } == date }.sumOf { if (it.durationSeconds > 0) it.durationSeconds else it.minutes * 60L }
        val lastTwoEmpty = dates.take(2).all { secondsOf(it) < 60 }
        val moods = reflections.filter { it.date in dates }.map { it.mood }
        val avgMood = moods.takeIf { it.isNotEmpty() }?.average()
        val avgPauses = recent.takeIf { it.isNotEmpty() }?.map { it.pauseCount }?.average() ?: 0.0
        val total = dates.sumOf(::secondsOf)
        val state = when {
            lastTwoEmpty -> "RESTART"
            (avgMood != null && avgMood <= 2.5) || avgPauses >= 5.0 -> "REST"
            total >= 12 * 3600 && (avgMood == null || avgMood >= 3.0) -> "SPRINT"
            else -> "STEADY"
        }
        return lines.getValue(state)[today.toEpochDay().mod(5)]
    }
}
