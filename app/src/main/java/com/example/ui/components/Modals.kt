package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.data.Post
import com.example.data.User
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper
import com.example.ui.theme.Paper2
import com.example.ui.theme.PostGradientList

@Composable
fun ComposerDialog(
    currentUser: User,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (text: String, mediaUrls: List<String>, bgColorIndex: Int, visibility: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedBgIndex by remember { mutableStateOf(0) }
    var selectedVisibility by remember { mutableStateOf("public") }
    var isPrivacyDropdownOpen by remember { mutableStateOf(false) }
    var selectedMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    // Preset Ethiopian Cultural Visuals for testing photo posts easily
    val samplePhotos = listOf(
        "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1578922746465-3a80a228f223?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800&auto=format&fit=crop&q=80"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Paper
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Ink)
                    }

                    Text(
                        text = MeskotStrings.get("createPost", currentLanguage),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Ink
                    )

                    Button(
                        onClick = {
                            if (text.isNotBlank() || selectedMediaUrls.isNotEmpty()) {
                                onSubmit(text, selectedMediaUrls, selectedBgIndex, selectedVisibility)
                            }
                        },
                        enabled = text.isNotBlank() || selectedMediaUrls.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_post_btn")
                    ) {
                        Text(text = MeskotStrings.get("post", currentLanguage), fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = LineBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Author & Privacy Selector
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(photoUrl = currentUser.photoUrl, name = currentUser.displayName, size = 48)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = currentUser.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Paper2)
                                    .border(1.dp, LineBorder, RoundedCornerShape(14.dp))
                                    .clickable { isPrivacyDropdownOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val visLabel = when (selectedVisibility) {
                                    "public" -> "🌍 " + MeskotStrings.get("public", currentLanguage)
                                    "friends" -> "👥 " + MeskotStrings.get("friendsLabel", currentLanguage)
                                    else -> "🔒 " + MeskotStrings.get("onlyMe", currentLanguage)
                                }
                                Text(text = "$visLabel ▾", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                            }

                            DropdownMenu(
                                expanded = isPrivacyDropdownOpen,
                                onDismissRequest = { isPrivacyDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🌍 " + MeskotStrings.get("public", currentLanguage)) },
                                    onClick = { selectedVisibility = "public"; isPrivacyDropdownOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("👥 " + MeskotStrings.get("friendsLabel", currentLanguage)) },
                                    onClick = { selectedVisibility = "friends"; isPrivacyDropdownOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔒 " + MeskotStrings.get("onlyMe", currentLanguage)) },
                                    onClick = { selectedVisibility = "onlyme"; isPrivacyDropdownOpen = false }
                                )
                            }
                        }
                    }
                }

                // Text Body
                val bgBrush = PostGradientList.getOrNull(selectedBgIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier.background(Color.White))
                        .border(1.dp, LineBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = if (bgBrush != null) Alignment.Center else Alignment.TopStart
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = {
                            Text(
                                text = MeskotStrings.get("composerPh", currentLanguage),
                                fontSize = if (bgBrush != null) 20.sp else 16.sp,
                                color = if (bgBrush != null) Color.White.copy(alpha = 0.8f) else MutedText,
                                fontWeight = if (bgBrush != null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (bgBrush != null) Color.White else Ink,
                            unfocusedTextColor = if (bgBrush != null) Color.White else Ink
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("composer_text_input")
                    )
                }

                // Attached Photos Preview
                if (selectedMediaUrls.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMediaUrls.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = selectedMediaUrls[i],
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable {
                                            selectedMediaUrls = selectedMediaUrls.toMutableList().also { it.removeAt(i) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "✕", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Gradient Background Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PostGradientList.forEachIndexed { index, brush ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (brush != null) Modifier.background(brush)
                                    else Modifier
                                        .background(Paper2)
                                        .border(1.dp, LineBorder, RoundedCornerShape(8.dp))
                                )
                                .border(
                                    width = if (selectedBgIndex == index) 2.5.dp else 0.dp,
                                    color = if (selectedBgIndex == index) Gold else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedBgIndex = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == 0) {
                                Text(text = "Aa", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                            }
                        }
                    }
                }

                // Quick photo selector
                Text(
                    text = "🖼️ " + MeskotStrings.get("addPhoto", currentLanguage) + " (Habesha Gallery):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(samplePhotos.size) { idx ->
                        val url = samplePhotos[idx]
                        AsyncImage(
                            model = url,
                            contentDescription = "Sample",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (selectedMediaUrls.contains(url)) 2.dp else 0.5.dp,
                                    color = if (selectedMediaUrls.contains(url)) Gold else LineBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (selectedMediaUrls.contains(url)) {
                                        selectedMediaUrls = selectedMediaUrls - url
                                    } else {
                                        selectedMediaUrls = selectedMediaUrls + url
                                        selectedBgIndex = 0 // photos disable gradient mode
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostOptionsMenu(
    post: Post,
    isAuthor: Boolean,
    isAdmin: Boolean,
    isSaved: Boolean,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSaveToggle: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit,
    onInterested: () -> Unit,
    onNotInterested: () -> Unit,
    onToggleNotifs: () -> Unit,
    onCopyLink: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isAuthor) {
                    MenuOptionItem(
                        emoji = "➕",
                        title = MeskotStrings.get("interested", currentLanguage),
                        onClick = { onInterested(); onDismiss() }
                    )
                    MenuOptionItem(
                        emoji = "➖",
                        title = MeskotStrings.get("notInterested", currentLanguage),
                        onClick = { onNotInterested(); onDismiss() }
                    )
                }

                MenuOptionItem(
                    emoji = "🔖",
                    title = if (isSaved) MeskotStrings.get("unsavePostAction", currentLanguage) else MeskotStrings.get("savePostAction", currentLanguage),
                    onClick = { onSaveToggle(); onDismiss() }
                )

                MenuOptionItem(
                    emoji = "🔗",
                    title = MeskotStrings.get("share", currentLanguage),
                    onClick = { onShare(); onDismiss() }
                )

                if (isAuthor) {
                    MenuOptionItem(
                        emoji = "✏️",
                        title = MeskotStrings.get("editPostAction", currentLanguage),
                        onClick = { onEdit() }
                    )
                }

                if (isAuthor || isAdmin) {
                    MenuOptionItem(
                        emoji = "🗑️",
                        title = MeskotStrings.get("deletePost", currentLanguage),
                        isDanger = true,
                        onClick = { onDelete(); onDismiss() }
                    )
                } else {
                    MenuOptionItem(
                        emoji = "🚫",
                        title = MeskotStrings.get("postHidden", currentLanguage),
                        onClick = { onHide(); onDismiss() }
                    )
                    MenuOptionItem(
                        emoji = "⚠️",
                        title = MeskotStrings.get("reportPostAction", currentLanguage),
                        isDanger = true,
                        onClick = { onReport(); onDismiss() }
                    )
                }

                MenuOptionItem(
                    emoji = "🔔",
                    title = MeskotStrings.get("turnOnNotifs", currentLanguage),
                    onClick = { onToggleNotifs(); onDismiss() }
                )

                MenuOptionItem(
                    emoji = "📋",
                    title = MeskotStrings.get("copyLink", currentLanguage),
                    onClick = { onCopyLink(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun MenuOptionItem(
    emoji: String,
    title: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDanger) CrossRed else Ink
        )
    }
}

@Composable
fun TipModal(
    post: Post,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmTip: (Double) -> Unit
) {
    var selectedAmount by remember { mutableStateOf(25.0) }
    var customAmountText by remember { mutableStateOf("") }

    val presetAmounts = listOf(10.0, 25.0, 50.0, 100.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "💰 " + MeskotStrings.get("supportCreator", currentLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Ink
                )

                Text(
                    text = MeskotStrings.get("supportSub", currentLanguage),
                    fontSize = 12.sp,
                    color = MutedText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.forEach { amount ->
                        val isSelected = selectedAmount == amount && customAmountText.isEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Gold else Paper2)
                                .border(1.dp, if (isSelected) GoldDeep else LineBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedAmount = amount
                                    customAmountText = ""
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${amount.toInt()} ETB",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else Ink
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = {
                        customAmountText = it
                        val parsed = it.toDoubleOrNull()
                        if (parsed != null && parsed > 0) selectedAmount = parsed
                    },
                    label = { Text(MeskotStrings.get("customAmount", currentLanguage)) },
                    placeholder = { Text("e.g. 150") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = MeskotStrings.get("cancel", currentLanguage), color = MutedText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirmTip(selectedAmount) },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = MeskotStrings.get("continueToPay", currentLanguage), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentUser: User,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, photoUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(currentUser.displayName) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var photoUrl by remember { mutableStateOf(currentUser.photoUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = MeskotStrings.get("editProfile", currentLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(MeskotStrings.get("fullName", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(MeskotStrings.get("bio", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text(MeskotStrings.get("photo", currentLanguage) + " URL") },
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
                        onClick = { onSave(name, bio, photoUrl) },
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
