package id.bukuberbagi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.bukuberbagi.R

// ---------------------------------------------------------------------
// Warna (krem hangat + aksen koral, sesuai demo)
// ---------------------------------------------------------------------
val Cream = Color(0xFFF4EEE6)
val Paper = Color(0xFFFBF8F3)
val Sand = Color(0xFFEBE2D6)
val SandDark = Color(0xFFCBBBA8)
val Line = Color(0xFFE3D8CA)
val Ink = Color(0xFF2B2622)
val InkSoft = Color(0xFF453D36)
val Brown = Color(0xFF4A3F35)
val BrownSoft = Color(0xFF5F5249)
val Muted = Color(0xFF8C7F72)
val MutedLight = Color(0xFFA89A8B)
val Coral = Color(0xFFE5605A)
val Ribbon = Color(0xFFF2A93B)
val ErrorRed = Color(0xFFC0392B)

/** Pasangan warna sampul [depan, punggung] untuk buku tanpa foto & ikon kategori. */
val CoverPalette = listOf(
    Color(0xFF5B6FB0) to Color(0xFF3F4F86),
    Color(0xFFD4574E) to Color(0xFFA23D36),
    Color(0xFF4F8F5B) to Color(0xFF36683F),
    Color(0xFF8A6FC0) to Color(0xFF644E94),
    Color(0xFFD79A3A) to Color(0xFFA8741F),
    Color(0xFF3F8F88) to Color(0xFF2A6762),
    Color(0xFFC0566F) to Color(0xFF913C51),
    Color(0xFF7A6A5C) to Color(0xFF54483D)
)

fun paletteFor(seed: String): Pair<Color, Color> =
    CoverPalette[(seed.hashCode() and Int.MAX_VALUE) % CoverPalette.size]

// ---------------------------------------------------------------------
// Huruf: Fraunces (judul, serif) + Nunito (isi)
// ---------------------------------------------------------------------
val Fraunces = FontFamily(Font(R.font.fraunces_medium, FontWeight.Medium))
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 35.sp, color = Ink),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 31.sp, color = Ink),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 27.sp, color = Ink),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 24.sp, color = Ink),
    titleMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp, color = Ink),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp, color = Ink),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = Brown),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, color = Brown),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, color = Muted),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 13.sp)
)

private val AppColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Coral,
    onSecondary = Color.White,
    tertiary = Ribbon,
    background = Cream,
    onBackground = Brown,
    surface = Paper,
    onSurface = Brown,
    surfaceVariant = Sand,
    onSurfaceVariant = Muted,
    outline = SandDark,
    error = ErrorRed
)

@Composable
fun BukuBerbagiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
