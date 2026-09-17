package com.example.kaoyanfocus.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.example.kaoyanfocus.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.roundToInt

data class FoodDefinition(val key: String, val name: String, val unsafe: Boolean = false)

object FoodCatalog {
    /** Items offered to the user and the recognizer: each is a real, commonly eaten serving. */
    val items = listOf(
        FoodDefinition("fried_rice", "炒饭"), FoodDefinition("egg_fried_rice", "蛋炒饭"), FoodDefinition("curry_rice", "咖喱饭"),
        FoodDefinition("claypot_rice", "煲仔饭"), FoodDefinition("bibimbap", "拌饭"), FoodDefinition("yellow_braised_chicken_rice", "黄焖鸡米饭"),
        FoodDefinition("bento", "便当"), FoodDefinition("porridge_side", "粥和小菜"), FoodDefinition("century_egg_porridge", "皮蛋瘦肉粥"),
        FoodDefinition("fried_noodles", "炒面"), FoodDefinition("mixed_noodles", "拌面"), FoodDefinition("chow_fun", "干炒牛河"), FoodDefinition("hot_dry_noodles", "热干面"),
        FoodDefinition("beef_noodle_soup", "牛肉面"), FoodDefinition("spicy_meat_noodles", "辣肉面"), FoodDefinition("braised_noodles", "烩面"),
        FoodDefinition("rice_noodles", "牛肉米粉"), FoodDefinition("snail_noodles", "螺蛳粉"), FoodDefinition("sour_spicy_noodles", "酸辣粉"), FoodDefinition("wonton", "馄饨"),
        FoodDefinition("dumplings", "饺子"), FoodDefinition("baozi", "包子"), FoodDefinition("shaomai", "烧卖"), FoodDefinition("spring_roll", "春卷"),
        FoodDefinition("jiaozi_soup", "水饺汤"), FoodDefinition("soup_dumplings", "汤包（小笼包）"), FoodDefinition("har_gow", "虾饺皇"), FoodDefinition("red_rice_roll", "红米肠"), FoodDefinition("boat_congee", "船仔粥"),
        FoodDefinition("changfen", "肠粉"), FoodDefinition("jianbing", "煎饼果子"), FoodDefinition("mantou", "馒头"),
        FoodDefinition("bread", "面包"), FoodDefinition("toast", "吐司"), FoodDefinition("pineapple_bun_butter", "菠萝油"), FoodDefinition("sandwich", "三明治"),
        FoodDefinition("hamburger", "汉堡"), FoodDefinition("pizza", "披萨"), FoodDefinition("pasta", "意大利面"),
        FoodDefinition("fried_chicken", "炸鸡"), FoodDefinition("roast_duck", "烤鸭"), FoodDefinition("grilled_meat", "烤肉"), FoodDefinition("sausage", "香肠"),
        FoodDefinition("kungpao_chicken", "宫保鸡丁"),
        FoodDefinition("yellow_braised_chicken", "黄焖鸡"), FoodDefinition("braised_pork", "红烧肉"), FoodDefinition("char_siu_rice", "叉烧饭"), FoodDefinition("sweet_sour_pork", "糖醋里脊"),
        FoodDefinition("guobaorou", "锅包肉"), FoodDefinition("fish_flavored_pork", "鱼香肉丝"), FoodDefinition("mapo_tofu", "麻婆豆腐"),
        FoodDefinition("tomato_egg", "番茄炒蛋"), FoodDefinition("fried_egg", "煎蛋"), FoodDefinition("steamed_fish", "清蒸鱼"),
        FoodDefinition("grilled_fish", "烤鱼"), FoodDefinition("barbecue", "烧烤"), FoodDefinition("hotpot", "火锅"),
        FoodDefinition("malatang", "麻辣烫"), FoodDefinition("spicy_pot", "麻辣香锅"), FoodDefinition("sushi", "寿司"), FoodDefinition("shrimp", "虾"), FoodDefinition("crab", "螃蟹"),
        FoodDefinition("apple", "苹果"), FoodDefinition("banana", "香蕉"), FoodDefinition("pear", "梨"),
        FoodDefinition("peach", "桃子"), FoodDefinition("mango", "芒果"), FoodDefinition("pineapple", "菠萝"),
        FoodDefinition("kiwi", "猕猴桃"), FoodDefinition("blueberry", "蓝莓"), FoodDefinition("cherry", "樱桃"),
        FoodDefinition("strawberry", "草莓"), FoodDefinition("watermelon", "西瓜"), FoodDefinition("melon", "甜瓜"),
        FoodDefinition("orange", "橙子"), FoodDefinition("grape", "葡萄"), FoodDefinition("dragon_fruit", "火龙果"),
        FoodDefinition("potato", "烤土豆"), FoodDefinition("mushroom", "黄油蘑菇"), FoodDefinition("tofu", "烧豆腐"),
        FoodDefinition("salad", "鸡肉沙拉"), FoodDefinition("cake", "蛋糕"), FoodDefinition("egg_tart", "蛋挞"), FoodDefinition("cheesecake", "芝士蛋糕"), FoodDefinition("fried_yogurt", "炒酸奶"),
        FoodDefinition("cookie", "饼干"),
        FoodDefinition("donut", "甜甜圈"), FoodDefinition("pudding", "布丁"), FoodDefinition("jelly", "果冻"),
        FoodDefinition("ice_cream", "冰淇淋"), FoodDefinition("chips", "薯条"), FoodDefinition("popcorn", "爆米花"),
        FoodDefinition("milk", "牛奶"), FoodDefinition("yogurt", "酸奶"), FoodDefinition("milk_tea", "奶茶"),
        FoodDefinition("juice", "果汁"), FoodDefinition("coffee", "咖啡"), FoodDefinition("tea", "茶"), FoodDefinition("soda", "汽水")
    )

