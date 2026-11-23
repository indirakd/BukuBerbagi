package id.bukuberbagi.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.bukuberbagi.ui.components.AppTextField
import id.bukuberbagi.ui.components.BackHeader
import id.bukuberbagi.ui.components.BookCover
import id.bukuberbagi.ui.components.CategoryBookIcon
import id.bukuberbagi.ui.components.FieldLabel
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.ErrorRed
import id.bukuberbagi.ui.theme.Muted
import androidx.compose.ui.graphics.Color

@Composable
fun SplashScreen() {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        if (visible) 1f else 0.6f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "splash"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
            CategoryBookIcon(label = "", seed = "all", iconKey = null, selected = false, onClick = null, isAll = true)
        }
        Text("BukuBerbagi", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Berbagi Buku, Membuka Peluang Membaca",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = Muted
        )
    }
}

@Composable
private fun FormMessages(error: String?, info: String?) {
    if (error != null) {
        Text(
            error, style = MaterialTheme.typography.bodySmall, color = ErrorRed,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF9DCD9)).padding(10.dp)
        )
    }
    if (info != null) {
        Text(
            info, style = MaterialTheme.typography.bodySmall, color = Color(0xFF27500A),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE3EFD9)).padding(10.dp)
        )
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, onRegister: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            BookCover("Dongeng Nusantara", "Tim Penulis", null, "b2", 64.dp)
            BookCover("Fisika Dasar", "Halliday", null, "b1", 80.dp)
            BookCover("Sirah", "Penulis", null, "b5", 64.dp)
        }
        Spacer(Modifier.height(22.dp))
        Text("Selamat datang kembali", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text("Masuk untuk mulai berbagi buku", style = MaterialTheme.typography.bodyMedium, color = Muted)

        FieldLabel("Email")
        AppTextField(
            value = email, onValueChange = { email = it }, placeholder = "nama@email.com",
            error = form.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        FieldLabel("Kata sandi")
        AppTextField(
            value = password, onValueChange = { password = it }, placeholder = "Minimal 6 karakter",
            error = form.passwordError,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        FormMessages(form.error, form.info)

        PillButton(
            "Masuk", onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
            loading = form.loading
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Belum punya akun? ", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Text(
                "Daftar", style = MaterialTheme.typography.titleSmall, color = Coral,
                modifier = Modifier.bounceClick(onClick = onRegister)
            )
        }
    }
}

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Buat akun", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Akun baru otomatis berperan sebagai pengguna", style = MaterialTheme.typography.bodyMedium, color = Muted)
            FieldLabel("Nama lengkap")
            AppTextField(name, { name = it }, placeholder = "Nama kamu", error = form.nameError)
            FieldLabel("Email")
            AppTextField(
                email, { email = it }, placeholder = "nama@email.com", error = form.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            FieldLabel("Kata sandi")
            AppTextField(
                password, { password = it }, placeholder = "Minimal 6 karakter", error = form.passwordError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            FieldLabel("Ulangi kata sandi")
            AppTextField(
                confirm, { confirm = it }, placeholder = "Ketik ulang kata sandi", error = form.confirmError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            FormMessages(form.error, form.info)
            PillButton(
                "Daftar", onClick = { viewModel.register(name, email, password, confirm) },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                loading = form.loading
            )
        }
    }
}
