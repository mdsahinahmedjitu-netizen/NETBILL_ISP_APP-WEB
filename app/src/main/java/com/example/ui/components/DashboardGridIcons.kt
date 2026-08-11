package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. COLLECTION ICON (3D Green Card with White Circle 'ট', 3D Hand & Yellow Rays)
@Composable
fun CollectionGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top-Right Yellow Rays/Sparks
        val rayColor = Color(0xFFFFEB3B)
        drawLine(color = rayColor, start = Offset(w * 0.74f, h * 0.18f), end = Offset(w * 0.86f, h * 0.10f), strokeWidth = 5.5f, cap = StrokeCap.Round)
        drawLine(color = rayColor, start = Offset(w * 0.80f, h * 0.27f), end = Offset(w * 0.94f, h * 0.25f), strokeWidth = 5.5f, cap = StrokeCap.Round)
        drawLine(color = rayColor, start = Offset(w * 0.76f, h * 0.36f), end = Offset(w * 0.89f, h * 0.40f), strokeWidth = 5.5f, cap = StrokeCap.Round)

        // Green 3D Card / Button
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF15803D))
            ),
            topLeft = Offset(w * 0.16f, h * 0.10f),
            size = Size(w * 0.62f, h * 0.38f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // White Circle with 'ট'
        val circleCenter = Offset(w * 0.47f, h * 0.29f)
        val circleRadius = w * 0.12f
        drawCircle(
            color = Color.White,
            radius = circleRadius,
            center = circleCenter
        )

        // Bengali "ট" Text inside Circle
        val takaLayout = textMeasurer.measure(
            text = "ট",
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
        )
        drawText(
            textLayoutResult = takaLayout,
            topLeft = Offset(circleCenter.x - takaLayout.size.width / 2f, circleCenter.y - takaLayout.size.height / 2f)
        )

        // --- 3D HAND GRAPHIC ---
        // 1. Blue Sleeve Cuff at base
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
            ),
            topLeft = Offset(w * 0.32f, h * 0.78f),
            size = Size(w * 0.32f, h * 0.20f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // 2. Palm / Hand Body (3D Soft Peach/Orange)
        val palmPath = Path().apply {
            moveTo(w * 0.36f, h * 0.78f)
            lineTo(w * 0.36f, h * 0.54f)
            quadraticTo(w * 0.38f, h * 0.46f, w * 0.46f, h * 0.46f)
            lineTo(w * 0.53f, h * 0.46f)
            quadraticTo(w * 0.62f, h * 0.46f, w * 0.62f, h * 0.54f)
            lineTo(w * 0.62f, h * 0.78f)
            close()
        }
        drawPath(
            path = palmPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD8B3), Color(0xFFFFB273), Color(0xFFFB923C), Color(0xFFEA580C))
            )
        )

        // 3. Tucked Thumb (Left Side)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFE3C7), Color(0xFFFB923C))
            ),
            topLeft = Offset(w * 0.28f, h * 0.56f),
            size = Size(w * 0.14f, h * 0.18f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // 4. Folded Fingers (Right Side 3D Knuckles)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFB923C), Color(0xFFE65100))
            ),
            topLeft = Offset(w * 0.52f, h * 0.50f),
            size = Size(w * 0.14f, h * 0.26f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // 5. Pointing Index Finger (Reaches straight up onto the white circle button)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF0DC), Color(0xFFFFC085), Color(0xFFFB923C), Color(0xFFEA580C))
            ),
            topLeft = Offset(w * 0.41f, h * 0.26f),
            size = Size(w * 0.13f, h * 0.38f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Fingertip Glossy Highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = w * 0.035f,
            center = Offset(w * 0.475f, h * 0.31f)
        )
    }
}

// 2. COLLECTION REPORT ICON (Green Growth Arrow, Bar Chart, Gold Taka Coins)
@Composable
fun CollectionReportGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 3D Bar Chart (Blue & Purple Bars)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
            topLeft = Offset(w * 0.46f, h * 0.48f),
            size = Size(w * 0.13f, h * 0.32f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFC084FC), Color(0xFF7C3AED))),
            topLeft = Offset(w * 0.63f, h * 0.32f),
            size = Size(w * 0.13f, h * 0.48f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Green Upward Trend Arrow
        val arrowPath = Path().apply {
            moveTo(w * 0.16f, h * 0.56f)
            lineTo(w * 0.64f, h * 0.16f)
        }
        drawPath(
            path = arrowPath,
            color = Color(0xFF00E65B),
            style = Stroke(width = 9f, cap = StrokeCap.Round)
        )

        val arrowHead = Path().apply {
            moveTo(w * 0.46f, h * 0.16f)
            lineTo(w * 0.66f, h * 0.16f)
            lineTo(w * 0.66f, h * 0.36f)
        }
        drawPath(
            path = arrowHead,
            color = Color(0xFF00E65B),
            style = Stroke(width = 9f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        // Stack of Gold Taka Coins
        // Bottom Coin
        drawOval(
            brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFCA8A04))),
            topLeft = Offset(w * 0.40f, h * 0.68f),
            size = Size(w * 0.34f, h * 0.16f)
        )

        // Middle Coin
        drawOval(
            brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFD97706))),
            topLeft = Offset(w * 0.40f, h * 0.62f),
            size = Size(w * 0.34f, h * 0.16f)
        )

        // Top Coin
        drawOval(
            brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
            topLeft = Offset(w * 0.40f, h * 0.56f),
            size = Size(w * 0.34f, h * 0.16f)
        )

        // Front Facing Gold Coin with Taka Symbol
        val coinCenter = Offset(w * 0.28f, h * 0.66f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
            radius = w * 0.17f,
            center = coinCenter
        )
        drawCircle(color = Color(0xFFFEF08A), radius = w * 0.13f, center = coinCenter, style = Stroke(width = 3f))

        val takaLayout = textMeasurer.measure(
            text = "৳",
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
        )
        drawText(
            textLayoutResult = takaLayout,
            topLeft = Offset(coinCenter.x - takaLayout.size.width / 2f, coinCenter.y - takaLayout.size.height / 2f)
        )
    }
}

