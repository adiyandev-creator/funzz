package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameTileThumbnail(
    gameId: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (gameId) {
            "spinner_battle" -> drawSpinnerBattleThumb(w, h)
            "paint_arena" -> drawPaintArenaThumb(w, h)
            "micro_racing" -> drawMicroRacingThumb(w, h)
            "reaction_battle" -> drawReactionBattleThumb(w, h)
            "tic_tac_toe" -> drawTicTacToeThumb(w, h)
            "memory_match" -> drawMemoryMatchThumb(w, h)
            "tug_of_war" -> drawTugOfWarThumb(w, h)
            "basketball_shots" -> drawBasketballThumb(w, h)
            else -> drawSpinnerBattleThumb(w, h)
        }
    }
}

private fun DrawScope.drawSpinnerBattleThumb(w: Float, h: Float) {
    // Purple arena background
    drawRect(Color(0xFF38254A))
    // Circular sand arena disk
    val diskCenter = Offset(w * 0.75f, h * 0.75f)
    val diskRadius = w * 0.65f
    drawCircle(Color(0xFFFCD385), radius = diskRadius, center = diskCenter)
    drawCircle(Color(0xFF261833), radius = diskRadius, center = diskCenter, style = Stroke(width = 8f))

    // Buzzsaw spinner 1 (Blue)
    drawBuzzsaw(Offset(w * 0.42f, h * 0.46f), 18f, Color(0xFF0EA5E9))
    // Buzzsaw spinner 2 (Cyan mini)
    drawBuzzsaw(Offset(w * 0.74f, h * 0.58f), 14f, Color(0xFF06B6D4))
}

private fun DrawScope.drawBuzzsaw(center: Offset, radius: Float, innerColor: Color) {
    val teeth = 8
    val path = Path()
    val outerR = radius * 1.35f
    val innerR = radius * 0.85f

    for (i in 0 until (teeth * 2)) {
        val angle = (i * Math.PI / teeth).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(path, color = Color(0xFF1E293B))
    drawPath(path, color = Color.Black, style = Stroke(width = 3f))
    drawCircle(color = innerColor, center = center, radius = radius * 0.55f)
    drawCircle(color = Color.White, center = center, radius = radius * 0.25f)
}

private fun DrawScope.drawPaintArenaThumb(w: Float, h: Float) {
    // Diagonal split: Cyan / Blue on top-left, Red/Coral on bottom-right
    val splitPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(0f, h)
        close()
    }
    drawRect(Color(0xFFEF4444))
    drawPath(splitPath, Color(0xFF0EA5E9))

    // White divider line
    drawLine(Color.White, Offset(0f, h), Offset(w, 0f), strokeWidth = 10f)

    // Blue paint roller on top
    drawRoller(Offset(w * 0.32f, h * 0.28f), angleDeg = 30f, color = Color(0xFF0284C7))
    // Red paint roller on bottom
    drawRoller(Offset(w * 0.70f, h * 0.72f), angleDeg = 210f, color = Color(0xFFDC2626))
}