    /** Kept only so pre-2.8.7 feeding records continue to display correctly. */
    private val legacyItems = listOf(
        FoodDefinition("rice", "米饭"), FoodDefinition("congee", "粥"), FoodDefinition("noodles", "面条"), FoodDefinition("mantou", "馒头"),
        FoodDefinition("chicken", "鸡肉"), FoodDefinition("beef", "牛肉"), FoodDefinition("pork", "猪肉"), FoodDefinition("duck", "鸭肉"),
        FoodDefinition("lamb", "羊肉"), FoodDefinition("sausage", "香肠"), FoodDefinition("fish", "鱼"), FoodDefinition("shrimp", "虾"),
        FoodDefinition("crab", "螃蟹"), FoodDefinition("shellfish", "贝类"), FoodDefinition("egg", "鸡蛋"), FoodDefinition("tofu", "豆腐"),
        FoodDefinition("cheese", "奶酪"), FoodDefinition("other", "其他食物")
    )
    private val allItems = items + legacyItems
    private val byKey = allItems.associateBy { it.key }
    private val byName = allItems.associateBy { it.name }
    fun find(keyOrName: String): FoodDefinition = byKey[keyOrName] ?: byName[keyOrName] ?: byKey.getValue("other")
    fun promptList(): String = items.joinToString("、") { "${it.key}=${it.name}" }
}

data class FoodRecognitionResult(
    val isFood: Boolean,
    val key: String,
    val name: String,
    val candidates: List<String>,
    val confidence: Float,
    val unsafe: Boolean,
    val provider: String,
    val model: String
)

data class PendingFoodRecognition(
    val photoPath: String,
    val sourceType: String,
    val result: FoodRecognitionResult? = null,
    val fallbackReason: String? = null
)

data class FoodAiConfig(val apiKey: String, val baseUrl: String, val model: String) {
    val configured get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

class FoodAiConfigStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("food_ai_config", Context.MODE_PRIVATE)
    private val alias = "yantu_food_ai_key"

    fun effective(): FoodAiConfig {
        val overrideKey = decrypt(prefs.getString("key", null)).orEmpty()
        return FoodAiConfig(
            overrideKey.ifBlank { BuildConfig.QWEN_API_KEY },
            prefs.getString("base", null).orEmpty().ifBlank { BuildConfig.QWEN_BASE_URL },
            prefs.getString("model", null).orEmpty().ifBlank { BuildConfig.QWEN_MODEL }
        )
    }

    fun hasOverride(): Boolean = prefs.contains("key")

    fun save(apiKey: String, baseUrl: String, model: String) {
        require(apiKey.isNotBlank()) { "API Key不能为空" }
        require(baseUrl.startsWith("https://")) { "接口地址必须使用HTTPS" }
        prefs.edit().putString("key", encrypt(apiKey.trim())).putString("base", baseUrl.trim())
            .putString("model", model.trim()).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val packed = cipher.iv + cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(value: String?): String? = runCatching {
        if (value == null) return null
        val packed = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, packed.copyOfRange(0, 12)))
        String(cipher.doFinal(packed.copyOfRange(12, packed.size)))
    }.getOrNull()
}

data class PreparedFoodImage(val uploadBytes: ByteArray, val thumbnailPath: String)

object FoodImageStore {
    fun prepare(context: Context, uri: Uri): PreparedFoodImage {
        val bitmap = decode(context, uri)
        val upload = scale(bitmap, 1024)
        val uploadBytes = ByteArrayOutputStream().use { out ->
            var quality = 86
            upload.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 1_000_000 && quality > 50) {
                out.reset(); quality -= 8; upload.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            out.toByteArray()
        }
        val thumb = scale(upload, 512)
        val directory = File(context.filesDir, "pet_food").apply { mkdirs() }
        val file = File(directory, "food_${System.currentTimeMillis()}_${UUID.randomUUID()}.webp")
        file.outputStream().use { out ->
            val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            thumb.compress(format, 78, out)
        }
        File(context.cacheDir, "food_camera").listFiles()?.forEach { it.delete() }
        if (bitmap !== upload) bitmap.recycle()
        if (upload !== thumb) upload.recycle()
        thumb.recycle()
        return PreparedFoodImage(uploadBytes, file.absolutePath)
    }