// 3. LIST REPORT ICON (Colorful 3D User Avatars Group)
@Composable
fun ListReportGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left Avatar (Orange Head, Green Body)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFF8A65), Color(0xFFFF5722))),
            radius = w * 0.11f,
            center = Offset(w * 0.26f, h * 0.38f)
        )
        val leftBody = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(w * 0.11f, h * 0.48f, w * 0.41f, h * 0.82f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(leftBody, brush = Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A))))

        // Right Avatar (Yellow Head, Green Body)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFDE047), Color(0xFFEAB308))),
            radius = w * 0.11f,
            center = Offset(w * 0.74f, h * 0.38f)
        )
        val rightBody = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(w * 0.59f, h * 0.48f, w * 0.89f, h * 0.82f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(rightBody, brush = Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A))))

        // Top Center Head (Cyan)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
            radius = w * 0.13f,
            center = Offset(w * 0.50f, h * 0.28f)
        )

        // Center Front Avatar (Blue)
        val centerBody = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(w * 0.28f, h * 0.42f, w * 0.72f, h * 0.88f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(centerBody, brush = Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1D4ED8))))
    }
}

// 4. DUE LIST ICON (White 3D Clipboard with Lines & Blue/Magenta Pie Chart)
@Composable
fun DueListGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // White 3D Clipboard Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))),
            topLeft = Offset(w * 0.20f, h * 0.16f),
            size = Size(w * 0.60f, h * 0.72f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Dark Blue Clamp Header
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))),
            topLeft = Offset(w * 0.34f, h * 0.10f),
            size = Size(w * 0.32f, h * 0.12f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Blue horizontal text lines
        drawLine(color = Color(0xFF60A5FA), start = Offset(w * 0.28f, h * 0.32f), end = Offset(w * 0.64f, h * 0.32f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFF93C5FD), start = Offset(w * 0.28f, h * 0.42f), end = Offset(w * 0.50f, h * 0.42f), strokeWidth = 4f, cap = StrokeCap.Round)

        // 3D Pie Chart on bottom right
        val pieCenter = Offset(w * 0.56f, h * 0.62f)
        val pieRadius = w * 0.18f

        // Blue Slice (Paid)
        drawArc(
            brush = Brush.sweepGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
            startAngle = -40f,
            sweepAngle = 240f,
            useCenter = true,
            topLeft = Offset(pieCenter.x - pieRadius, pieCenter.y - pieRadius),
            size = Size(pieRadius * 2f, pieRadius * 2f)
        )

        // Magenta/Red Slice (Due)
        drawArc(
            brush = Brush.sweepGradient(listOf(Color(0xFFFF007A), Color(0xFFE11D48))),
            startAngle = 200f,
            sweepAngle = 120f,
            useCenter = true,
            topLeft = Offset(pieCenter.x - pieRadius, pieCenter.y - pieRadius),
            size = Size(pieRadius * 2f, pieRadius * 2f)
        )
    }
}

// 5. CREATE NEW ICON (3D Purple Silhouette + Cyan Plus Badge)
@Composable
fun CreateNewGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 3D Purple/Blue User Silhouette
        val headCenter = Offset(w * 0.44f, h * 0.32f)
        val headRadius = w * 0.19f

        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFA5B4FC), Color(0xFF6366F1), Color(0xFF4338CA))),
            radius = headRadius,
            center = headCenter
        )

        val bodyPath = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(
                    headCenter.x - w * 0.30f,
                    headCenter.y + h * 0.08f,
                    headCenter.x + w * 0.30f,
                    headCenter.y + h * 0.54f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF3730A3)))
        )

        // Cyan Badge with Bold White Plus "+" Sign
        val plusCenter = Offset(w * 0.72f, h * 0.62f)
        val plusRadius = w * 0.18f

        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF22D3EE), Color(0xFF06B6D4), Color(0xFF0891B2))),
            radius = plusRadius,
            center = plusCenter
        )

        // Plus Horizontal line
        drawLine(
            color = Color.White,
            start = Offset(plusCenter.x - plusRadius * 0.5f, plusCenter.y),
            end = Offset(plusCenter.x + plusRadius * 0.5f, plusCenter.y),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )

        // Plus Vertical line
        drawLine(
            color = Color.White,
            start = Offset(plusCenter.x, plusCenter.y - plusRadius * 0.5f),
            end = Offset(plusCenter.x, plusCenter.y + plusRadius * 0.5f),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }
}

