package com.example.kaoyanfocus

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.kaoyanfocus.data.PetProfile
import com.example.kaoyanfocus.data.PetUnlock
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import android.graphics.BitmapFactory
import java.io.File

private data class PetSheet(
    @DrawableRes val resource: Int,
    val frames: Int = 8,
    val columns: Int = 4,
    val rows: Int = 2,
    val durationMillis: Long = PetMotionTiming.WAVE,
    // Every generated sheet is opaque.  Remove its border-connected studio /
    // checkerboard background before cropping individual frames so a neighbor
    // frame or a rectangular corner never shows during an animation.
    val stripCheckerboard: Boolean = true
)

object PetMotionTiming {
    const val TOUCH = 2_800L
    const val WAVE = 3_200L
    const val READ = 7_200L
    const val READING_REST = 1_400L
    const val STRETCH = 3_600L
    const val CELEBRATE = 3_600L
    const val DRINK = 4_200L
    const val LIE = 5_000L
    const val EAT = 2_000L
    const val REFUSE = 2_000L

    fun forAction(action: String): Long = when (action) {
        "action_wave" -> WAVE
        "action_read" -> READ
        "action_stretch" -> STRETCH
        "action_celebrate" -> CELEBRATE
        "action_drink" -> DRINK
        "action_lie" -> LIE
        "action_eat" -> EAT
        "action_refuse" -> REFUSE
        else -> TOUCH
    }
}

