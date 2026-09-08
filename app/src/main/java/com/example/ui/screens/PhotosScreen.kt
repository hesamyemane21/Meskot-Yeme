package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.AlbumItem
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.ui.MeskotViewModel
import com.example.ui.ScreenTab
import com.example.ui.theme.CardBg
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper2

@Composable
fun PhotosScreen(
    viewModel: MeskotViewModel,
    albums: List<AlbumItem>,
    currentLanguage: AppLanguage
) {
    var isCreateAlbumOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("photos_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = MeskotStrings.get("myAlbums", currentLanguage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Ink
            )

            Button(
                onClick = { isCreateAlbumOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "+ " + MeskotStrings.get("createAlbum", currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums) { album ->
                AlbumCard(
                    album = album,
                    currentLanguage = currentLanguage,
                    onClick = { viewModel.openAlbumDetail(album) }
                )
            }
        }
    }

    if (isCreateAlbumOpen) {
        CreateAlbumDialog(
            currentLanguage = currentLanguage,
            onDismiss = { isCreateAlbumOpen = false },
            onCreate = { title ->
                viewModel.createAlbum(title)
                isCreateAlbumOpen = false
            }
        )
    }
}

@Composable
fun AlbumCard(
    album: AlbumItem,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Paper2)
            ) {
                if (album.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "🖼️", fontSize = 32.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = album.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Ink,
                    maxLines = 1
                )
                Text(
                    text = "${album.count} ${MeskotStrings.get("items", currentLanguage)}",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    viewModel: MeskotViewModel,
    album: AlbumItem,
    currentLanguage: AppLanguage
) {
    var lightboxIndex by remember { mutableStateOf<Int?>(null) }
    var isAddPhotoOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("album_detail_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenTab.PHOTOS) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
            }

            Text(
                text = album.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Ink,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { isAddPhotoOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = "+ " + MeskotStrings.get("addMedia", currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Photo Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            itemsIndexed(album.photos) { index, photoUrl ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { lightboxIndex = index }
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    // Fullscreen Lightbox Viewer
    if (lightboxIndex != null) {
        val currentIdx = lightboxIndex!!
        Dialog(
            onDismissRequest = { lightboxIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                // Close button
                IconButton(
                    onClick = { lightboxIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                // Main Image
                AsyncImage(
                    model = album.photos[currentIdx],
                    contentDescription = "Enlarged photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )

                // Navigation Arrows
                if (album.photos.size > 1) {
                    IconButton(
                        onClick = {
                            lightboxIndex = (currentIdx - 1 + album.photos.size) % album.photos.size
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            lightboxIndex = (currentIdx + 1) % album.photos.size
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.White)
                    }
                }
            }
        }
    }

    // Quick Add Photo Dialog
    if (isAddPhotoOpen) {
        var photoUrlInput by remember {
            mutableStateOf("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&auto=format&fit=crop&q=80")
        }

        Dialog(onDismissRequest = { isAddPhotoOpen = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = MeskotStrings.get("addMedia", currentLanguage),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = photoUrlInput,
                        onValueChange = { photoUrlInput = it },
                        label = { Text("Photo Image URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAddPhotoOpen = false }) {
                            Text(text = MeskotStrings.get("cancel", currentLanguage), color = MutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (photoUrlInput.isNotBlank()) {
                                    viewModel.addPhotoToAlbum(album.id, photoUrlInput)
                                    isAddPhotoOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = MeskotStrings.get("save", currentLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateAlbumDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = MeskotStrings.get("createAlbum", currentLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(MeskotStrings.get("albumTitle", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = MeskotStrings.get("cancel", currentLanguage), color = MutedText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onCreate(title) },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = MeskotStrings.get("create", currentLanguage), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