    fun delete(path: String?) { path?.let { runCatching { File(it).delete() } } }
    fun clear(context: Context) { File(context.filesDir, "pet_food").listFiles()?.forEach { it.delete() } }

    private fun decode(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val max = maxOf(info.size.width, info.size.height)
                if (max > 1600) decoder.setTargetSize((info.size.width * 1600f / max).roundToInt(), (info.size.height * 1600f / max).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        @Suppress("DEPRECATION")
        val raw = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val orientation = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        val degrees = when (orientation) { ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f }
        if (degrees == 0f) return raw
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { raw.recycle() }
    }

    private fun scale(source: Bitmap, maxSide: Int): Bitmap {
        val max = maxOf(source.width, source.height)
        if (max <= maxSide) return source
        val ratio = maxSide.toFloat() / max
        return Bitmap.createScaledBitmap(source, (source.width * ratio).roundToInt(), (source.height * ratio).roundToInt(), true)
    }
}

class QwenFoodRecognizer {
    fun test(config: FoodAiConfig) {
        require(config.configured) { "尚未配置通义千问API" }
        val body = JSONObject().put("model", config.model).put("temperature", 0).put("max_tokens", 8)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "只回复OK")))
        val connection = URL(qwenChatCompletionsUrl(config.baseUrl)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 15_000; connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            val response = connection.responseText(code)
            require(code in 200..299) { qwenErrorMessage(code, response) }
        } finally { connection.disconnect() }
    }

    fun recognize(image: ByteArray, config: FoodAiConfig): FoodRecognitionResult {
        require(config.configured) { "尚未配置通义千问API" }
        val prompt = """判断照片是否为食物，并从以下目录选择最接近的一项：${FoodCatalog.promptList()}。
只返回JSON：{"isFood":true,"foodKey":"rice","displayName":"米饭","confidence":0.95,"candidates":["rice","fried_rice"],"unsafe":false}。
只有主要食物是巧克力或明显含巧克力时unsafe才为true；其他所有食物unsafe必须为false。无法确定时foodKey用other。"""
        val content = JSONArray().put(JSONObject().put("type", "text").put("text", prompt)).put(
            JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,${Base64.encodeToString(image, Base64.NO_WRAP)}"))
        )
        val body = JSONObject().put("model", config.model).put("temperature", 0).put("messages", JSONArray().put(
            JSONObject().put("role", "user").put("content", content)
        )).put("response_format", JSONObject().put("type", "json_object"))
        val connection = URL(qwenChatCompletionsUrl(config.baseUrl)).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"; connection.connectTimeout = 20_000; connection.readTimeout = 30_000
            connection.doOutput = true; connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            val response = connection.responseText(code)
            require(code in 200..299) { qwenErrorMessage(code, response) }
            val text = JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(text)
            val isFood = parsed.optBoolean("isFood", false)
            val selected = FoodCatalog.find(parsed.optString("foodKey", "other"))
            val candidates = parsed.optJSONArray("candidates")?.let { array -> (0 until array.length()).map { FoodCatalog.find(array.optString(it)).key }.distinct().take(4) }.orEmpty()
            FoodRecognitionResult(isFood, selected.key, selected.name, (listOf(selected.key) + candidates).distinct(), parsed.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f), selected.unsafe, "QWEN", config.model)
        } finally { connection.disconnect() }
    }
}

internal fun qwenChatCompletionsUrl(rawBaseUrl: String): String {
    var base = rawBaseUrl.trim().trimEnd('/')
    if (base.endsWith("/chat/completions")) return base
    if (base == "https://dashscope.aliyuncs.com" || base == "https://dashscope-intl.aliyuncs.com") {
        base += "/compatible-mode/v1"
    }
    return "$base/chat/completions"
}

private fun HttpURLConnection.responseText(code: Int): String {
    val stream = if (code in 200..299) inputStream else errorStream
    return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
}

private fun qwenErrorMessage(code: Int, response: String): String {
    val detail = runCatching {
        val json = JSONObject(response)
        val error = json.optJSONObject("error")
        error?.optString("message").orEmpty().ifBlank { json.optString("message") }
    }.getOrDefault("")
    val hint = when (code) {
        401, 403 -> "API Key无效或没有该模型权限"
        404 -> "接口地址或模型名称不正确"
        429 -> "调用额度已用完或触发限流"
        else -> "通义千问服务返回错误"
    }
    return if (detail.isBlank()) "$hint（$code）" else "$hint（$code）：${detail.take(160)}"
}