// 6. SEARCH ICON (Glossy 3D Magnifying Glass inspects User Avatar)
@Composable
fun SearchGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background soft avatars
        drawCircle(color = Color(0x66C084FC), radius = w * 0.10f, center = Offset(w * 0.24f, h * 0.38f))
        drawCircle(color = Color(0x66C084FC), radius = w * 0.10f, center = Offset(w * 0.76f, h * 0.38f))

        // Center User Silhouette inside lens
        val userCenter = Offset(w * 0.50f, h * 0.36f)
        drawCircle(color = Color.White, radius = w * 0.12f, center = userCenter)

        val userBody = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(w * 0.36f, h * 0.40f, w * 0.64f, h * 0.62f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(userBody, color = Color.White)

        // Magnifying Glass Lens
        val magCenter = Offset(w * 0.50f, h * 0.44f)
        val magRadius = w * 0.22f

        // Cyan Glass Tint
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x3338BDF8), Color(0x110284C7))),
            radius = magRadius,
            center = magCenter
        )

        // White/Glow Lens Rim
        drawCircle(
            color = Color.White,
            radius = magRadius,
            center = magCenter,
            style = Stroke(width = 8f)
        )

        // Handle
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(magCenter.x + magRadius * 0.7f, magCenter.y + magRadius * 0.7f),
            end = Offset(w * 0.86f, h * 0.86f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
    }
}

// 7. COMPLAINT LIST ICON (3D Checklist with Orange Header, Checks & Blue Pen)
@Composable
fun ComplinListGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // White Document Card
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFAFA))),
            topLeft = Offset(w * 0.20f, h * 0.14f),
            size = Size(w * 0.56f, h * 0.72f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Orange Header Bar
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))),
            topLeft = Offset(w * 0.20f, h * 0.14f),
            size = Size(w * 0.56f, h * 0.10f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Green Checkmark 1 ✓
        val check1 = Path().apply {
            moveTo(w * 0.26f, h * 0.34f)
            lineTo(w * 0.32f, h * 0.40f)
            lineTo(w * 0.42f, h * 0.28f)
        }
        drawPath(check1, color = Color(0xFF22C55E), style = Stroke(width = 5f, cap = StrokeCap.Round))

        // Red Cross Mark X
        val cx = w * 0.34f
        val cy = h * 0.52f
        val cr = w * 0.06f
        drawLine(color = Color(0xFFEF4444), start = Offset(cx - cr, cy - cr), end = Offset(cx + cr, cy + cr), strokeWidth = 5f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFFEF4444), start = Offset(cx + cr, cy - cr), end = Offset(cx - cr, cy + cr), strokeWidth = 5f, cap = StrokeCap.Round)

        // Green Checkmark 2 ✓
        val check2 = Path().apply {
            moveTo(w * 0.26f, h * 0.68f)
            lineTo(w * 0.32f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.62f)
        }
        drawPath(check2, color = Color(0xFF22C55E), style = Stroke(width = 5f, cap = StrokeCap.Round))

        // Document Lines
        drawLine(color = Color(0xFF94A3B8), start = Offset(w * 0.48f, h * 0.34f), end = Offset(w * 0.68f, h * 0.34f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFF94A3B8), start = Offset(w * 0.48f, h * 0.52f), end = Offset(w * 0.68f, h * 0.52f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFF94A3B8), start = Offset(w * 0.48f, h * 0.68f), end = Offset(w * 0.68f, h * 0.68f), strokeWidth = 3.5f, cap = StrokeCap.Round)

        // 3D Angled Blue Pen writing on document
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))),
            start = Offset(w * 0.82f, h * 0.24f),
            end = Offset(w * 0.62f, h * 0.84f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
}

// 8. BILL SUMMARY ICON (White Statement Receipt with Orange Header & Green Tag)
@Composable
fun BillSummaryGridIcon(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // White Receipt Card
        val cardLeft = w * 0.22f
        val cardTop = h * 0.14f
        val cardW = w * 0.56f
        val cardH = h * 0.72f

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC))),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Orange Header Bar
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, h * 0.12f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Statement Lines
        drawLine(color = Color(0xFFCBD5E1), start = Offset(w * 0.28f, h * 0.36f), end = Offset(w * 0.72f, h * 0.36f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFFCBD5E1), start = Offset(w * 0.28f, h * 0.48f), end = Offset(w * 0.64f, h * 0.48f), strokeWidth = 4f, cap = StrokeCap.Round)

        // Bright Green Bottom Button Tag
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))),
            topLeft = Offset(w * 0.28f, h * 0.64f),
            size = Size(w * 0.44f, h * 0.12f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}
