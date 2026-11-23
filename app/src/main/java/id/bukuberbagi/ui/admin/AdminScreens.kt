package id.bukuberbagi.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VolunteerActivism
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bukuberbagi.data.model.BookStatus
import id.bukuberbagi.data.model.Category
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.ui.catalog.BookGridItem
import id.bukuberbagi.ui.components.AppTextField
import id.bukuberbagi.ui.components.Avatar
import id.bukuberbagi.ui.components.BackHeader
import id.bukuberbagi.ui.components.BookCover
import id.bukuberbagi.ui.components.CategoryBookIcon
import id.bukuberbagi.ui.components.CategoryIcons
import id.bukuberbagi.ui.components.CircleIconButton
import id.bukuberbagi.ui.components.ConfirmDialog
import id.bukuberbagi.ui.components.EmptyState
import id.bukuberbagi.ui.components.FieldLabel
import id.bukuberbagi.ui.components.LoadingBox
import id.bukuberbagi.ui.components.PaperCard
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.PillStyle
import id.bukuberbagi.ui.components.SectionHeader
import id.bukuberbagi.ui.components.SelectChip
import id.bukuberbagi.ui.components.ShowMessage
import id.bukuberbagi.ui.components.StatusBadge
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.Ribbon
import id.bukuberbagi.ui.theme.Sand
import id.bukuberbagi.ui.theme.SandDark
import id.bukuberbagi.ui.theme.paletteFor

