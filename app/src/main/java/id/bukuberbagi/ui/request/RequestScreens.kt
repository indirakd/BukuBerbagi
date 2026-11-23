package id.bukuberbagi.ui.request

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bukuberbagi.data.model.DonationRequest
import id.bukuberbagi.data.model.HandoverMethod
import id.bukuberbagi.data.model.RequestStatus
import id.bukuberbagi.ui.components.AppTextField
import id.bukuberbagi.ui.components.Avatar
import id.bukuberbagi.ui.components.BackHeader
import id.bukuberbagi.ui.components.BookCover
import id.bukuberbagi.ui.components.ConfirmDialog
import id.bukuberbagi.ui.components.EmptyState
import id.bukuberbagi.ui.components.ErrorText
import id.bukuberbagi.ui.components.FieldLabel
import id.bukuberbagi.ui.components.LoadingBox
import id.bukuberbagi.ui.components.PaperCard
import id.bukuberbagi.ui.components.PillButton
import id.bukuberbagi.ui.components.PillStyle
import id.bukuberbagi.ui.components.QuoteText
import id.bukuberbagi.ui.components.SectionHeader
import id.bukuberbagi.ui.components.SelectChip
import id.bukuberbagi.ui.components.ShowMessage
import id.bukuberbagi.ui.components.StatusBadge
import id.bukuberbagi.ui.components.bounceClick
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Ink
import id.bukuberbagi.ui.theme.Muted
import id.bukuberbagi.ui.theme.MutedLight
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.Sand
import id.bukuberbagi.ui.theme.SandDark
import id.bukuberbagi.util.DateUtils
import java.time.LocalDate

// =====================================================================
// RequestDonationScreen - form minta buku
// =====================================================================
@Composable
fun RequestDonationScreen(
    bookId: String,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: DonationRequestViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var reason by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(bookId) { viewModel.loadBook(bookId) }
    LaunchedEffect(state.createdEvent) {
        if (state.createdEvent) { viewModel.consumeCreated(); onCreated() }
    }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Minta buku", onBack)
        val book = state.book
        if (book == null) {
            LoadingBox(); return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            PaperCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCover(book.title, book.author, book.imageUrl, book.id, 50.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(book.title, style = MaterialTheme.typography.titleSmall)
                        Text("Donatur: ${book.owner?.fullName ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            FieldLabel("Kenapa kamu membutuhkan buku ini?")
            AppTextField(
                reason, { reason = it }, placeholder = "Saya siswa kelas XI dan belum punya buku paket ini",
                singleLine = false, minLines = 5, error = state.reasonError
            )
            Text(
                "Buku diberikan gratis dan menjadi milikmu. Penyerahan diatur setelah donatur menyetujui.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)
            )
            PillButton(
                "Kirim permintaan", { viewModel.submitRequest(reason) },
                Modifier.fillMaxWidth().padding(top = 18.dp), loading = state.submitting
            )
        }
    }
}

// =====================================================================
// RequestsScreen - tab "Permintaan saya" & "Permintaan masuk"
// =====================================================================
@Composable
fun RequestsScreen(
    initialTab: String,
    onOpenRequest: (String) -> Unit,
    onBrowse: () -> Unit,
    viewModel: DonationRequestViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(if (initialTab == "in") "in" else "mine") }
    LaunchedEffect(Unit) { viewModel.loadLists() }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    Column(Modifier.fillMaxSize()) {
        Text("Permintaan", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 12.dp))
        RequestTabs(
            selected = tab,
            mineCount = state.myActiveCount,
            inCount = state.incomingPendingCount,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            if (tab == "mine") "Buku yang kamu minta dari donatur lain."
            else "Orang lain yang ingin menerima bukumu. Setujui atau tolak di sini.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (tab == "mine") {
            MyRequestsScreen(state.myRequests, state.loading, onOpenRequest, onBrowse)
        } else {
            IncomingRequestsScreen(state.incomingRequests, state.loading, onOpenRequest)
        }
    }
}

