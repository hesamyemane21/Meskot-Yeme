package com.example.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.data.Post
import com.example.data.User
import com.example.ui.MeskotViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CardBg
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper
import com.example.ui.theme.Paper2

@Composable
fun ProfileScreen(
    viewModel: MeskotViewModel,
    user: User,
    currentUser: User?,
    userPosts: List<Post>,
    friendUids: Set<String>,
    currentLanguage: AppLanguage
) {
    val isMe = currentUser?.uid == user.uid
    val isFriend = friendUids.contains(user.uid)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen")
    ) {
        // Back Header if viewing other user
        if (!isMe) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateTo(ScreenTab.FEED) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
                    }
                    Text(
                        text = user.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
            }
        }

        // Profile Hero Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Cover Banner with Ethiopian Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Brush.horizontalGradient(listOf(Gold, CrossRed, GoldDeep)))
                    )

                    // Overlapping Avatar
                    Box(
                        modifier = Modifier
                            .offset(y = (-40).dp)
                            .padding(bottom = (-30).dp)
                    ) {
                        UserAvatar(
                            photoUrl = user.photoUrl,
                            name = user.displayName,
                            size = 84,
                            modifier = Modifier.border(3.dp, Color.White, CircleShape)
                        )
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Ink
                            )
                            if (user.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "🛡️", fontSize = 16.sp)
                            }
                        }

                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.bio,
                                fontSize = 13.sp,
                                color = MutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions
                        if (isMe) {
                            Button(
                                onClick = { viewModel.openEditProfile() },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "✏️ " + MeskotStrings.get("editProfile", currentLanguage), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isFriend) {
                                    Button(
                                        onClick = { viewModel.openChat(user) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = "💬 " + MeskotStrings.get("messageBtn", currentLanguage), fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.unfriend(user) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = MeskotStrings.get("unfriend", currentLanguage), color = CrossRed)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.sendFriendRequest(user) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = "+ " + MeskotStrings.get("addFriend", currentLanguage), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // User's Posts Feed
        item {
            Text(
                text = MeskotStrings.get("posts", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        if (userPosts.isEmpty()) {
            item {
                EmptyNotice(text = MeskotStrings.get("noPostsYet", currentLanguage))
            }
        } else {
            items(userPosts) { post ->
                val comments = viewModel.getComments(post.id)
                PostCard(
                    post = post,
                    currentUserId = currentUser?.uid,
                    comments = comments,
                    currentLanguage = currentLanguage,
                    onAuthorClick = { viewModel.openProfileByUid(it) },
                    onToggleReaction = { pid, type -> viewModel.toggleReaction(pid, type) },
                    onShare = { viewModel.sharePost(it) },
                    onToggleSave = { viewModel.toggleSavePost(it) },
                    onTipClick = { viewModel.openTipModal(it) },
                    onOpenMenu = { viewModel.openPostMenu(it) },
                    onAddComment = { pid, text, parentId -> viewModel.addComment(pid, text, parentId) },
                    onToggleCommentLike = { pid, cid -> viewModel.toggleCommentLike(pid, cid) },
                    onDeleteComment = { pid, cid -> viewModel.deleteComment(pid, cid) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MenuScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    allUsers: List<User>,
    currentLanguage: AppLanguage
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("menu_screen")
    ) {
        // User Profile Banner
        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openProfile(currentUser) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(photoUrl = currentUser.photoUrl, name = currentUser.displayName, size = 52)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = currentUser.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(text = MeskotStrings.get("viewProfile", currentLanguage), fontSize = 12.sp, color = GoldDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Grid Menu Options
        item {
            val menuItems = listOf(
                Triple("🏠", MeskotStrings.get("navFeed", currentLanguage), ScreenTab.FEED),
                Triple("👥", MeskotStrings.get("navFriends", currentLanguage), ScreenTab.FRIENDS),
                Triple("💬", MeskotStrings.get("navMessages", currentLanguage), ScreenTab.MESSAGES),
                Triple("👪", MeskotStrings.get("navGroups", currentLanguage), ScreenTab.GROUPS),
                Triple("🖼️", MeskotStrings.get("navPhotos", currentLanguage), ScreenTab.PHOTOS),
                Triple("🔔", MeskotStrings.get("navNotifs", currentLanguage), ScreenTab.NOTIFICATIONS),
                Triple("📊", "Dashboard", ScreenTab.DASHBOARD),
                Triple("🔖", MeskotStrings.get("savedPosts", currentLanguage), ScreenTab.SAVED)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                menuItems.take(2).forEach { item ->
                    MenuShortcutCard(
                        emoji = item.first,
                        title = item.second,
                        onClick = { viewModel.navigateTo(item.third) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                menuItems.drop(2).take(2).forEach { item ->
                    MenuShortcutCard(
                        emoji = item.first,
                        title = item.second,
                        onClick = { viewModel.navigateTo(item.third) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                menuItems.drop(4).take(2).forEach { item ->
                    MenuShortcutCard(
                        emoji = item.first,
                        title = item.second,
                        onClick = { viewModel.navigateTo(item.third) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                menuItems.drop(6).take(2).forEach { item ->
                    MenuShortcutCard(
                        emoji = item.first,
                        title = item.second,
                        onClick = { viewModel.navigateTo(item.third) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Admin Panel if Admin
        if (currentUser?.isAdmin == true) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                MenuShortcutCard(
                    emoji = "🛡️",
                    title = MeskotStrings.get("adminPanel", currentLanguage),
                    onClick = { viewModel.navigateTo(ScreenTab.ADMIN) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Language & Log Out
        item {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleLanguage() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🌐 " + MeskotStrings.get("language", currentLanguage), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        text = if (currentLanguage == AppLanguage.EN) "English (US)" else "አማርኛ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldDeep
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.logout() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚪 " + MeskotStrings.get("logOut", currentLanguage), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CrossRed)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MenuShortcutCard(
    emoji: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }
    }
}

@Composable
fun AuthScreen(
    viewModel: MeskotViewModel,
    allUsers: List<User> = emptyList(),
    currentLanguage: AppLanguage
) {
    var isSignUp by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(24.dp)
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Meskot Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(Brush.linearGradient(listOf(Gold, GoldDeep)))
                .border(2.dp, Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(52.dp)
                    .background(Color.White.copy(alpha = 0.8f))
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Meskot",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = Ink,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "መስኮት · Ethiopian & Habesha Community Network",
            fontSize = 12.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Tab switcher (Sign in vs Register)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Paper2)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isSignUp) Ink else Color.Transparent)
                            .clickable {
                                isSignUp = false
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = MeskotStrings.get("signIn", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!isSignUp) Color.White else MutedText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSignUp) Ink else Color.Transparent)
                            .clickable {
                                isSignUp = true
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = MeskotStrings.get("createAccount", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSignUp) Color.White else MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSignUp) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(MeskotStrings.get("fullName", currentLanguage)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(MeskotStrings.get("email", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(MeskotStrings.get("password", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MutedText
                            )
                        }
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = CrossRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        if (isSignUp) {
                            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all fields"
                            } else {
                                isLoading = true
                                viewModel.signup(fullName, email, password) { ok, err ->
                                    isLoading = false
                                    if (!ok) {
                                        errorMessage = err ?: "Failed to create account"
                                    }
                                }
                            }
                        } else {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password"
                            } else {
                                isLoading = true
                                viewModel.login(email, password) { ok, err ->
                                    isLoading = false
                                    if (!ok) {
                                        errorMessage = err ?: "Invalid email or password"
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) MeskotStrings.get("createAccount", currentLanguage) else MeskotStrings.get("signIn", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
