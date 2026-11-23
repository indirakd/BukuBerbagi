package id.bukuberbagi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bukuberbagi.data.model.Profile
import id.bukuberbagi.ui.components.Avatar
import id.bukuberbagi.ui.components.BookCover
import id.bukuberbagi.ui.components.CategoryIcons
import id.bukuberbagi.ui.components.CategoryRow
import id.bukuberbagi.ui.components.CircleIconButton
import id.bukuberbagi.ui.components.LoadingBox
import id.bukuberbagi.ui.components.PaperCard
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.PillStyle
import id.bukuberbagi.ui.components.RibbonShape
import id.bukuberbagi.ui.components.SectionHeader
import id.bukuberbagi.ui.components.ShowMessage
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Line
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.MutedLight
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.Ribbon
import id.bukuberbagi.ui.theme.Sand
import id.bukuberbagi.util.DateUtils
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    profile: Profile,
    onSearch: () -> Unit,
    onCategory: (String?) -> Unit,
    onSeeAll: () -> Unit,
    onDonate: () -> Unit,
    onOpenBook: (String) -> Unit,
    onMyRequests: () -> Unit,
    onIncomingRequests: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        // Salam + notifikasi
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.fullName, profile.profileImageUrl, 40.dp)
                Spacer(Modifier.width(10.dp))
                Text("Hai, ${profile.firstName}!", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Box {
                    CircleIconButton(Icons.Outlined.Notifications, "Permintaan masuk", onIncomingRequests)
                    if (state.incomingPending > 0) {
                        Box(Modifier.align(Alignment.TopEnd).offset((-6).dp, 6.dp).size(8.dp).clip(CircleShape).background(Coral))
                    }
                }
            }
        }
        // Pencarian
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cari judul atau penulis",
                    style = MaterialTheme.typography.bodyMedium, color = Muted,
                    modifier = Modifier.weight(1f).bounceClick(pressedScale = 0.98f, onClick = onSearch)
                        .clip(RoundedCornerShape(50)).background(Sand).padding(horizontal = 16.dp, vertical = 13.dp)
                )
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Outlined.Search, "Cari", onSearch, size = 46.dp, background = Coral, tint = Color.White)
            }
        }
        // Kategori
        item {
            CategoryRow(
                categories = state.categories,
                selectedId = "-",
                onSelect = onCategory,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        // Banner
        item {
            PaperCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Selamat berbagi, ${profile.firstName}", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Buku di rakmu bisa membuka jendela dunia orang lain.",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp)
                        )
                        PillButton("Donasikan", onDonate, trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward, compact = true)
                    }
                    StackedBooks()
                }
            }
        }
        // Ringkasan permintaan
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Permintaan saya", state.myActiveRequests, onMyRequests, Modifier.weight(1f))
                SummaryCard("Permintaan masuk", state.incomingPending, onIncomingRequests, Modifier.weight(1f))
            }
        }
        // Buku terbaru (LazyRow)
        item {
            SectionHeader("Baru didonasikan", Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillButton("Lihat semua", onSeeAll, style = PillStyle.Light, compact = true)
                    CircleIconButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Sebelumnya", {
                        scope.launch { rowState.animateScrollToItem((rowState.firstVisibleItemIndex - 1).coerceAtLeast(0)) }
                    }, size = 32.dp)
                    CircleIconButton(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Berikutnya", {
                        scope.launch { rowState.animateScrollToItem(rowState.firstVisibleItemIndex + 1) }
                    }, size = 32.dp)
                }
            }
        }
        item {
            if (state.loading) LoadingBox()
            else if (state.latestBooks.isEmpty()) {
                Text(
                    "Belum ada buku. Jadilah donatur pertama!",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(state.latestBooks, key = { it.id }) { b ->
                        Column(Modifier.width(120.dp).bounceClick { onOpenBook(b.id) }) {
                            BookCover(b.title, b.author, b.imageUrl, b.id, 120.dp, icon = CategoryIcons.of(b.category?.icon))
                            Text(b.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, modifier = Modifier.padding(top = 12.dp))
                            Text(b.author, style = MaterialTheme.typography.labelMedium, color = Coral, maxLines = 1)
                        }
                    }
                }
            }
        }
        // Kabar pembaca (timeline)
        if (state.latestBooks.isNotEmpty()) {
            item { SectionHeader("Kabar pembaca", Modifier.padding(horizontal = 16.dp)) }
            items(state.latestBooks.take(4), key = { "feed_" + it.id }) { b ->
                Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
                    Avatar(b.owner?.fullName.orEmpty(), b.owner?.profileImageUrl, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.owner?.fullName ?: "Donatur", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Baru saja mendonasikan buku ${b.category?.name?.lowercase() ?: ""}. Semoga bermanfaat.",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Coral, modifier = Modifier.size(13.dp))
                            Text(
                                " ${b.title}", style = MaterialTheme.typography.labelMedium, color = Coral,
                                modifier = Modifier.weight(1f).bounceClick { onOpenBook(b.id) }, maxLines = 1
                            )
                            Text(DateUtils.relative(b.createdAt), style = MaterialTheme.typography.labelSmall, color = MutedLight)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PaperCard(modifier, onClick = onClick) {
        Column {
            Text("$value", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
        }
    }
}

/** Ilustrasi tumpukan buku di banner (seperti referensi). */
@Composable
private fun StackedBooks() {
    val books = listOf(
        Triple(Color(0xFF8A6FC0), Color(0xFF644E94), 84),
        Triple(Color(0xFFD4574E), Color(0xFFA23D36), 92),
        Triple(Color(0xFF4F8F5B), Color(0xFF36683F), 78),
        Triple(Color(0xFF5B6FB0), Color(0xFF3F4F86), 88),
        Triple(Color(0xFFD79A3A), Color(0xFFA8741F), 80)
    )
    Column(Modifier.width(92.dp)) {
        books.forEachIndexed { i, (c1, c2, w) ->
            Box(Modifier.padding(start = if (i % 2 == 1) 6.dp else 0.dp, top = 3.dp)) {
                Row(Modifier.width(w.dp).height(15.dp).clip(RoundedCornerShape(4.dp)).background(c1)) {
                    Box(Modifier.width(6.dp).fillMaxHeight().background(c2))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.width(10.dp).fillMaxHeight().background(Paper))
                }
                if (i == 1) {
                    Box(Modifier.padding(start = (w - 24).dp, top = 10.dp).width(6.dp).height(12.dp).clip(RibbonShape).background(Ribbon))
                }
            }
        }
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Line))
    }
}
