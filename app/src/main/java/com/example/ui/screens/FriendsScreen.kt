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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.data.User
import com.example.ui.MeskotViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CardBg
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper2

enum class FriendsSubTab {
    DISCOVER,
    REQUESTS,
    MY_FRIENDS
}

@Composable
fun FriendsScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    allUsers: List<User>,
    friendUids: Set<String>,
    incomingRequests: List<User>,
    outgoingRequests: Set<String>,
    currentLanguage: AppLanguage
) {
    var activeSubTab by remember { mutableStateOf(FriendsSubTab.DISCOVER) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("friends_screen")
    ) {
        // Tab Selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(14.dp),
            color = CardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Discover Tab
                TabButton(
                    title = MeskotStrings.get("tabDiscover", currentLanguage),
                    isSelected = activeSubTab == FriendsSubTab.DISCOVER,
                    onClick = { activeSubTab = FriendsSubTab.DISCOVER },
                    modifier = Modifier.weight(1f)
                )

                // Requests Tab
                TabButton(
                    title = MeskotStrings.get("tabRequests", currentLanguage),
                    badgeCount = incomingRequests.size,
                    isSelected = activeSubTab == FriendsSubTab.REQUESTS,
                    onClick = { activeSubTab = FriendsSubTab.REQUESTS },
                    modifier = Modifier.weight(1f)
                )

                // My Friends Tab
                TabButton(
                    title = MeskotStrings.get("tabMyFriends", currentLanguage),
                    isSelected = activeSubTab == FriendsSubTab.MY_FRIENDS,
                    onClick = { activeSubTab = FriendsSubTab.MY_FRIENDS },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar (if Discover tab)
        if (activeSubTab == FriendsSubTab.DISCOVER) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedText) },
                placeholder = { Text(MeskotStrings.get("searchPeople", currentLanguage), fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("people_search_input"),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
        }

        // SubTab Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            when (activeSubTab) {
                FriendsSubTab.DISCOVER -> {
                    val otherUsers = allUsers.filter { it.uid != currentUser?.uid }
                    val filtered = if (searchQuery.isBlank()) otherUsers else {
                        otherUsers.filter {
                            it.displayName.contains(searchQuery, ignoreCase = true) ||
                                it.bio.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyNotice(text = MeskotStrings.get("noPeople", currentLanguage))
                        }
                    } else {
                        items(filtered) { user ->
                            val isFriend = friendUids.contains(user.uid)
                            val isIncoming = incomingRequests.any { it.uid == user.uid }
                            val isOutgoing = outgoingRequests.contains(user.uid)

                            PersonCard(
                                user = user,
                                isFriend = isFriend,
                                isIncoming = isIncoming,
                                isOutgoing = isOutgoing,
                                currentLanguage = currentLanguage,
                                onProfileClick = { viewModel.openProfile(user) },
                                onAddFriend = { viewModel.sendFriendRequest(user) },
                                onCancelRequest = { viewModel.cancelFriendRequest(user) },
                                onAccept = { viewModel.acceptFriendRequest(user) },
                                onDecline = { viewModel.declineFriendRequest(user) },
                                onMessage = { viewModel.openChat(user) },
                                onUnfriend = { viewModel.unfriend(user) }
                            )
                        }
                    }
                }

                FriendsSubTab.REQUESTS -> {
                    if (incomingRequests.isEmpty()) {
                        item {
                            EmptyNotice(text = MeskotStrings.get("noRequests", currentLanguage))
                        }
                    } else {
                        items(incomingRequests) { user ->
                            PersonCard(
                                user = user,
                                isFriend = false,
                                isIncoming = true,
                                isOutgoing = false,
                                currentLanguage = currentLanguage,
                                onProfileClick = { viewModel.openProfile(user) },
                                onAddFriend = {},
                                onCancelRequest = {},
                                onAccept = { viewModel.acceptFriendRequest(user) },
                                onDecline = { viewModel.declineFriendRequest(user) },
                                onMessage = {},
                                onUnfriend = {}
                            )
                        }
                    }
                }

                FriendsSubTab.MY_FRIENDS -> {
                    val friendsList = allUsers.filter { friendUids.contains(it.uid) }
                    if (friendsList.isEmpty()) {
                        item {
                            EmptyNotice(text = MeskotStrings.get("noFriendsYet", currentLanguage))
                        }
                    } else {
                        items(friendsList) { user ->
                            PersonCard(
                                user = user,
                                isFriend = true,
                                isIncoming = false,
                                isOutgoing = false,
                                currentLanguage = currentLanguage,
                                onProfileClick = { viewModel.openProfile(user) },
                                onAddFriend = {},
                                onCancelRequest = {},
                                onAccept = {},
                                onDecline = {},
                                onMessage = { viewModel.openChat(user) },
                                onUnfriend = { viewModel.unfriend(user) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Ink else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MutedText
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CrossRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badgeCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PersonCard(
    user: User,
    isFriend: Boolean,
    isIncoming: Boolean,
    isOutgoing: Boolean,
    currentLanguage: AppLanguage,
    onProfileClick: () -> Unit,
    onAddFriend: () -> Unit,
    onCancelRequest: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit,
    onUnfriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onProfileClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(photoUrl = user.photoUrl, name = user.displayName, size = 52)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        fontSize = 12.sp,
                        color = MutedText,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            when {
                isFriend -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = onMessage,
                            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(MeskotStrings.get("messageBtn", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                isIncoming -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(MeskotStrings.get("accept", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onDecline,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(MeskotStrings.get("decline", currentLanguage), fontSize = 11.sp, color = Ink)
                        }
                    }
                }
                isOutgoing -> {
                    OutlinedButton(
                        onClick = onCancelRequest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(MeskotStrings.get("requested", currentLanguage), fontSize = 11.sp, color = MutedText)
                    }
                }
                else -> {
                    Button(
                        onClick = onAddFriend,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(MeskotStrings.get("addFriend", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MutedText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