private fun DrawScope.drawRoller(center: Offset, angleDeg: Float, color: Color) {
    val rollW = 34f
    val rollH = 16f
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - rollW / 2f - 2f, center.y - rollH / 2f - 2f),
        size = Size(rollW + 4f, rollH + 4f)
    )
    drawRect(
        color = color,
        topLeft = Offset(center.x - rollW / 2f, center.y - rollH / 2f),
        size = Size(rollW, rollH)
    )
    // Handle
    drawLine(
        color = Color(0xFF0F172A),
        start = Offset(center.x, center.y + rollH / 2f),
        end = Offset(center.x, center.y + rollH / 2f + 16f),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMicroRacingThumb(w: Float, h: Float) {
    // Dark asphalt track
    drawRect(Color(0xFF242730))

    // White dashed center line
    drawLine(
        color = Color.White,
        start = Offset(w * 0.35f, 0f),
        end = Offset(w * 0.35f, h * 0.6f),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    // Red & White corner curb
    drawRect(Color(0xFFEF4444), topLeft = Offset(w * 0.72f, h * 0.55f), size = Size(w * 0.28f, h * 0.45f))
    drawRect(Color.White, topLeft = Offset(w * 0.72f, h * 0.75f), size = Size(w * 0.28f, 20f))

    // Red racing car with black border
    val carX = w * 0.60f
    val carY = h * 0.40f
    drawRoundRect(
        color = Color.Black,
        topLeft = Offset(carX - 18f, carY - 26f),
        size = Size(36f, 52f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(carX - 15f, carY - 23f),
        size = Size(30f, 46f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )
    // Windshield
    drawRoundRect(
        color = Color(0xFF93C5FD),
        topLeft = Offset(carX - 10f, carY - 14f),
        size = Size(20f, 16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawReactionBattleThumb(w: Float, h: Float) {
    drawRect(Color(0xFF1E293B))

    // Lightning reflex / target circle
    drawCircle(Color(0xFF22C55E).copy(alpha = 0.25f), radius = w * 0.42f, center = Offset(w / 2f, h / 2f))
    drawCircle(Color(0xFF22C55E), radius = w * 0.32f, center = Offset(w / 2f, h / 2f))

    // Center Lightning Bolt (Yellow)
    val path = Path().apply {
        moveTo(w * 0.54f, h * 0.24f)
        lineTo(w * 0.36f, h * 0.54f)
        lineTo(w * 0.50f, h * 0.54f)
        lineTo(w * 0.46f, h * 0.76f)
        lineTo(w * 0.66f, h * 0.46f)
        lineTo(w * 0.52f, h * 0.46f)
        close()
    }
    drawPath(path, Color(0xFFFBBF24))
    drawPath(path, Color.White, style = Stroke(width = 3f))
}

private fun DrawScope.drawTicTacToeThumb(w: Float, h: Float) {
    drawRect(Color(0xFF1E1E2F))

    // Grid lines
    val p = 16f
    val step = (w - p * 2) / 3f
    val lineColor = Color(0xFF475569)

    for (i in 1..2) {
        drawLine(lineColor, Offset(p + i * step, p), Offset(p + i * step, h - p), strokeWidth = 5f)
        drawLine(lineColor, Offset(p, p + i * step), Offset(w - p, p + i * step), strokeWidth = 5f)
    }

    // X in cell (0, 0)
    val c0 = Offset(p + step / 2f, p + step / 2f)
    drawLine(Color(0xFFEF4444), Offset(c0.x - 14f, c0.y - 14f), Offset(c0.x + 14f, c0.y + 14f), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(Color(0xFFEF4444), Offset(c0.x + 14f, c0.y - 14f), Offset(c0.x - 14f, c0.y + 14f), strokeWidth = 8f, cap = StrokeCap.Round)

    // O in cell (1, 1)
    val c1 = Offset(p + 1.5f * step, p + 1.5f * step)
    drawCircle(Color(0xFF0EA5E9), radius = 16f, center = c1, style = Stroke(width = 8f))

    // X in cell (2, 2)
    val c2 = Offset(p + 2.5f * step, p + 2.5f * step)
    drawLine(Color(0xFFEF4444), Offset(c2.x - 14f, c2.y - 14f), Offset(c2.x + 14f, c2.y + 14f), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(Color(0xFFEF4444), Offset(c2.x + 14f, c2.y - 14f), Offset(c2.x - 14f, c2.y + 14f), strokeWidth = 8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawMemoryMatchThumb(w: Float, h: Float) {
    drawRect(Color(0xFF784732))

    // 4 tiles in 2x2
    val cardW = w * 0.38f
    val cardH = h * 0.38f
    val gap = w * 0.08f

    // Card 1 (Yellow Face Up)
    drawRoundRect(
        Color(0xFFFBBF24),
        topLeft = Offset(gap, gap),
        size = Size(cardW, cardH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    // Card 2 (Blue Face Up with star)
    drawRoundRect(
        Color(0xFF38BDF8),
        topLeft = Offset(gap * 2 + cardW, gap),
        size = Size(cardW, cardH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawCircle(Color.White, radius = 10f, center = Offset(gap * 2 + cardW + cardW / 2f, gap + cardH / 2f))

    // Card 3 & 4 (Backs)
    drawRoundRect(
        Color(0xFFFBBF24),
        topLeft = Offset(gap, gap * 2 + cardH),
        size = Size(cardW, cardH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        Color(0xFF38BDF8),
        topLeft = Offset(gap * 2 + cardW, gap * 2 + cardH),
        size = Size(cardW, cardH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawCircle(Color.White, radius = 10f, center = Offset(gap * 2 + cardW + cardW / 2f, gap * 2 + cardH + cardH / 2f))
}

private fun DrawScope.drawTugOfWarThumb(w: Float, h: Float) {
    // Striped turf & road
    drawRect(Color(0xFF0284C7))
    drawLine(Color.White, Offset(w * 0.2f, 0f), Offset(w * 0.5f, h), strokeWidth = 14f)
    drawLine(Color.White, Offset(w * 0.6f, 0f), Offset(w * 0.9f, h), strokeWidth = 14f)

    // Rope stretching diagonally
    drawLine(
        Color(0xFFD97706),
        Offset(0f, h * 0.25f),
        Offset(w, h * 0.75f),
        strokeWidth = 14f,
        cap = StrokeCap.Round
    )

    // Yellow ribbons wrapped around center of rope
    val centerRope = Offset(w * 0.5f, h * 0.5f)
    drawCircle(Color(0xFFFBBF24), radius = 14f, center = centerRope)
    drawCircle(Color(0xFFEF4444), radius = 8f, center = centerRope)
}

private fun DrawScope.drawBasketballThumb(w: Float, h: Float) {
    drawRect(Color(0xFF1E293B))

    // Hoop backboard on right
    val bbX = w * 0.78f
    drawRect(Color.White, Offset(bbX, h * 0.2f), Size(12f, h * 0.5f))
    drawRect(Color(0xFFEF4444), Offset(bbX, h * 0.32f), Size(12f, 26f), style = Stroke(width = 3f))

    // Rim & Net
    drawLine(Color(0xFFEA580C), Offset(bbX - 28f, h * 0.45f), Offset(bbX, h * 0.45f), strokeWidth = 6f)
    drawLine(Color.White.copy(alpha = 0.8f), Offset(bbX - 28f, h * 0.45f), Offset(bbX - 16f, h * 0.62f), strokeWidth = 3f)
    drawLine(Color.White.copy(alpha = 0.8f), Offset(bbX, h * 0.45f), Offset(bbX - 12f, h * 0.62f), strokeWidth = 3f)

    // Basketball in air on left
    drawCircle(Color(0xFFEA580C), radius = 18f, center = Offset(w * 0.32f, h * 0.55f))
    drawCircle(Color.Black, radius = 18f, center = Offset(w * 0.32f, h * 0.55f), style = Stroke(width = 2.5f))
}