@Composable
private fun RequestTabs(selected: String, mineCount: Int, inCount: Int, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Sand).padding(4.dp)
    ) {
        listOf(Triple("mine", "Permintaan saya", mineCount), Triple("in", "Permintaan masuk", inCount)).forEach { (key, label, count) ->
            val on = key == selected
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Paper else Color.Transparent)
                    .bounceClick { onSelect(key) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Ink else Muted)
                if (count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$count", style = MaterialTheme.typography.labelSmall, color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Coral else SandDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/** Daftar permintaan yang dibuat pengguna (LazyColumn). */
@Composable
fun MyRequestsScreen(requests: List<DonationRequest>, loading: Boolean, onOpen: (String) -> Unit, onBrowse: () -> Unit) {
    RequestList(requests, loading, mine = true, onOpen = onOpen) {
        EmptyState("Kamu belum meminta buku", actionText = "Cari di katalog", onAction = onBrowse)
    }
}

/** Daftar permintaan yang masuk ke donatur (LazyColumn). */
@Composable
fun IncomingRequestsScreen(requests: List<DonationRequest>, loading: Boolean, onOpen: (String) -> Unit) {
    RequestList(requests, loading, mine = false, onOpen = onOpen) {
        EmptyState("Belum ada yang meminta bukumu")
    }
}

@Composable
private fun RequestList(
    requests: List<DonationRequest>,
    loading: Boolean,
    mine: Boolean,
    onOpen: (String) -> Unit,
    empty: @Composable () -> Unit
) {
    when {
        loading && requests.isEmpty() -> LoadingBox()
        requests.isEmpty() -> empty()
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(requests, key = { it.id }) { r ->
                val other = if (mine) r.donor else r.requester
                PaperCard(Modifier.fillMaxWidth(), onClick = { onOpen(r.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BookCover(r.book?.title.orEmpty(), r.book?.author.orEmpty(), r.book?.imageUrl, r.bookId, 50.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.book?.title ?: "-", style = MaterialTheme.typography.titleSmall, maxLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Avatar(other?.fullName.orEmpty(), other?.profileImageUrl, 18.dp)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "${if (mine) "Donatur" else "Peminta"}: ${other?.fullName ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(r.status)
                                Spacer(Modifier.width(8.dp))
                                Text(DateUtils.relative(r.createdAt), style = MaterialTheme.typography.labelSmall, color = MutedLight)
                            }
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MutedLight)
                    }
                }
            }
        }
    }
}

// =====================================================================
// RequestDetailScreen
// =====================================================================
@Composable
fun RequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    onHandover: (String) -> Unit,
    viewModel: DonationRequestViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmAction by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(requestId) { viewModel.loadDetail(requestId) }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    val r = state.request
    Column(Modifier.fillMaxSize()) {
        BackHeader(if (state.isDonor) "Permintaan masuk" else "Permintaan saya", onBack)
        if (r == null) {
            if (state.loading) LoadingBox() else EmptyState("Permintaan tidak ditemukan")
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            PaperCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCover(r.book?.title.orEmpty(), r.book?.author.orEmpty(), r.book?.imageUrl, r.bookId, 60.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.book?.title ?: "-", style = MaterialTheme.typography.titleMedium)
                        Text(r.book?.author.orEmpty(), style = MaterialTheme.typography.labelMedium, color = Coral)
                        Spacer(Modifier.height(6.dp))
                        StatusBadge(r.status)
                    }
                }
            }

            StatusStepper(r.status)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PartyCard("Peminta", r.requester?.fullName.orEmpty(), r.requester?.profileImageUrl, state.isRequester, Modifier.weight(1f))
                PartyCard("Donatur", r.donor?.fullName.orEmpty(), r.donor?.profileImageUrl, state.isDonor, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            PaperCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Alasan", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    QuoteText(r.reason)
                }
            }
            Spacer(Modifier.height(14.dp))

            when {
                state.isDonor && r.status == RequestStatus.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Tolak", { confirmAction = "reject" }, Modifier.weight(1f), style = PillStyle.DangerOutline, enabled = !state.actionLoading)
                    PillButton("Setujui", { confirmAction = "approve" }, Modifier.weight(1f), loading = state.actionLoading)
                }
                state.isRequester && r.status == RequestStatus.PENDING -> PillButton(
                    "Batalkan permintaan", { confirmAction = "cancel" }, Modifier.fillMaxWidth(),
                    style = PillStyle.DangerOutline, loading = state.actionLoading
                )
                r.status == RequestStatus.APPROVED -> PillButton(
                    "Atur dan konfirmasi penyerahan", { onHandover(r.id) }, Modifier.fillMaxWidth(),
                    style = PillStyle.Coral, icon = Icons.Outlined.CalendarMonth
                )
                r.status == RequestStatus.COMPLETED -> DoneBanner()
            }
        }
    }

    confirmAction?.let { action ->
        val (title, msg, btn) = when (action) {
            "approve" -> Triple("Setujui permintaan?", "Buku akan dipesan untuk peminta ini. Permintaan lain untuk buku yang sama otomatis ditolak.", "Setujui")
            "reject" -> Triple("Tolak permintaan?", "Peminta akan melihat status Ditolak. Bukumu tetap tersedia.", "Tolak")
            else -> Triple("Batalkan permintaan?", "Permintaanmu untuk buku ini akan dibatalkan.", "Batalkan")
        }
        ConfirmDialog(title, msg, btn, onConfirm = {
            when (action) {
                "approve" -> viewModel.approve()
                "reject" -> viewModel.reject()
                else -> viewModel.cancel()
            }
            confirmAction = null
        }, onDismiss = { confirmAction = null })
    }
}

