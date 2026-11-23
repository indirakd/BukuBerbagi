package id.bukuberbagi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.bukuberbagi.ui.theme.Fraunces
import id.bukuberbagi.ui.theme.Line
import id.bukuberbagi.ui.theme.Nunito
import id.bukuberbagi.ui.theme.Paper
import id.bukuberbagi.ui.theme.Ribbon
import id.bukuberbagi.ui.theme.paletteFor

/** Bentuk pita pembatas dengan takik di bawah. */
val RibbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(size.width / 2f, size.height * 0.78f)
    lineTo(0f, size.height)
    close()
}

/**
 * Sampul buku bergaya 3D: sampul depan + punggung + lapisan halaman + pita pembatas.
 * Jika [imageUrl] ada, foto dari Supabase Storage dipakai sebagai sampul.
 */
@Composable
fun BookCover(
    title: String,
    author: String,
    imageUrl: String?,
    seed: String,
    width: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val (front, back) = if (imageUrl.isNullOrBlank()) paletteFor(seed) else Color(0xFF7A6A5C) to Color(0xFF54483D)
    val coverHeight = width * 1.38f
    val pageHeight = maxOf(8.dp, width * 0.13f)
    val small = width < 70.dp
    val corner = if (small) 6.dp else 10.dp

    Box(modifier = modifier.width(width).height(coverHeight + pageHeight + 4.dp)) {
        // Punggung belakang + lapisan halaman putih
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 4.dp)
                .fillMaxWidth()
                .height(pageHeight * 2)
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = corner, bottomEnd = corner))
                .background(back)
                .padding(start = pageHeight * 0.7f, bottom = pageHeight * 0.45f),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(pageHeight * 0.75f)
                    .clip(RoundedCornerShape(topStart = pageHeight, bottomStart = pageHeight))
                    .background(Paper)
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        }

        // Sampul depan
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(coverHeight)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = corner, bottomEnd = corner, bottomStart = 4.dp))
                .background(front)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!small) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = width * 0.15f, end = width * 0.1f, top = width * 0.12f, bottom = width * 0.1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontFamily = Fraunces,
                        fontSize = maxOf(12f, width.value * 0.12f).sp,
                        lineHeight = (maxOf(12f, width.value * 0.12f) * 1.15f).sp,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (icon != null) {
                        Icon(
                            icon, contentDescription = null, tint = Paper,
                            modifier = Modifier.align(Alignment.CenterHorizontally).size(width * 0.28f)
                        )
                    }
                    Text(
                        text = author,
                        fontFamily = Nunito,
                        fontSize = maxOf(9f, width.value * 0.075f).sp,
                        color = Color(0xFFF4EEE6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Punggung buku di sisi kiri
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(if (small) 3.dp else 7.dp)
                    .background(back.copy(alpha = if (imageUrl.isNullOrBlank()) 1f else 0.85f))
            )
        }

        // Pita pembatas
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = width * 0.16f)
                .offset(y = if (small) 2.dp else 8.dp)
                .width(width * 0.13f)
                .height(pageHeight * 2 + if (small) 2.dp else 12.dp)
                .clip(RibbonShape)
                .background(Ribbon)
        )
    }
}