@Composable
fun AnimatedPet(
    profile: PetProfile,
    unlocks: List<PetUnlock>,
    motion: PetMotionUi,
    studying: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val unlockedKeys = remember(unlocks) { unlocks.mapTo(mutableSetOf()) { it.itemKey } }
    val baseAction = if (studying && "action_read" in unlockedKeys) "action_read" else "action_idle"
    val action = motion.actionKey ?: baseAction
    val sheet = sheetFor(action, profile.equippedOutfit)
    val outfitCell = outfitCell(profile.equippedOutfit)
    val proceduralMotion = sheet == null && action != "action_idle" && (outfitCell != null || action in setOf("action_eat", "action_refuse"))
    val characterAspect = when {
        sheet != null -> .75f
        outfitCell != null -> 1.5f
        else -> 1207f / 1303f
    }
    var progress by remember(action, motion.token) { mutableFloatStateOf(0f) }
    val displayedFrame = sheet?.let { (progress * it.frames).toInt().coerceIn(0, it.frames - 1) } ?: 0

    LaunchedEffect(action, motion.token) {
        progress = 0f
        if (sheet != null) {
            val frameDelay = (sheet.durationMillis / sheet.frames).coerceAtLeast(32)
            do {
                repeat(sheet.frames) { frame ->
                    progress = frame.toFloat() / sheet.frames
                    delay(frameDelay)
                }
                if (action == "action_read") delay(PetMotionTiming.READING_REST)
            } while (action == "action_read")
            progress = (sheet.frames - 1f) / sheet.frames
        } else if (proceduralMotion) {
            val frames = 24
            val duration = PetMotionTiming.forAction(action)
            do {
                repeat(frames) { frame ->
                    progress = frame.toFloat() / frames
                    delay((duration / frames).coerceAtLeast(32))
                }
                if (action == "action_read") delay(PetMotionTiming.READING_REST)
            } while (action == "action_read")
        }
    }

    Box(
        modifier
            .semantics { contentDescription = "摸摸${profile.name.ifBlank { "宠物" }}"; role = Role.Button }
            .pointerInput(profile.id) {
                detectTapGestures {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTap()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val phase = sin(progress * 2f * PI.toFloat())
        Box(Modifier.fillMaxSize().graphicsLayer {
            if (proceduralMotion) when (action) {
                "action_wave" -> { rotationZ = phase * 8f; translationX = phase * 7f }
                "action_read" -> translationY = abs(phase) * 7f
                "action_stretch" -> { scaleY = 1f + phase * .075f; scaleX = 1f - phase * .035f }
                "action_celebrate" -> { translationY = -abs(phase) * 24f; rotationZ = phase * 5f }
                "action_drink" -> rotationZ = phase * 7f
                "action_lie" -> { translationY = abs(phase) * 12f; rotationZ = 68f + phase * 4f; scaleX = .82f; scaleY = .82f }
                "action_eat" -> { translationY = abs(phase) * 9f; scaleY = 1f - abs(phase) * .025f }
                "action_refuse" -> rotationZ = phase * 5f
                else -> { scaleX = 1f + phase * .018f; scaleY = 1f + phase * .018f }
            }
        }) {
            if (sheet != null) {
                val bitmap = remember(sheet.resource, sheet.stripCheckerboard) {
                    if (sheet.stripCheckerboard) checkerboardTransparentBitmap(context, sheet.resource)
                    else ImageBitmap.imageResource(context.resources, sheet.resource)
                }
                Canvas(Modifier.fillMaxSize()) {
                    drawSheetFrame(bitmap, displayedFrame, sheet.columns, sheet.rows)
                }
            } else {
                val resource = if (outfitCell == null) R.drawable.pet_idle else R.drawable.pet_outfit_sheet
                val bitmap = ImageBitmap.imageResource(resource)
                Canvas(Modifier.fillMaxSize()) {
                    if (outfitCell == null) drawSingleImage(bitmap) else drawSheetFrame(bitmap, outfitCell, 2, 2)
                }
            }
            PetAccessoryOverlay(profile, characterAspect)
        }

        if (proceduralMotion) PetActionPropOverlay(action, progress)

        if (action == "action_eat") {
            val foodBitmap = remember(motion.foodKey, motion.foodPhotoPath) {
                foodArtworkResource(motion.foodKey)?.let { BitmapFactory.decodeResource(context.resources, it)?.asImageBitmap() }
                    ?: motion.foodPhotoPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
            }
            Canvas(Modifier.fillMaxSize()) {
                val bowlCenter = Offset(size.width * .5f, size.height * .80f)
                val radius = size.minDimension * .10f
                drawOval(Color(0xFF8CC9F2), topLeft = Offset(bowlCenter.x - radius * 1.25f, bowlCenter.y - radius * .25f), size = Size(radius * 2.5f, radius * .82f))
                drawOval(Color(0xFFE7F6FF), topLeft = Offset(bowlCenter.x - radius, bowlCenter.y - radius * .42f), size = Size(radius * 2f, radius * .58f))
                if (foodBitmap != null) drawImage(foodBitmap, dstOffset = IntOffset((bowlCenter.x - radius * .55f).toInt(), (bowlCenter.y - radius * .62f).toInt()), dstSize = IntSize((radius * 1.1f).toInt(), (radius * .72f).toInt()))
                else drawCircle(Color(0xFFF2B66D), radius * .38f, Offset(bowlCenter.x, bowlCenter.y - radius * .30f))
            }
        }

        AnimatedVisibility(
            visible = motion.caption != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp
            ) {
                Text(motion.caption.orEmpty(), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PetAccessoryOverlay(profile: PetProfile, characterAspect: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val characterWidth = minOf(w, h * characterAspect)
        val characterHeight = characterWidth / characterAspect
        val characterLeft = (w - characterWidth) / 2f
        val characterTop = (h - characterHeight) / 2f

        if (profile.equippedHeadAccessory.isNotBlank()) {
            val gold = Color(0xFFFFD369)
            val center = Offset(characterLeft + characterWidth * .53f, characterTop + characterHeight * .205f)
            val radius = characterHeight * .034f
            drawCircle(gold.copy(alpha = .16f), radius * 1.75f, center)
            drawPath(Path().apply {
                repeat(10) { index ->
                    val angle = -Math.PI / 2 + index * Math.PI / 5
                    val r = if (index % 2 == 0) radius else radius * .44f
                    val x = center.x + kotlin.math.cos(angle).toFloat() * r
                    val y = center.y + kotlin.math.sin(angle).toFloat() * r
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }, gold)
            drawCircle(Color.White.copy(alpha = .78f), radius * .16f, Offset(center.x - radius * .20f, center.y - radius * .25f))
            drawLine(Color(0xFF668CCB), Offset(center.x - radius * .78f, center.y + radius * .62f), Offset(center.x + radius * .72f, center.y + radius * .62f), radius * .20f, StrokeCap.Round)
        }
    }
}

private fun sheetFor(action: String, outfit: String): PetSheet? {
    if (outfit == "outfit_school") {
        when (action) {
            "action_pet", "action_pet_reading" -> return PetSheet(R.drawable.pet_sprite_school_touch, durationMillis = PetMotionTiming.TOUCH)
            "action_wave" -> return PetSheet(R.drawable.pet_sprite_school_wave, durationMillis = PetMotionTiming.WAVE)
            "action_read" -> return PetSheet(R.drawable.pet_sprite_school_read, durationMillis = PetMotionTiming.READ)
            "action_stretch" -> return PetSheet(R.drawable.pet_sprite_school_stretch, durationMillis = PetMotionTiming.STRETCH)
            "action_celebrate" -> return PetSheet(R.drawable.pet_sprite_school_celebrate, durationMillis = PetMotionTiming.CELEBRATE)
            "action_drink" -> return PetSheet(R.drawable.pet_sprite_school_drink, durationMillis = PetMotionTiming.DRINK)
            "action_lie" -> return PetSheet(R.drawable.pet_sprite_school_lie, durationMillis = PetMotionTiming.LIE)
        }
    }
    if (outfit == "outfit_star_pajamas") {
        when (action) {
            "action_pet", "action_pet_reading" -> return PetSheet(R.drawable.pet_sprite_pajamas_touch_v2, durationMillis = PetMotionTiming.TOUCH, stripCheckerboard = true)
            "action_wave" -> return PetSheet(R.drawable.pet_sprite_pajamas_wave_v2, durationMillis = PetMotionTiming.WAVE, stripCheckerboard = true)
            "action_read" -> return PetSheet(R.drawable.pet_sprite_pajamas_read_v2, durationMillis = PetMotionTiming.READ, stripCheckerboard = true)
            "action_stretch" -> return PetSheet(R.drawable.pet_sprite_pajamas_stretch_v2, durationMillis = PetMotionTiming.STRETCH, stripCheckerboard = true)
            "action_celebrate" -> return PetSheet(R.drawable.pet_sprite_pajamas_celebrate_v2, durationMillis = PetMotionTiming.CELEBRATE, stripCheckerboard = true)
            "action_drink" -> return PetSheet(R.drawable.pet_sprite_pajamas_drink_v2, durationMillis = PetMotionTiming.DRINK, stripCheckerboard = true)
            "action_lie" -> return PetSheet(R.drawable.pet_sprite_pajamas_lie_v2, durationMillis = PetMotionTiming.LIE, stripCheckerboard = true)
        }
    }
    if (outfit == "outfit_library") {
        when (action) {
            "action_pet", "action_pet_reading" -> return PetSheet(R.drawable.pet_sprite_library_touch_v2, durationMillis = PetMotionTiming.TOUCH, stripCheckerboard = true)
            "action_wave" -> return PetSheet(R.drawable.pet_sprite_library_wave_v2, durationMillis = PetMotionTiming.WAVE, stripCheckerboard = true)
            "action_read" -> return PetSheet(R.drawable.pet_sprite_library_read_v2, durationMillis = PetMotionTiming.READ, stripCheckerboard = true)
            "action_stretch" -> return PetSheet(R.drawable.pet_sprite_library_stretch_v2, durationMillis = PetMotionTiming.STRETCH, stripCheckerboard = true)
            "action_celebrate" -> return PetSheet(R.drawable.pet_sprite_library_celebrate_v2, durationMillis = PetMotionTiming.CELEBRATE, stripCheckerboard = true)
            "action_drink" -> return PetSheet(R.drawable.pet_sprite_library_drink_v2, durationMillis = PetMotionTiming.DRINK, stripCheckerboard = true)
            "action_lie" -> return PetSheet(R.drawable.pet_sprite_library_lie_v2, durationMillis = PetMotionTiming.LIE, stripCheckerboard = true)
        }
    }
    if (outfit in setOf("outfit_starry", "outfit_moonlight")) {
        when (action) {
            "action_pet", "action_pet_reading" -> return PetSheet(R.drawable.pet_sprite_starry_touch_v2, durationMillis = PetMotionTiming.TOUCH, stripCheckerboard = true)
            "action_wave" -> return PetSheet(R.drawable.pet_sprite_starry_wave_v2, durationMillis = PetMotionTiming.WAVE, stripCheckerboard = true)
            "action_read" -> return PetSheet(R.drawable.pet_sprite_starry_read_v2, durationMillis = PetMotionTiming.READ, stripCheckerboard = true)
            "action_stretch" -> return PetSheet(R.drawable.pet_sprite_starry_stretch_v2, durationMillis = PetMotionTiming.STRETCH, stripCheckerboard = true)
            "action_celebrate" -> return PetSheet(R.drawable.pet_sprite_starry_celebrate_v2, durationMillis = PetMotionTiming.CELEBRATE, stripCheckerboard = true)
            "action_drink" -> return PetSheet(R.drawable.pet_sprite_starry_drink_v2, durationMillis = PetMotionTiming.DRINK, stripCheckerboard = true)
            "action_lie" -> return PetSheet(R.drawable.pet_sprite_starry_lie_v2, durationMillis = PetMotionTiming.LIE, stripCheckerboard = true)
        }
    }
    if (outfitCell(outfit) != null) return null
    return when (action) {
    "action_wave" -> PetSheet(R.drawable.pet_sprite_wave, durationMillis = PetMotionTiming.WAVE)
    "action_read" -> PetSheet(R.drawable.pet_sprite_read, durationMillis = PetMotionTiming.READ)
    "action_pet", "action_pet_reading" -> PetSheet(R.drawable.pet_sprite_touch, durationMillis = PetMotionTiming.TOUCH)
    "action_celebrate" -> PetSheet(R.drawable.pet_sprite_celebrate, durationMillis = PetMotionTiming.CELEBRATE)
    "action_stretch" -> PetSheet(R.drawable.pet_sprite_stretch, durationMillis = PetMotionTiming.STRETCH)
    "action_drink" -> PetSheet(R.drawable.pet_sprite_drink, durationMillis = PetMotionTiming.DRINK)
    "action_lie" -> PetSheet(R.drawable.pet_sprite_lie, durationMillis = PetMotionTiming.LIE)
        else -> null
    }
}

private fun checkerboardTransparentBitmap(context: android.content.Context, @DrawableRes resource: Int): ImageBitmap {
    val bitmap = BitmapFactory.decodeResource(context.resources, resource).copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    if (pixels.any { it ushr 24 < 255 }) return bitmap.asImageBitmap()
    val visited = BooleanArray(pixels.size)
    val queue = IntArray(pixels.size)
    var head = 0
    var tail = 0
    fun offer(index: Int) {
        if (index !in pixels.indices || visited[index] || !isGeneratedCheckerboardPixel(pixels[index])) return
        visited[index] = true
        queue[tail++] = index
    }
    for (x in 0 until width) { offer(x); offer((height - 1) * width + x) }
    for (y in 0 until height) { offer(y * width); offer(y * width + width - 1) }
    while (head < tail) {
        val index = queue[head++]
        val x = index % width
        val y = index / width
        pixels[index] = pixels[index] and 0x00FFFFFF
        if (x > 0) offer(index - 1)
        if (x + 1 < width) offer(index + 1)
        if (y > 0) offer(index - width)
        if (y + 1 < height) offer(index + width)
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}

internal fun isGeneratedCheckerboardPixel(color: Int): Boolean {
    val red = color shr 16 and 0xFF
    val green = color shr 8 and 0xFF
    val blue = color and 0xFF
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    // Older school-uniform sheets use a dark grey studio gradient, while the
    // newer ones use a white checkerboard. Both are neutral, border-connected
    // backgrounds; the character itself is contained by its outline.
    return max - min <= 34
}

@Composable
private fun PetActionPropOverlay(action: String, progress: Float) {
    val phase = sin(progress * 2f * PI.toFloat())
    Canvas(Modifier.fillMaxSize()) {
        val unit = size.minDimension
        when (action) {
            "action_wave" -> {
                val center = Offset(size.width * .79f, size.height * (.31f - phase * .025f))
                drawCircle(Color(0xFFF8FBFF), unit * .055f, center)
                repeat(3) { i -> drawCircle(Color(0xFFE7F3FF), unit * .018f, Offset(center.x + (i - 1) * unit * .035f, center.y - unit * .045f)) }
            }
            "action_read" -> {
                val centerX = size.width * .50f
                val top = size.height * .72f
                val halfWidth = size.width * .17f
                val height = unit * .105f
                val blue = Color(0xFF547FC9)
                val pages = Color(0xFFFFFBED)
                val leftPage = Path().apply {
                    moveTo(centerX, top + unit * .015f)
                    quadraticBezierTo(centerX - halfWidth * .48f, top - unit * .018f, centerX - halfWidth, top)
                    lineTo(centerX - halfWidth * .94f, top + height)
                    quadraticBezierTo(centerX - halfWidth * .45f, top + height * .78f, centerX, top + height)
                    close()
                }
                val rightPage = Path().apply {
                    moveTo(centerX, top + unit * .015f)
                    quadraticBezierTo(centerX + halfWidth * .48f, top - unit * .018f, centerX + halfWidth, top)
                    lineTo(centerX + halfWidth * .94f, top + height)
                    quadraticBezierTo(centerX + halfWidth * .45f, top + height * .78f, centerX, top + height)
                    close()
                }
                drawPath(leftPage, pages)
                drawPath(rightPage, pages)
                val outline = androidx.compose.ui.graphics.drawscope.Stroke(unit * .012f)
                drawPath(leftPage, blue, style = outline)
                drawPath(rightPage, blue, style = outline)
                drawLine(Color(0xFFFFC94F), Offset(centerX, top + unit * .015f), Offset(centerX, top + height), unit * .010f)
            }
            "action_celebrate" -> repeat(6) { i ->
                val x = size.width * (.18f + i * .13f)
                val y = size.height * (.18f + ((i % 2) * .10f)) - abs(phase) * unit * .06f
                drawCircle(if (i % 2 == 0) Color(0xFFFFD45A) else Color(0xFF7FC7FF), unit * .022f, Offset(x, y))
            }
            "action_drink" -> {
                val x = size.width * .69f; val y = size.height * (.51f + phase * .018f)
                drawRoundRect(Color(0xFF8CC9F2), Offset(x, y), Size(unit * .12f, unit * .13f))
                drawCircle(Color(0xFFEAF8FF), unit * .037f, Offset(x + unit * .13f, y + unit * .065f), style = androidx.compose.ui.graphics.drawscope.Stroke(unit * .016f))
            }
            "action_lie" -> {
                repeat(3) { i ->
                    val scale = 1f + i * .24f
                    drawCircle(Color(0xFF8DB8F2).copy(alpha = .85f - i * .18f), unit * .015f * scale, Offset(size.width * (.69f + i * .06f), size.height * (.21f - i * .045f)))
                }
            }
        }
    }
}

private fun outfitCell(key: String): Int? = when (key) {
    "outfit_school" -> 0
    "outfit_star_pajamas" -> 1
    "outfit_library" -> 2
    "outfit_starry", "outfit_moonlight" -> 3
    else -> null
}

private fun DrawScope.drawSingleImage(bitmap: ImageBitmap) {
    val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
    val width = bitmap.width * scale
    val height = bitmap.height * scale
    drawImage(bitmap, dstOffset = IntOffset(((size.width - width) / 2).toInt(), ((size.height - height) / 2).toInt()), dstSize = IntSize(width.toInt(), height.toInt()))
}

private fun DrawScope.drawSheetFrame(bitmap: ImageBitmap, frame: Int, columns: Int, rows: Int) {
    val frameWidth = bitmap.width / columns
    val frameHeight = bitmap.height / rows
    val sourceX = (frame % columns) * frameWidth
    val sourceY = (frame / columns) * frameHeight
    val scale = minOf(size.width / frameWidth, size.height / frameHeight)
    val width = frameWidth * scale
    val height = frameHeight * scale
    drawImage(
        image = bitmap,
        srcOffset = IntOffset(sourceX, sourceY),
        srcSize = IntSize(frameWidth, frameHeight),
        dstOffset = IntOffset(((size.width - width) / 2).toInt(), ((size.height - height) / 2).toInt()),
        dstSize = IntSize(width.toInt(), height.toInt())
    )
}
