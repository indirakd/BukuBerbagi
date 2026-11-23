package id.bukuberbagi.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.ui.components.AppTextField
import id.bukuberbagi.ui.components.Avatar
import id.bukuberbagi.ui.components.BackHeader
import id.bukuberbagi.ui.components.ConfirmDialog
import id.bukuberbagi.ui.components.FieldLabel
import id.bukuberbagi.ui.components.PaperCard
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.ShowMessage
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.BrownSoft
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.MutedLight
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.util.ImageUtils

@Composable
fun ProfileScreen(
    profile: Profile,
    onEditProfile: () -> Unit,
    onMyBooks: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var askLogout by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!profile.isAdmin) viewModel.loadStats() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(profile.fullName, profile.profileImageUrl, 88.dp, Modifier.padding(top = 8.dp))
        Text(profile.fullName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
        if (profile.isAdmin) {
            Text(
                "Admin", style = MaterialTheme.typography.labelMedium, color = Color(0xFF3C3489),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE4E0F5)).padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Text(profile.email.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BrownSoft, modifier = Modifier.size(14.dp))
            Text(profile.location ?: "Lokasi belum diisi", style = MaterialTheme.typography.bodySmall, color = BrownSoft)
        }

        if (!profile.isAdmin) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Diposting", state.postedCount, Modifier.weight(1f))
                StatBox("Didonasikan", state.donatedCount, Modifier.weight(1f))
                StatBox("Diterima", state.receivedCount, Modifier.weight(1f))
            }
        } else Spacer(Modifier.height(16.dp))

        MenuRow(Icons.Outlined.ManageAccounts, "Edit profil", onClick = onEditProfile)
        if (!profile.isAdmin) MenuRow(Icons.Outlined.MenuBook, "Buku saya", onClick = onMyBooks)
        MenuRow(Icons.AutoMirrored.Outlined.Logout, "Keluar", color = Coral, onClick = { askLogout = true })
    }

    if (askLogout) {
        ConfirmDialog("Keluar dari akun?", "Kamu perlu login lagi untuk memakai BukuBerbagi.", "Keluar",
            onConfirm = { askLogout = false; onLogout() }, onDismiss = { askLogout = false })
    }
}

@Composable
private fun StatBox(label: String, value: Int, modifier: Modifier = Modifier) {
    PaperCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, color: Color = Ink, onClick: () -> Unit) {
    PaperCard(Modifier.fillMaxWidth().padding(bottom = 9.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MutedLight)
        }
    }
}

@Composable
fun EditProfileScreen(
    profile: Profile,
    onBack: () -> Unit,
    onSaved: (Profile) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf(profile.fullName) }
    var location by rememberSaveable { mutableStateOf(profile.location.orEmpty()) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }

    LaunchedEffect(state.savedProfile) {
        state.savedProfile?.let { viewModel.consumeSaved(); onSaved(it) }
    }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Edit profil", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.bounceClick { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri, contentDescription = "Foto profil", contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                        )
                    } else {
                        Avatar(profile.fullName, profile.profileImageUrl, 96.dp)
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(Coral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Ganti foto", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                "Ketuk foto untuk mengganti. Foto diunggah ke Supabase Storage (bucket avatars).",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FieldLabel("Nama lengkap")
            AppTextField(name, { name = it }, error = state.nameError)
            FieldLabel("Email")
            AppTextField(profile.email.orEmpty(), {}, enabled = false)
            FieldLabel("Lokasi umum")
            AppTextField(location, { location = it }, placeholder = "Kota Jambi")
            PillButton(
                "Simpan", onClick = {
                    val uri = photoUri
                    val photo: (suspend () -> ByteArray)? = uri?.let { u ->
                        suspend { ImageUtils.compressToJpeg(context, u, maxSize = 600) }
                    }
                    viewModel.save(profile, name, location, photo)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp), loading = state.saving
            )
        }
    }
}
