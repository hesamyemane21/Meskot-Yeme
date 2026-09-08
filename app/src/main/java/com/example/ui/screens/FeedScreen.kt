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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.data.Post
import com.example.data.User
import com.example.ui.MeskotViewModel
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.CardBg
import com.example.ui.theme.Gold
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper2

@Composable
fun FeedScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    feedPosts: List<Post>,
    friends: List<User>,
    currentLanguage: AppLanguage
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("feed_screen")
    ) {
        // Top Story / Online Friends Strip
        if (friends.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(friends) { friend ->
                            StoryAvatarItem(
                                user = friend,
                                onClick = { viewModel.openChat(friend) }
                            )
                        }
                    }
                    HorizontalDivider(
                        color = LineBorder,
                        modifier = Modifier.padding(top = 10.dp, start = 14.dp, end = 14.dp)
                    )
                }
            }
        }

        // Composer trigger pill
        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable { viewModel.openComposer() }
                        .testTag("composer_trigger_card"),
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
                        UserAvatar(
                            photoUrl = currentUser.photoUrl,
                            name = currentUser.displayName,
                            size = 40
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Paper2)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = MeskotStrings.get("composerPh", currentLanguage),
                                color = MutedText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Posts List
        if (feedPosts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🪟", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = MeskotStrings.get("emptyFeed", currentLanguage),
                        color = MutedText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            items(feedPosts, key = { it.id }) { post ->
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
fun StoryAvatarItem(
    user: User,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(56.dp)
        ) {
            UserAvatar(
                photoUrl = user.photoUrl,
                name = user.displayName,
                size = 56
            )
            // Green online badge
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(ActiveGreen)
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.displayName.split(" ").firstOrNull() ?: user.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