// =====================================================================
// AdminDashboardScreen
// =====================================================================
@Composable
fun AdminDashboardScreen(
    profile: Profile,
    onManageCategories: () -> Unit,
    onOpenBook: (String) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadDashboard() }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.fullName, profile.profileImageUrl, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Panel admin", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text(profile.fullName, style = MaterialTheme.typography.titleSmall)
                }
            }
            Text(
                "Ringkasan\nBukuBerbagi", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
        item {
            val s = state.stats
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Pengguna", s.totalUsers, Icons.Outlined.Group, dark = true, modifier = Modifier.weight(1f))
                    StatCard("Buku tersedia", s.availableBooks, Icons.Outlined.MenuBook, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Permintaan aktif", s.activeRequests, Icons.Outlined.SwapHoriz, modifier = Modifier.weight(1f))
                    StatCard("Didonasikan", s.donatedBooks, Icons.Outlined.VolunteerActivism, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            SectionHeader("Buku per kategori") {
                PillButton("Kelola", onManageCategories, style = PillStyle.Light, compact = true)
            }
            PaperCard(Modifier.fillMaxWidth()) {
                val counts = state.countByCategory
                val max = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.categories.isEmpty()) Text("Belum ada kategori", style = MaterialTheme.typography.bodySmall)
                    state.categories.forEach { c ->
                        val n = counts[c.id] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(Sand)) {
                                Box(
                                    Modifier.fillMaxWidth(n / max.toFloat()).fillMaxHeight()
                                        .clip(RoundedCornerShape(50)).background(paletteFor(c.id).first)
                                )
                            }
                            Text("$n", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp).width(20.dp))
                        }
                    }
                }
            }
        }
        item { SectionHeader("Permintaan terbaru") }
        if (state.loading && state.recentRequests.isEmpty()) item { LoadingBox() }
        else if (state.recentRequests.isEmpty()) item { Text("Belum ada permintaan", style = MaterialTheme.typography.bodySmall) }
        items(state.recentRequests, key = { it.id }) { r ->
            PaperCard(Modifier.fillMaxWidth(), onClick = { onOpenBook(r.bookId) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCover(r.book?.title.orEmpty(), r.book?.author.orEmpty(), r.book?.imageUrl, r.bookId, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.book?.title ?: "-", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            "${r.requester?.fullName ?: "-"} → ${r.donor?.fullName ?: "-"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    StatusBadge(r.status)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Long, icon: ImageVector, modifier: Modifier = Modifier, dark: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (dark) Ink else Paper)
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (dark) Ribbon else Coral, modifier = Modifier.size(20.dp))
        Text(
            "$value", style = MaterialTheme.typography.headlineMedium,
            color = if (dark) Paper else Ink, modifier = Modifier.padding(top = 4.dp)
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (dark) SandDark else Muted)
    }
}

// =====================================================================
// CategoryManagementScreen - daftar kategori (LazyColumn) + hapus
// =====================================================================
@Composable
fun CategoryManagementScreen(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Category?>(null) }
    LaunchedEffect(Unit) { viewModel.loadCategories() }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kelola kategori", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                PillButton("Tambah", onAdd, icon = Icons.Outlined.Add, compact = true)
            }
            Text(
                "Kategori yang kamu tambahkan langsung muncul di katalog dan form donasi pengguna.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
        }
        if (state.loading && state.categories.isEmpty()) item { LoadingBox() }
        else if (state.categories.isEmpty()) item { EmptyState("Belum ada kategori", actionText = "Tambah kategori", onAction = onAdd) }
        items(state.categories, key = { it.id }) { c ->
            val count = state.countByCategory[c.id] ?: 0
            PaperCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBookIcon(label = "", seed = c.id, iconKey = c.icon, selected = false, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$count buku" + (c.description?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall, maxLines = 2
                        )
                    }
                    CircleIconButton(Icons.Outlined.Edit, "Edit", { onEdit(c.id) }, size = 32.dp, background = MaterialTheme.colorScheme.background)
                    Spacer(Modifier.width(6.dp))
                    CircleIconButton(Icons.Outlined.Delete, "Hapus", { toDelete = c }, size = 32.dp, background = MaterialTheme.colorScheme.background, tint = Coral)
                }
            }
        }
    }

    toDelete?.let { c ->
        ConfirmDialog(
            title = "Hapus kategori?",
            message = "Kategori \"${c.name}\" akan dihapus. Kategori yang masih dipakai buku tidak bisa dihapus.",
            confirmText = "Hapus",
            onConfirm = { viewModel.deleteCategory(c); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

// =====================================================================
// AddCategoryScreen - tambah / edit kategori
// =====================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryScreen(
    categoryId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("bookmark") }
    var prefilled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(categoryId) { viewModel.loadCategoryForEdit(categoryId) }
    LaunchedEffect(state.editingCategory) {
        val c = state.editingCategory
        if (c != null && !prefilled) {
            name = c.name; description = c.description.orEmpty(); icon = c.icon; prefilled = true
        }
    }
    LaunchedEffect(state.savedEvent) {
        if (state.savedEvent) { viewModel.consumeSaved(); onSaved() }
    }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    val isEdit = !categoryId.isNullOrBlank()
    Column(Modifier.fillMaxSize()) {
        BackHeader(if (isEdit) "Edit kategori" else "Tambah kategori", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CategoryBookIcon(
                    label = name.ifBlank { "Pratinjau" }, seed = state.editingCategory?.id ?: name.ifBlank { "baru" },
                    iconKey = icon, selected = true, onClick = null
                )
            }
            FieldLabel("Nama kategori")
            AppTextField(name, { name = it }, placeholder = "Sejarah", error = state.nameError)
            FieldLabel("Deskripsi")
            AppTextField(description, { description = it }, placeholder = "Buku sejarah Indonesia dan dunia")
            FieldLabel("Ikon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcons.options.forEach { (key, vector) ->
                    val selected = key == icon
                    Box(
                        Modifier
                            .bounceClick(pressedScale = 0.85f) { icon = key }
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) Ink else Sand),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(vector, contentDescription = key, tint = if (selected) Color.White else Ink, modifier = Modifier.size(20.dp))
                    }
                }
            }
            PillButton(
                if (isEdit) "Simpan" else "Tambah kategori",
                onClick = { viewModel.saveCategory(name, description, icon) },
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                loading = state.saving
            )
        }
    }
}

// =====================================================================
// AdminBooksScreen - semua buku (LazyVerticalGrid) dengan filter status
// =====================================================================
@Composable
fun AdminBooksScreen(onOpenBook: (String) -> Unit, viewModel: AdminViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadBooks() }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    val filters = listOf(null to "Semua", BookStatus.AVAILABLE to "Tersedia", BookStatus.RESERVED to "Dipesan", BookStatus.DONATED to "Didonasikan")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Semua buku", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 14.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(filters) { (status, label) ->
                        val count = if (status == null) state.books.size else state.books.count { it.status == status }
                        SelectChip("$label ($count)", state.statusFilter == status, onClick = { viewModel.setStatusFilter(status) })
                    }
                }
            }
        }
        if (state.loading && state.books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
        else if (state.filteredBooks.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("Tidak ada buku") }
        items(state.filteredBooks, key = { it.id }) { book ->
            BookGridItem(book, onClick = { onOpenBook(book.id) }, showStatus = true)
        }
    }
}
