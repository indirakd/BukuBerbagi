package id.bukuberbagi.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bukuberbagi.data.model.Book
import id.bukuberbagi.data.model.BookStatus
import id.bukuberbagi.ui.components.AppTextField
import id.bukuberbagi.ui.components.Avatar
import id.bukuberbagi.ui.components.BackHeader
import id.bukuberbagi.ui.components.BookCover
import id.bukuberbagi.ui.components.CategoryIcons
import id.bukuberbagi.ui.components.CategoryRow
import id.bukuberbagi.ui.components.EmptyState
import id.bukuberbagi.ui.components.LoadingBox
import id.bukuberbagi.ui.components.PaperCard
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.PillStyle
import id.bukuberbagi.ui.components.QuoteText
import id.bukuberbagi.ui.components.SelectChip
import id.bukuberbagi.ui.components.ShowMessage
import id.bukuberbagi.ui.components.StatusBadge
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.Sand

// =====================================================================
// CatalogScreen + pencarian (LazyVerticalGrid)
// =====================================================================
@Composable
fun CatalogScreen(
    initialCategoryId: String?,
    focusSearch: Boolean,
    onOpenBook: (String) -> Unit,
    viewModel: CatalogViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { viewModel.start(initialCategoryId) }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            kotlinx.coroutines.delay(300)
            runCatching { focus.requestFocus() }
        }
    }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    "Temukan buku\nuntukmu",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
                )
                SearchBar(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp),
                    focusRequester = focus
                )
                CategoryRow(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = viewModel::selectCategory,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    buildAnnotatedString {
                        append("Menampilkan ")
                        withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.Bold)) { append("${state.books.size} buku") }
                        state.selectedCategoryName?.let {
                            append(" di kategori ")
                            withStyle(SpanStyle(color = Coral, fontWeight = FontWeight.Bold)) { append(it) }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
                )
            }
        }

        when {
            state.loading && state.books.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
            state.books.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("Belum ada buku yang cocok", subtitle = "Coba kata kunci atau kategori lain.")
            }
            else -> items(state.books, key = { it.id }) { book ->
                BookGridItem(book, onClick = { onOpenBook(book.id) })
            }
        }
    }
}

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Cari judul atau penulis",
            shape = RoundedCornerShape(50),
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(Coral),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Search, contentDescription = "Cari", tint = Color.White)
        }
    }
}

/** Item grid: buku 3D + judul + penulis + kategori. Dipakai juga oleh admin. */
@Composable
fun BookGridItem(book: Book, onClick: () -> Unit, showStatus: Boolean = false, currentUserId: String? = null) {
    Column(
        Modifier.fillMaxWidth().bounceClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BookCover(book.title, book.author, book.imageUrl, book.id, 118.dp, icon = CategoryIcons.of(book.category?.icon))
        Column(Modifier.width(118.dp).padding(top = 12.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(book.author, style = MaterialTheme.typography.labelMedium, color = Coral, maxLines = 1)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(book.category?.name.orEmpty(), style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
                if (showStatus) StatusBadge(book.status)
                else if (currentUserId != null && book.userId == currentUserId) {
                    Text("Milikmu", style = MaterialTheme.typography.labelSmall, color = Color(0xFF3C3489))
                }
            }
        }
    }
}

// =====================================================================
// BookDetailScreen
// =====================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onRequest: (String) -> Unit,
    onEdit: (String) -> Unit,
    onOpenRequest: (String) -> Unit,
    viewModel: BookDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(bookId) { viewModel.load(bookId) }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    Column(Modifier.fillMaxSize()) {
        BackHeader("", onBack)
        val book = state.book
        if (book == null) {
            if (state.loading) LoadingBox() else EmptyState("Buku tidak ditemukan")
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BookCover(book.title, book.author, book.imageUrl, book.id, 170.dp, icon = CategoryIcons.of(book.category?.icon))
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.headlineSmall)
                    Text(book.author, style = MaterialTheme.typography.titleSmall, color = Coral, modifier = Modifier.padding(top = 4.dp))
                }
                StatusBadge(book.status)
            }
            FlowRow(
                Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SelectChip(book.category?.name ?: "-", false, null, icon = Icons.Outlined.Category)
                SelectChip(book.conditionLabel, false, null, icon = Icons.Outlined.AutoAwesome)
                if (!book.location.isNullOrBlank()) SelectChip(book.location, false, null, icon = Icons.Outlined.LocationOn)
            }
            QuoteText(book.description?.ifBlank { null } ?: "Tidak ada deskripsi.")
            Spacer(Modifier.height(14.dp))
            PaperCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(book.owner?.fullName.orEmpty(), book.owner?.profileImageUrl, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(book.owner?.fullName ?: "Donatur", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Donatur" + (book.owner?.location?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // Tombol sesuai aturan bisnis
            val active = state.myActiveRequest
            when {
                isAdmin -> InfoPill("Mode admin: hanya melihat")
                state.isOwner -> PillButton(
                    "Ini bukumu, edit", { onEdit(book.id) }, Modifier.fillMaxWidth(),
                    style = PillStyle.Outline, icon = Icons.Outlined.Edit
                )
                active != null -> PillButton(
                    "Permintaanmu sedang diproses", { onOpenRequest(active.id) }, Modifier.fillMaxWidth(),
                    style = PillStyle.Light
                )
                book.status != BookStatus.AVAILABLE -> InfoPill("Buku sudah tidak tersedia")
                else -> PillButton(
                    "Minta buku ini", { onRequest(book.id) }, Modifier.fillMaxWidth(),
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward
                )
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Sand).padding(12.dp)
    )
}