@Composable
private fun StatusStepper(status: String) {
    val steps = listOf("Diajukan", "Disetujui", "Selesai")
    val index = when (status) {
        RequestStatus.PENDING -> 0
        RequestStatus.APPROVED -> 1
        RequestStatus.COMPLETED -> 2
        else -> -1
    }
    if (index < 0) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 16.dp), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val active = i <= index
                val pop by animateFloatAsState(
                    if (active) 1f else 0.85f,
                    spring(dampingRatio = Spring.DampingRatioHighBouncy), label = "step"
                )
                Box(
                    Modifier.size(28.dp).graphicsLayer { scaleX = pop; scaleY = pop }
                        .clip(CircleShape).background(if (active) Coral else Sand),
                    contentAlignment = Alignment.Center
                ) {
                    if (i < index || status == RequestStatus.COMPLETED) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (active) Color.White else MutedLight)
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(top = 4.dp))
            }
            if (i < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(top = 14.dp, start = 4.dp, end = 4.dp),
                    thickness = 1.5.dp,
                    color = if (i < index) Coral else SandDark
                )
            }
        }
    }
}

@Composable
private fun PartyCard(role: String, name: String, photo: String?, isMe: Boolean, modifier: Modifier = Modifier) {
    PaperCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, photo, 30.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(role + if (isMe) " (kamu)" else "", style = MaterialTheme.typography.labelSmall, color = Muted)
                Text(name.ifBlank { "-" }, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DoneBanner() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFE3EFD9)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Buku sudah berpindah tangan", style = MaterialTheme.typography.titleMedium, color = Color(0xFF27500A))
        Text("Terima kasih sudah berbagi", style = MaterialTheme.typography.bodySmall, color = Color(0xFF27500A))
    }
}

