package id.bukuberbagi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.bukuberbagi.ui.theme.Brown
import id.bukuberbagi.ui.theme.Coral
import id.bukuberbagi.ui.theme.Line
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.SandDark
import id.bukuberbagi.ui.theme.paletteFor

/** Pemetaan kolom categories.icon -> ikon Material. */
object CategoryIcons {
    val options: List<Pair<String, ImageVector>> = listOf(
        "school" to Icons.Outlined.School,
        "science" to Icons.Outlined.Science,
        "religion" to Icons.Outlined.NightsStay,
        "fiction" to Icons.Outlined.AutoStories,
        "child" to Icons.Outlined.ChildCare,
        "growth" to Icons.Outlined.Psychology,
        "history" to Icons.Outlined.HistoryEdu,
        "art" to Icons.Outlined.Palette,
        "language" to Icons.Outlined.Translate,
        "food" to Icons.Outlined.Restaurant,
        "bookmark" to Icons.Outlined.Bookmark
    )

    fun of(key: String?): ImageVector = options.firstOrNull { it.first == key }?.second ?: Icons.Outlined.Bookmark
}

/**
 * Ikon kategori berbentuk buku mini. Saat [selected], buku terangkat dan miring,
 * bayangannya mengecil, dan label berwarna koral (sesuai permintaan revisi).
 * [seed] dipakai untuk memilih warna; [isAll] menampilkan tumpukan buku untuk "Semua".
 */
@Composable
fun CategoryBookIcon(
    label: String,
    seed: String,
    iconKey: String?,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isAll: Boolean = false
) {
    val spec = spring<androidx.compose.ui.unit.Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val lift by animateDpAsState(if (selected) (-9).dp else 0.dp, spec, label = "lift")
    val tilt by animateFloatAsState(
        if (selected) -8f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tilt"
    )
    val shadowScale by animateFloatAsState(if (selected) 0.7f else 1f, label = "shadow")
    val labelColor by animateColorAsState(if (selected) Coral else Brown, label = "labelColor")
    val (front, back) = paletteFor(seed)

    val clickable = if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier
    Column(
        modifier = modifier.width(56.dp).then(clickable).padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(46.dp)
                .offset(y = lift)
                .rotate(tilt),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (isAll) {
                Column {
                    listOf(
                        Triple(Color(0xFFD4574E), Color(0xFFA23D36), 34),
                        Triple(Color(0xFF4F8F5B), Color(0xFF36683F), 38),
                        Triple(Color(0xFF5B6FB0), Color(0xFF3F4F86), 32)
                    ).forEach { (c1, c2, w) ->
                        Row(
                            Modifier
                                .padding(top = 2.dp)
                                .width(w.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(c1)
                        ) {
                            Box(Modifier.width(4.dp).fillMaxHeight().background(c2))
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.width(7.dp).fillMaxHeight().background(Paper))
                        }
                    }
                }
            } else {
                Column(Modifier.width(34.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                            .background(front)
                    ) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(back))
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(CategoryIcons.of(iconKey), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(
                        Modifier
                            .offset(y = (-4).dp)
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                            .background(back)
                            .padding(start = 4.dp, bottom = 3.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                                .background(Paper)
                        )
                    }
                }
            }
        }
        // Bayangan elips di bawah buku
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(44.dp)
                .height(5.dp)
                .graphicsLayer { scaleX = shadowScale }
                .clip(CircleShape)
                .background(if (selected) SandDark.copy(alpha = 0.6f) else Line)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1)
    }
}

/** Baris horizontal kategori dengan pilihan "Semua". */
@Composable
fun CategoryRow(
    categories: List<id.bukuberbagi.data.model.Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = true
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        if (showAll) {
            item(key = "all") {
                CategoryBookIcon(
                    label = "Semua", seed = "all", iconKey = null,
                    selected = selectedId == null, onClick = { onSelect(null) }, isAll = true
                )
            }
        }
        items(categories.size, key = { categories[it].id }) { index ->
            val c = categories[index]
            CategoryBookIcon(
                label = c.name.substringBefore(" "),
                seed = c.id,
                iconKey = c.icon,
                selected = selectedId == c.id,
                onClick = { onSelect(c.id) }
            )
        }
    }
}
