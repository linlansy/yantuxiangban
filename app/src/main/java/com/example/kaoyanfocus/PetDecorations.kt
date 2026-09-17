package com.example.kaoyanfocus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.kaoyanfocus.data.PetProfile

/** Scalable, theme-aware room decorations drawn behind the pet. */
@Composable
fun BoxScope.PolishedPetFurniture(profile: PetProfile?) {
    val placed = profile?.placedFurnitureCsv.orEmpty().split(',').filter(String::isNotBlank).toSet()
    if (placed.isEmpty()) return
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline

    Canvas(Modifier.matchParentSize()) {
        val floorY = size.height * .88f
        drawLine(outline.copy(alpha = .15f), Offset(size.width * .05f, floorY), Offset(size.width * .95f, floorY), 2f)
        drawOval(outline.copy(alpha = .08f), Offset(size.width * .08f, floorY - 3f), Size(size.width * .84f, size.height * .045f))
        if ("furniture_books" in placed) drawBookStack(Offset(size.width * .13f, size.height * .79f), size.width * .18f, primary, secondary, surface)
        if ("furniture_lamp" in placed) drawStudyLamp(Offset(size.width * .14f, size.height * .39f), size.width * .17f, primary, secondary)
        if ("furniture_clock" in placed) drawQuietClock(Offset(size.width * .86f, size.height * .28f), size.width * .082f, primary, surface, outline)
        if ("furniture_plant" in placed) drawPlant(Offset(size.width * .87f, size.height * .78f), size.width * .16f, primary, secondary)
        if ("furniture_star_globe" in placed) drawStarGlobe(Offset(size.width * .86f, size.height * .54f), size.width * .19f, primary, secondary, surface)
        if ("furniture_target_board" in placed) drawTargetBoard(Offset(size.width * .14f, size.height * .57f), size.width * .19f, primary, secondary, surface)
        if ("furniture_cloud_cushion" in placed) drawCloudCushion(Offset(size.width * .50f, size.height * .82f), size.width * .31f, primary, surface)
        if ("furniture_goal_trophy" in placed) drawGoalTrophy(Offset(size.width * .73f, size.height * .80f), size.width * .13f, primary, secondary)
    }
}

private fun DrawScope.drawCloudCushion(center: Offset, width: Float, primary: Color, surface: Color) {
    val cloud = Color(0xFFDCEEFF)
    val shade = Color(0xFF9FC8E8)
    drawOval(primary.copy(alpha = .10f), Offset(center.x - width * .55f, center.y + width * .12f), Size(width * 1.10f, width * .18f))
    drawRoundRect(shade.copy(alpha = .72f), Offset(center.x - width * .46f, center.y - width * .05f), Size(width * .92f, width * .28f), CornerRadius(width * .14f))
    drawCircle(cloud, width * .22f, Offset(center.x - width * .25f, center.y - width * .05f))
    drawCircle(cloud, width * .28f, Offset(center.x, center.y - width * .13f))
    drawCircle(cloud, width * .21f, Offset(center.x + width * .27f, center.y - width * .04f))
    drawRoundRect(cloud, Offset(center.x - width * .43f, center.y - width * .07f), Size(width * .86f, width * .25f), CornerRadius(width * .12f))
    drawArc(surface.copy(alpha = .82f), 205f, 42f, false, Offset(center.x - width * .28f, center.y - width * .08f), Size(width * .56f, width * .24f), style = Stroke(width * .018f))
}

private fun DrawScope.drawGoalTrophy(center: Offset, width: Float, primary: Color, secondary: Color) {
    val gold = Color(0xFFFFC857)
    val deepGold = Color(0xFFD99A28)
    drawOval(primary.copy(alpha = .10f), Offset(center.x - width * .52f, center.y + width * .50f), Size(width * 1.04f, width * .16f))
    drawPath(Path().apply {
        moveTo(center.x - width * .34f, center.y - width * .48f)
        lineTo(center.x + width * .34f, center.y - width * .48f)
        quadraticTo(center.x + width * .28f, center.y + width * .10f, center.x, center.y + width * .18f)
        quadraticTo(center.x - width * .28f, center.y + width * .10f, center.x - width * .34f, center.y - width * .48f)
        close()
    }, gold)
    drawArc(deepGold, 85f, 190f, false, Offset(center.x - width * .62f, center.y - width * .39f), Size(width * .50f, width * .55f), style = Stroke(width * .09f, cap = StrokeCap.Round))
    drawArc(deepGold, -95f, 190f, false, Offset(center.x + width * .12f, center.y - width * .39f), Size(width * .50f, width * .55f), style = Stroke(width * .09f, cap = StrokeCap.Round))
    drawLine(deepGold, Offset(center.x, center.y + width * .16f), Offset(center.x, center.y + width * .42f), width * .09f, StrokeCap.Round)
    drawRoundRect(secondary, Offset(center.x - width * .34f, center.y + width * .38f), Size(width * .68f, width * .17f), CornerRadius(width * .05f))
    drawCircle(Color.White.copy(alpha = .82f), width * .055f, Offset(center.x - width * .11f, center.y - width * .27f))
}