// =====================================================================
// HandoverConfirmationScreen - jadwal + konfirmasi dua pihak
// =====================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HandoverConfirmationScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: DonationRequestViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(requestId) { viewModel.loadDetail(requestId) }
    ShowMessage(state.message) { viewModel.consumeMessage() }

    val r = state.request
    val h = state.handover
    var method by rememberSaveable { mutableStateOf<String?>(null) }
    var dateText by rememberSaveable { mutableStateOf<String?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var prefilled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(h) {
        if (h != null && !prefilled) {
            method = h.handoverMethod
            dateText = DateUtils.toLocalDate(h.handoverDate)?.toString()
            notes = h.notes.orEmpty()
            prefilled = true
        }
    }
    val selectedDate = dateText?.let { LocalDate.parse(it) }
    val today = remember { LocalDate.now() }
    val days = remember(today) { (0 until 14).map { today.plusDays(it.toLong()) } }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Serah terima", onBack)
        if (r == null) {
            LoadingBox(); return@Column
        }
        if (r.status == RequestStatus.COMPLETED) {
            Column(Modifier.padding(16.dp)) { DoneBanner() }
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(bottom = 24.dp)
        ) {
            PaperCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookCover(r.book?.title.orEmpty(), r.book?.author.orEmpty(), r.book?.imageUrl, r.bookId, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(r.book?.title ?: "-", style = MaterialTheme.typography.titleSmall)
                        Text("${r.donor?.fullName ?: "-"} → ${r.requester?.fullName ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            SectionHeader("Jadwal penyerahan", Modifier.padding(horizontal = 16.dp)) {
                Text(DateUtils.monthYear(selectedDate ?: today), style = MaterialTheme.typography.bodySmall)
            }
            // Deretan tanggal seperti "Schedule Reading" di referensi
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(days, key = { it.toString() }) { d ->
                    val on = d == selectedDate
                    val sunday = d.dayOfWeek.value == 7
                    Column(
                        Modifier
                            .then(if (state.isDonor) Modifier.bounceClick { dateText = d.toString() } else Modifier)
                            .width(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (on) Sand else Color.Transparent)
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(DateUtils.dayShort(d), style = MaterialTheme.typography.labelSmall, color = if (sunday) Coral else Muted)
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleSmall, color = if (sunday) Coral else Ink, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (selectedDate != null) {
                Text(DateUtils.longDate(selectedDate), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                FieldLabel("Metode")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HandoverMethod.entries.forEach { m ->
                        SelectChip(m.label, method == m.code, onClick = if (state.isDonor) ({ method = m.code }) else null)
                    }
                }
                FieldLabel("Catatan")
                AppTextField(
                    notes, { notes = it }, placeholder = "Ketemu di depan perpustakaan kampus",
                    singleLine = false, minLines = 2, enabled = state.isDonor
                )
                if (state.isDonor) {
                    PillButton(
                        "Simpan jadwal", { viewModel.saveSchedule(method, selectedDate, notes) },
                        Modifier.fillMaxWidth().padding(top = 10.dp), style = PillStyle.Outline,
                        enabled = !state.actionLoading
                    )
                } else {
                    Text("Jadwal diatur oleh donatur", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                ErrorText(state.scheduleError)

                SectionHeader("Konfirmasi kedua pihak")
                ConfirmRow("Donatur sudah menyerahkan", h?.donorConfirmed == true, state.isDonor, state.actionLoading) {
                    viewModel.confirmHandover()
                }
                Spacer(Modifier.height(9.dp))
                ConfirmRow("Penerima sudah menerima", h?.requesterConfirmed == true, state.isRequester, state.actionLoading) {
                    viewModel.confirmHandover()
                }
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, confirmed: Boolean, mine: Boolean, busy: Boolean, onConfirm: () -> Unit) {
    val pop by animateFloatAsState(
        if (confirmed) 1f else 0.8f, spring(dampingRatio = Spring.DampingRatioHighBouncy), label = "confirm"
    )
    PaperCard(Modifier.fillMaxWidth(), onClick = if (mine && !confirmed && !busy) onConfirm else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).graphicsLayer { scaleX = pop; scaleY = pop }
                    .clip(CircleShape).background(if (confirmed) Coral else Sand),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        confirmed -> "Sudah dikonfirmasi"
                        mine -> "Ketuk untuk konfirmasi"
                        else -> "Menunggu konfirmasi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (confirmed) Coral else Muted
                )
            }
        }
    }
}
