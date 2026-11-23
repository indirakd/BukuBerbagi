package id.bukuberbagi.ui.book

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.BookCondition
import id.bukuberbagi.ui.components.AppTextField
import id.bukuberbagi.ui.components.BackHeader
import id.bukuberbagi.ui.components.BookCover
import id.bukuberbagi.ui.components.CategoryBookIcon
import id.bukuberbagi.ui.components.CategoryIcons
import id.bukuberbagi.ui.components.CircleIconButton
import id.bukuberbagi.ui.components.ConfirmDialog
import id.bukuberbagi.ui.components.EmptyState
import id.bukuberbagi.ui.components.ErrorText
import id.bukuberbagi.ui.components.FieldLabel
import id.bukuberbagi.ui.components.LoadingBox
import id.bukuberbagi.ui.components.PaperCard
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.PillStyle
import id.bukuberbagi.ui.components.SelectChip
import id.bukuberbagi.ui.components.ShowMessage
import id.bukuberbagi.ui.components.StatusBadge
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.SandDark
import id.bukuberbagi.util.ImageUtils

// =====================================================================
// MyBooksScreen - daftar buku milik pengguna (LazyColumn)
// =====================================================================
@Composable
fun MyBooksScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onOpen: (String) -> Unit,
    viewModel: BookViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Book?>(null) }
    LaunchedEffect(Unit) { viewModel.loadMyBooks() }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Buku saya", onBack)
        when {
            state.loading && state.myBooks.isEmpty() -> LoadingBox()
            state.myBooks.isEmpty() -> EmptyState(
                "Kamu belum mendonasikan buku",
                subtitle = "Buku di rakmu bisa jadi jendela dunia orang lain.",
                actionText = "Tambah buku", onAction = onAdd
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.myBooks, key = { it.id }) { book ->
                    MyBookRow(
                        book = book,
                        pending = state.pendingCounts[book.id] ?: 0,
                        onOpen = { onOpen(book.id) },
                        onEdit = { onEdit(book.id) },
                        onDelete = { toDelete = book }
                    )
                }
                item {
                    PillButton("Tambah buku", onAdd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
    }

    toDelete?.let { book ->
        ConfirmDialog(
            title = "Hapus buku?",
            message = "\"${book.title}\" akan dihapus dari katalog. Permintaan yang masih menunggu ikut dibatalkan.",
            confirmText = "Hapus",
            onConfirm = { viewModel.delete(book); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun MyBookRow(book: Book, pending: Int, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    PaperCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookCover(book.title, book.author, book.imageUrl, book.id, 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    "${book.category?.name ?: "-"} · ${book.conditionLabel}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    Modifier.padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(book.status)
                    if (pending > 0) {
                        Text("$pending permintaan masuk", style = MaterialTheme.typography.labelMedium, color = Coral)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CircleIconButton(Icons.Outlined.Edit, "Edit", onEdit, size = 32.dp, background = MaterialTheme.colorScheme.background)
                CircleIconButton(Icons.Outlined.Delete, "Hapus", onDelete, size = 32.dp, background = MaterialTheme.colorScheme.background, tint = Coral)
            }
        }
    }
}

// =====================================================================
// AddBookScreen & EditBookScreen - memakai form yang sama
// =====================================================================
@Composable
fun AddBookScreen(onSaved: () -> Unit, onMyBooks: () -> Unit, viewModel: BookViewModel = viewModel()) {
    BookFormScreen(editBookId = null, onBack = null, onSaved = onSaved, onMyBooks = onMyBooks, viewModel = viewModel)
}

@Composable
fun EditBookScreen(bookId: String, onBack: () -> Unit, onSaved: () -> Unit, viewModel: BookViewModel = viewModel()) {
    BookFormScreen(editBookId = bookId, onBack = onBack, onSaved = onSaved, onMyBooks = null, viewModel = viewModel)
}

@Composable
private fun BookFormScreen(
    editBookId: String?,
    onBack: (() -> Unit)?,
    onSaved: () -> Unit,
    onMyBooks: (() -> Unit)?,
    viewModel: BookViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var form by remember { mutableStateOf(BookForm()) }
    var prefilled by rememberSaveable { mutableStateOf(false) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoUri = cameraUri
    }

    LaunchedEffect(editBookId) { viewModel.loadForm(editBookId) }
    LaunchedEffect(state.editingBook) {
        val b = state.editingBook
        if (b != null && !prefilled) {
            form = BookForm(b.title, b.author, b.categoryId, b.condition, b.description.orEmpty(), b.location.orEmpty())
            prefilled = true
        }
    }
    LaunchedEffect(state.savedEvent) {
        if (state.savedEvent) {
            viewModel.consumeSaved()
            form = BookForm()
            photoUri = null
            onSaved()
        }
    }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) BackHeader("Edit buku", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            if (onBack == null) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
                    Text("Donasikan\nbukumu", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    if (onMyBooks != null) PillButton("Buku saya", onMyBooks, style = PillStyle.Light, compact = true)
                }
            }

            // ---------------- Foto ----------------
            FieldLabel("Foto buku")
            val preview: Any? = photoUri ?: state.editingBook?.imageUrl
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Paper)
                    .border(BorderStroke(1.5.dp, SandDark), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    AsyncImage(model = preview, contentDescription = "Foto buku", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Coral, modifier = Modifier.size(32.dp))
                        Text("Tambahkan foto sampul buku", style = MaterialTheme.typography.labelLarge, color = Muted)
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    "Galeri", onClick = {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    style = PillStyle.Light, icon = Icons.Outlined.PhotoLibrary, compact = true
                )
                PillButton(
                    "Kamera", onClick = {
                        val uri = ImageUtils.createCameraUri(context)
                        cameraUri = uri
                        camera.launch(uri)
                    },
                    style = PillStyle.Light, icon = Icons.Outlined.CameraAlt, compact = true
                )
            }
            ErrorText(state.errors.photo)

            // ---------------- Data buku ----------------
            FieldLabel("Judul")
            AppTextField(form.title, { form = form.copy(title = it) }, placeholder = "Fisika Dasar Jilid 1", error = state.errors.title)
            FieldLabel("Penulis")
            AppTextField(form.author, { form = form.copy(author = it) }, placeholder = "Nama penulis", error = state.errors.author)

            FieldLabel("Kategori")
            if (state.loading && state.categories.isEmpty()) {
                LoadingBox()
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.categories, key = { it.id }) { c ->
                        CategoryBookIcon(
                            label = c.name.substringBefore(" "),
                            seed = c.id,
                            iconKey = c.icon,
                            selected = form.categoryId == c.id,
                            onClick = { form = form.copy(categoryId = c.id) }
                        )
                    }
                }
            }
            ErrorText(state.errors.category)

            FieldLabel("Kondisi")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BookCondition.entries.forEach { c ->
                    SelectChip(c.label, form.condition == c.code, onClick = { form = form.copy(condition = c.code) })
                }
            }
            ErrorText(state.errors.condition)

            FieldLabel("Deskripsi")
            AppTextField(
                form.description, { if (it.length <= 500) form = form.copy(description = it) },
                placeholder = "Kondisi halaman, catatan, dsb.", singleLine = false, minLines = 3
            )
            FieldLabel("Lokasi umum penyerahan")
            AppTextField(form.location, { form = form.copy(location = it) }, placeholder = "Kecamatan, kota")

            PillButton(
                if (editBookId == null) "Unggah dan simpan" else "Simpan perubahan",
                onClick = {
                    val uri = photoUri
                    viewModel.save(form, hasNewPhoto = uri != null) {
                        uri?.let { ImageUtils.compressToJpeg(context, it) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                loading = state.saving
            )
        }
    }
}