private fun DrawScope.drawBookStack(center: Offset, width: Float, primary: Color, secondary: Color, paper: Color) {
    val h = width * .18f
    val left = center.x - width / 2
    val colors = listOf(secondary, Color(0xFFE58B7B), primary)
    repeat(3) { index ->
        val y = center.y + h * (1 - index)
        val inset = if (index == 1) width * .05f else 0f
        drawRoundRect(colors[index], Offset(left + inset, y), Size(width - inset, h * .82f), CornerRadius(h * .28f))
        drawRoundRect(paper.copy(alpha = .9f), Offset(left + inset + width * .08f, y + h * .16f), Size(width * .68f, h * .47f), CornerRadius(h * .12f))
        drawLine(colors[index].copy(alpha = .55f), Offset(left + inset + width * .13f, y + h * .18f), Offset(left + inset + width * .13f, y + h * .59f), 2f)
    }
    drawPath(Path().apply {
        moveTo(center.x + width * .18f, center.y + h * 1.04f)
        lineTo(center.x + width * .30f, center.y + h * 1.04f)
        lineTo(center.x + width * .25f, center.y + h * 1.60f)
        lineTo(center.x + width * .20f, center.y + h * 1.40f)
        close()
    }, Color(0xFFFFD56A))
}

private fun DrawScope.drawStudyLamp(center: Offset, width: Float, primary: Color, secondary: Color) {
    val glow = Color(0xFFFFD978)
    drawCircle(glow.copy(alpha = .10f), width * .64f, Offset(center.x + width * .18f, center.y + width * .07f))
    drawCircle(glow.copy(alpha = .16f), width * .40f, Offset(center.x + width * .18f, center.y + width * .07f))
    drawLine(primary, Offset(center.x - width * .16f, center.y + width * .55f), Offset(center.x, center.y), width * .065f, StrokeCap.Round)
    drawLine(primary, Offset(center.x, center.y), Offset(center.x + width * .23f, center.y - width * .19f), width * .065f, StrokeCap.Round)
    drawPath(Path().apply {
        moveTo(center.x + width * .02f, center.y - width * .26f)
        quadraticTo(center.x + width * .27f, center.y - width * .47f, center.x + width * .48f, center.y - width * .19f)
        lineTo(center.x + width * .30f, center.y + width * .08f)
        quadraticTo(center.x + width * .10f, center.y - width * .02f, center.x + width * .02f, center.y - width * .26f)
        close()
    }, secondary)
    drawOval(primary.copy(alpha = .9f), Offset(center.x - width * .42f, center.y + width * .50f), Size(width * .64f, width * .14f))
    drawCircle(glow, width * .055f, Offset(center.x + width * .22f, center.y - width * .02f))
}

private fun DrawScope.drawQuietClock(center: Offset, radius: Float, primary: Color, face: Color, outline: Color) {
    drawCircle(primary.copy(alpha = .12f), radius * 1.18f, center)
    drawCircle(face, radius, center)
    drawCircle(primary, radius, center, style = Stroke(radius * .12f))
    repeat(12) { index ->
        val angle = Math.toRadians(index * 30.0 - 90.0)
        val start = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius * .72f, center.y + kotlin.math.sin(angle).toFloat() * radius * .72f)
        val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius * .82f, center.y + kotlin.math.sin(angle).toFloat() * radius * .82f)
        drawLine(outline.copy(alpha = .75f), start, end, if (index % 3 == 0) 3f else 1.6f, StrokeCap.Round)
    }
    drawLine(primary, center, Offset(center.x, center.y - radius * .50f), radius * .075f, StrokeCap.Round)
    drawLine(primary, center, Offset(center.x + radius * .39f, center.y + radius * .18f), radius * .075f, StrokeCap.Round)
    drawCircle(primary, radius * .09f, center)
}

