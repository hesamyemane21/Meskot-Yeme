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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.data.NotificationItem
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
import com.example.ui.theme.Paper2

@Composable
fun NotificationsScreen(
    viewModel: MeskotViewModel,
    notifications: List<NotificationItem>,
    currentLanguage: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = MeskotStrings.get("navNotifs", currentLanguage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Ink
            )

            TextButton(onClick = { viewModel.markAllNotifsRead() }) {
                Text(
                    text = MeskotStrings.get("markAllRead", currentLanguage),
                    fontSize = 13.sp,
                    color = GoldDeep,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    EmptyNotice(text = MeskotStrings.get("noNotifs", currentLanguage))
                }
            } else {
                items(notifications) { notif ->
                    NotificationRow(
                        notif = notif,
                        currentLanguage = currentLanguage,
                        onClick = {
                            when (notif.type) {
                                "friend_req" -> viewModel.navigateTo(ScreenTab.FRIENDS)
                                "message" -> viewModel.navigateTo(ScreenTab.MESSAGES)
                                else -> viewModel.navigateTo(ScreenTab.FEED)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun NotificationRow(
    notif: NotificationItem,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notif.isRead) CardBg else Paper2
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sender Avatar with notification type badge
            Box {
                UserAvatar(photoUrl = notif.fromPhoto, name = notif.fromName, size = 46)

                val badgeEmoji = when (notif.type) {
                    "like" -> "❤️"
                    "comment" -> "💬"
                    "friend_req" -> "👥"
                    "tip" -> "💰"
                    "call" -> "📞"
                    else -> "🔔"
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, LineBorder, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badgeEmoji, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notif.text,
                    fontSize = 14.sp,
                    color = Ink,
                    lineHeight = 18.sp,
                    fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.SemiBold
                )
                Text(
                    text = MeskotStrings.timeAgo(notif.createdAt, currentLanguage),
                    fontSize = 11.sp,
                    color = MutedText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!notif.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Gold)
                )
            }
        }
    }
}

@Composable
fun SavedScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    savedPosts: List<Post>,
    currentLanguage: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_screen")
    ) {
        Text(
            text = "🔖 " + MeskotStrings.get("savedPosts", currentLanguage),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = Ink,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (savedPosts.isEmpty()) {
                item {
                    EmptyNotice(text = MeskotStrings.get("noSaved", currentLanguage))
                }
            } else {
                items(savedPosts) { post ->
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
}
