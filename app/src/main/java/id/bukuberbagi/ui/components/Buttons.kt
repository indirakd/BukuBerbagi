package id.bukuberbagi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Paper

enum class PillStyle { Dark, Coral, Light, Outline, DangerOutline }

/** Tombol pil: hitam (utama), koral, terang, atau bergaris. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Dark,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val (bg, fg) = when (style) {
        PillStyle.Dark -> Ink to Paper
        PillStyle.Coral -> Coral to Color.White
        PillStyle.Light, PillStyle.Outline -> Paper to Ink
        PillStyle.DangerOutline -> Paper to Coral
    }
    val shape = RoundedCornerShape(50)
    var base = modifier
        .bounceClick(enabled = enabled && !loading, onClick = onClick)
        .clip(shape)
        .background(if (enabled) bg else bg.copy(alpha = 0.5f))
    if (style == PillStyle.Outline) base = base.border(1.5.dp, Ink, shape)
    if (style == PillStyle.DangerOutline) base = base.border(1.5.dp, Coral, shape)

    Box(
        modifier = base
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 8.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
                if (trailingIcon != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(trailingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Tombol bulat kecil (kembali, notifikasi, edit, hapus). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    background: Color = Paper,
    tint: Color = Ink
) {
    Box(
        modifier = modifier
            .bounceClick(pressedScale = 0.88f, onClick = onClick)
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}
