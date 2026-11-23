package id.bukuberbagi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.bukuberbagi.data.model.BookStatus
import id.bukuberbagi.data.model.RequestStatus
import id.bukuberbagi.ui.theme.Brown
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.ErrorRed
import id.bukuberbagi.ui.theme.Fraunces
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.Sand

/** Snackbar global (pengganti notifikasi hitam di demo). */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Fungsi global untuk menampilkan pesan singkat. Dijalankan di scope root,
 * jadi pesan tetap tampil walaupun layar berpindah.
 */
val LocalMessenger = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Tampilkan [message] sekali lalu panggil [onShown] untuk mengosongkannya di ViewModel. */
@Composable
fun ShowMessage(message: String?, onShown: () -> Unit) {
    val messenger = LocalMessenger.current
    androidx.compose.runtime.LaunchedEffect(message) {
        if (message != null) {
            messenger(message)
            onShown()
        }
    }
}

// ------------------------------ Avatar ------------------------------
private val AvatarColors = listOf(Color(0xFFF8E6C8), Color(0xFFE4E0F5), Color(0xFFE3EFD9), Color(0xFFF9DCD9))

@Composable
fun Avatar(name: String, imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    val bg = AvatarColors[(name.hashCode() and Int.MAX_VALUE) % AvatarColors.size]
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                fontFamily = Fraunces,
                fontSize = (size.value * 0.42f).sp,
                color = Brown
            )
        }
    }
}

// ------------------------------ Badge status ------------------------------
private data class BadgeStyle(val label: String, val bg: Color, val fg: Color)

private fun badgeStyle(status: String): BadgeStyle = when (status) {
    BookStatus.AVAILABLE -> BadgeStyle("Tersedia", Color(0xFFE3EFD9), Color(0xFF3B6D11))
    BookStatus.RESERVED -> BadgeStyle("Dipesan", Color(0xFFF8E6C8), Color(0xFF854F0B))
    BookStatus.DONATED -> BadgeStyle("Didonasikan", Sand, Color(0xFF5F5249))
    RequestStatus.PENDING -> BadgeStyle("Menunggu", Color(0xFFF8E6C8), Color(0xFF854F0B))
    RequestStatus.APPROVED -> BadgeStyle("Disetujui", Color(0xFFE4E0F5), Color(0xFF3C3489))
    RequestStatus.REJECTED -> BadgeStyle("Ditolak", Color(0xFFF9DCD9), Color(0xFFA23D36))
    RequestStatus.CANCELLED -> BadgeStyle("Dibatalkan", Sand, Color(0xFF5F5249))
    RequestStatus.COMPLETED -> BadgeStyle("Selesai", Color(0xFFE3EFD9), Color(0xFF27500A))
    else -> BadgeStyle(status, Sand, Brown)
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val s = badgeStyle(status)
    Text(
        s.label,
        style = MaterialTheme.typography.labelMedium,
        color = s.fg,
        modifier = modifier.clip(RoundedCornerShape(50)).background(s.bg).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

// ------------------------------ Teks & header ------------------------------
@Composable
fun BackHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali", onBack)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        action?.invoke()
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = Muted,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
fun ErrorText(text: String?) {
    if (!text.isNullOrBlank()) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.padding(top = 3.dp))
    }
}

/** Kolom input krem dengan garis koral saat fokus. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: ImageVector? = null,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .border(2.dp, if (focused) Coral else if (error != null) ErrorRed.copy(alpha = 0.6f) else Color.Transparent, shape),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Muted) },
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = Muted) } },
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Paper,
                unfocusedContainerColor = Sand,
                disabledContainerColor = Sand,
                errorContainerColor = Sand,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = Coral,
                disabledTextColor = Muted
            )
        )
        ErrorText(error)
    }
}

// ------------------------------ Chip ------------------------------
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val click = if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .then(click)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Ink else Paper)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Paper else Brown, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Paper else Brown)
    }
}

// ------------------------------ Status kosong / memuat ------------------------------
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Coral, strokeWidth = 3.dp)
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            PillButton(actionText, onAction)
        }
    }
}

@Composable
fun QuoteText(text: String, modifier: Modifier = Modifier) {
    Text(
        "“$text”",
        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 21.sp),
        color = Color(0xFF5F5249),
        modifier = modifier
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = Coral, style = MaterialTheme.typography.labelLarge) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Brown, style = MaterialTheme.typography.labelLarge) } }
    )
}

/** Kartu krem bulat yang dipakai di banyak layar. */
@Composable
fun PaperCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val click = if (onClick != null) Modifier.bounceClick(pressedScale = 0.97f, onClick = onClick) else Modifier
    Box(
        modifier
            .then(click)
            .clip(RoundedCornerShape(18.dp))
            .background(Paper)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) { content() }
}