private fun DrawScope.drawPlant(center: Offset, width: Float, primary: Color, secondary: Color) {
    val green = Color(0xFF5B9A78)
    val lightGreen = Color(0xFF83B992)
    drawLine(green, center, Offset(center.x, center.y - width * .56f), width * .035f, StrokeCap.Round)
    listOf(-.30f to -.38f, .28f to -.44f, -.22f to -.60f, .18f to -.70f, 0f to -.84f).forEachIndexed { index, (dx, dy) ->
        val leafCenter = Offset(center.x + width * dx, center.y + width * dy)
        drawOval(if (index % 2 == 0) green else lightGreen, Offset(leafCenter.x - width * .16f, leafCenter.y - width * .09f), Size(width * .32f, width * .18f))
        drawLine(Color.White.copy(alpha = .28f), Offset(leafCenter.x - width * .10f, leafCenter.y), Offset(leafCenter.x + width * .10f, leafCenter.y), 1.5f)
    }
    drawPath(Path().apply {
        moveTo(center.x - width * .31f, center.y - width * .03f)
        lineTo(center.x + width * .31f, center.y - width * .03f)
        lineTo(center.x + width * .22f, center.y + width * .38f)
        quadraticTo(center.x, center.y + width * .48f, center.x - width * .22f, center.y + width * .38f)
        close()
    }, secondary)
    drawLine(primary.copy(alpha = .45f), Offset(center.x - width * .23f, center.y + width * .14f), Offset(center.x + width * .23f, center.y + width * .14f), width * .025f)
}

private fun DrawScope.drawStarGlobe(center: Offset, width: Float, primary: Color, secondary: Color, surface: Color) {
    val radius = width * .34f
    drawCircle(primary.copy(alpha = .10f), radius * 1.35f, center)
    drawCircle(Color(0xFF293B75).copy(alpha = .88f), radius, center)
    drawCircle(Color(0xFF7399D4).copy(alpha = .45f), radius, center, style = Stroke(width * .045f))
    listOf(-.38f to -.24f, .20f to -.38f, .42f to .12f, -.12f to .35f, -.46f to .22f).forEachIndexed { index, (dx, dy) ->
        drawCircle(if (index == 1) Color(0xFFFFD66B) else surface, width * if (index == 1) .032f else .022f, Offset(center.x + radius * dx, center.y + radius * dy))
    }
    drawArc(Color(0xFFB8D6F1).copy(alpha = .40f), 195f, 100f, false, Offset(center.x - radius * .78f, center.y - radius * .70f), Size(radius * 1.56f, radius * 1.40f), style = Stroke(width * .018f))
    drawLine(secondary, Offset(center.x, center.y + radius), Offset(center.x, center.y + radius * 1.44f), width * .045f, StrokeCap.Round)
    drawOval(secondary, Offset(center.x - width * .25f, center.y + radius * 1.36f), Size(width * .50f, width * .10f))
}

private fun DrawScope.drawTargetBoard(center: Offset, width: Float, primary: Color, secondary: Color, paper: Color) {
    val height = width * .78f
    drawRoundRect(Color(0xFFC99A67), Offset(center.x - width / 2, center.y - height / 2), Size(width, height), CornerRadius(width * .08f))
    drawRoundRect(secondary.copy(alpha = .38f), Offset(center.x - width * .43f, center.y - height * .39f), Size(width * .86f, height * .78f), CornerRadius(width * .045f))
    drawRoundRect(paper.copy(alpha = .94f), Offset(center.x - width * .30f, center.y - height * .27f), Size(width * .60f, height * .48f), CornerRadius(width * .035f))
    drawCircle(Color(0xFFE75D68), width * .035f, Offset(center.x, center.y - height * .23f))
    drawLine(primary, Offset(center.x - width * .20f, center.y - height * .08f), Offset(center.x + width * .20f, center.y - height * .08f), width * .025f, StrokeCap.Round)
    drawLine(primary.copy(alpha = .55f), Offset(center.x - width * .20f, center.y + height * .04f), Offset(center.x + width * .10f, center.y + height * .04f), width * .02f, StrokeCap.Round)
    drawPath(Path().apply {
        moveTo(center.x + width * .10f, center.y + height * .23f)
        lineTo(center.x + width * .17f, center.y + height * .35f)
        lineTo(center.x + width * .30f, center.y + height * .36f)
        lineTo(center.x + width * .20f, center.y + height * .44f)
        lineTo(center.x + width * .23f, center.y + height * .57f)
        lineTo(center.x + width * .10f, center.y + height * .50f)
        lineTo(center.x - width * .03f, center.y + height * .57f)
        lineTo(center.x, center.y + height * .44f)
        lineTo(center.x - width * .10f, center.y + height * .36f)
        lineTo(center.x + width * .03f, center.y + height * .35f)
        close()
    }, Color(0xFFFFD66B))
}
