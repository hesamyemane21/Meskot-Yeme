package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppLanguage
import com.example.data.Comment
import com.example.data.MeskotStrings
import com.example.data.Post
import com.example.data.ReactionType
import com.example.data.SharedPostPreview
import com.example.ui.theme.CardBg
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper2
import com.example.ui.theme.PostGradientList

@Composable
fun PostCard(
    post: Post,
    currentUserId: String?,
    comments: List<Comment>,
    currentLanguage: AppLanguage,
    onAuthorClick: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onShare: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onTipClick: (Post) -> Unit,
    onOpenMenu: (Post) -> Unit,
    onAddComment: (String, String, String?) -> Unit,
    onToggleCommentLike: (String, String) -> Unit,
    onDeleteComment: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCommentsOpen by remember { mutableStateOf(false) }
    var isReactionPickerOpen by remember { mutableStateOf(false) }
    var isExpandedText by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }

    val myReaction = currentUserId?.let { post.reactions[it] }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Meskot decorative arch accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(Gold, CrossRed, GoldDeep)))
            )

            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onAuthorClick(post.uid) }
                        .weight(1f)
                ) {
                    UserAvatar(photoUrl = post.authorPhoto, name = post.authorName, size = 44)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            text = MeskotStrings.timeAgo(post.createdAt, currentLanguage) + if (post.editedAt != null) " · edited" else "",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }

                IconButton(
                    onClick = { onOpenMenu(post) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MutedText
                    )
                }
            }

            // Shared Post tag if shared
            if (post.sharedPost != null) {
                Text(
                    text = "🔁 " + MeskotStrings.get("sharedAPost", currentLanguage),
                    fontSize = 12.sp,
                    color = MutedText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // Post Body Text
            if (post.text.isNotBlank()) {
                val bgBrush = PostGradientList.getOrNull(post.bgColorIndex)
                if (bgBrush != null) {
                    // Colored gradient card style
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgBrush)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.text,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 28.sp
                        )
                    }
                } else {
                    // Standard text with "See more" if long
                    val isLong = post.text.length > 180
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            text = post.text,
                            fontSize = 15.sp,
                            color = Ink,
                            lineHeight = 22.sp,
                            maxLines = if (isExpandedText || !isLong) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isLong) {
                            Text(
                                text = if (isExpandedText) MeskotStrings.get("seeLess", currentLanguage) else MeskotStrings.get("seeMore", currentLanguage),
                                color = GoldDeep,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { isExpandedText = !isExpandedText }
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Media Images Grid
            if (post.mediaUrls.isNotEmpty()) {
                MediaGrid(urls = post.mediaUrls)
            }

            // Embedded Shared Post Preview
            if (post.sharedPost != null) {
                SharedPostBox(
                    shared = post.sharedPost,
                    currentLanguage = currentLanguage,
                    onAuthorClick = onAuthorClick
                )
            }

            // Reaction & Tip Counts Summary Line
            val totalReactions = post.reactions.size
            if (totalReactions > 0 || post.tipTotal > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (totalReactions > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Top reaction emojis
                            val topReactions = post.reactions.values.groupingBy { it }.eachCount()
                                .entries.sortedByDescending { it.value }.take(3).map { it.key }
                            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                                topReactions.forEach { code ->
                                    val emoji = ReactionType.values().find { it.code == code }?.emoji ?: "👍"
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.dp, LineBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$totalReactions " + MeskotStrings.get("likesLabel", currentLanguage),
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (post.tipTotal > 0) {
                        Text(
                            text = "💰 ${post.tipTotal.toInt()} ${MeskotStrings.get("etbReceived", currentLanguage)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldDeep
                        )
                    }
                }
            }

            HorizontalDivider(color = LineBorder, thickness = 0.8.dp)

            // Reaction Picker Popover
            AnimatedVisibility(visible = isReactionPickerOpen) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReactionType.values().forEach { r ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onToggleReaction(post.id, r.code)
                                        isReactionPickerOpen = false
                                    }
                                    .padding(6.dp)
                            ) {
                                Text(text = r.emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            // Post Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like / React Button
                val reactionObj = ReactionType.values().find { it.code == myReaction }
                TextButton(
                    onClick = {
                        if (myReaction != null) {
                            onToggleReaction(post.id, myReaction)
                        } else {
                            onToggleReaction(post.id, "like")
                        }
                    },
                    modifier = Modifier.testTag("btn_react_${post.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isReactionPickerOpen = !isReactionPickerOpen }
                    ) {
                        Text(
                            text = reactionObj?.emoji ?: "👍",
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (reactionObj != null) MeskotStrings.get(reactionObj.labelKey, currentLanguage) else MeskotStrings.get("like", currentLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reactionObj != null) (if (reactionObj == ReactionType.LOVE) CrossRed else GoldDeep) else MutedText
                        )
                    }
                }

                // Comment Button
                TextButton(
                    onClick = { isCommentsOpen = !isCommentsOpen },
                    modifier = Modifier.testTag("btn_comment_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = MeskotStrings.get("comment", currentLanguage) + if (post.commentCount > 0) " (${post.commentCount})" else "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedText
                    )
                }

                // Share Button
                IconButton(onClick = { onShare(post.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Save Bookmark Button
                IconButton(onClick = { onToggleSave(post.id) }) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (post.isSaved) GoldDeep else MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tip Creator Button (Support with Chapa)
                if (post.uid != currentUserId) {
                    TextButton(onClick = { onTipClick(post) }) {
                        Text(text = "💰", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = MeskotStrings.get("support", currentLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldDeep
                        )
                    }
                }
            }

            // Expandable Comments Section
            AnimatedVisibility(visible = isCommentsOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Paper2.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // List of comments
                    if (comments.isEmpty()) {
                        Text(
                            text = "No comments yet. Be the first to reply!",
                            fontSize = 13.sp,
                            color = MutedText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val topLevel = comments.filter { it.parentId == null }
                        topLevel.forEach { c ->
                            CommentItemRow(
                                comment = c,
                                currentUserId = currentUserId,
                                currentLanguage = currentLanguage,
                                onAuthorClick = onAuthorClick,
                                onLike = { onToggleCommentLike(post.id, c.id) },
                                onReply = { replyingToCommentId = c.id },
                                onDelete = { onDeleteComment(post.id, c.id) }
                            )

                            // Nested replies
                            val replies = comments.filter { it.parentId == c.id }
                            replies.forEach { r ->
                                CommentItemRow(
                                    comment = r,
                                    currentUserId = currentUserId,
                                    currentLanguage = currentLanguage,
                                    isReply = true,
                                    onAuthorClick = onAuthorClick,
                                    onLike = { onToggleCommentLike(post.id, r.id) },
                                    onReply = { replyingToCommentId = c.id },
                                    onDelete = { onDeleteComment(post.id, r.id) }
                                )
                            }
                        }
                    }

                    // Comment Input Bar
                    if (replyingToCommentId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Replying to comment…",
                                fontSize = 11.sp,
                                color = GoldDeep
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✕",
                                fontSize = 11.sp,
                                color = CrossRed,
                                modifier = Modifier.clickable { replyingToCommentId = null }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text(MeskotStrings.get("writeComment", currentLanguage), fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comment_input_${post.id}"),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (commentInput.isNotBlank()) {
                                    onAddComment(post.id, commentInput, replyingToCommentId)
                                    commentInput = ""
                                    replyingToCommentId = null
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (commentInput.isNotBlank()) {
                                    onAddComment(post.id, commentInput, replyingToCommentId)
                                    commentInput = ""
                                    replyingToCommentId = null
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Ink)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItemRow(
    comment: Comment,
    currentUserId: String?,
    currentLanguage: AppLanguage,
    isReply: Boolean = false,
    onAuthorClick: (String) -> Unit,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 28.dp else 0.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            photoUrl = comment.authorPhoto,
            name = comment.authorName,
            size = if (isReply) 28 else 32,
            modifier = Modifier.clickable { onAuthorClick(comment.uid) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comment.authorName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            modifier = Modifier.clickable { onAuthorClick(comment.uid) }
                        )

                        if (comment.uid == currentUserId) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = CrossRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = comment.text,
                        fontSize = 13.sp,
                        color = Ink,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Comment Action Row (Like, Reply, Time)
            Row(
                modifier = Modifier.padding(start = 6.dp, top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isLiked = currentUserId?.let { comment.likes[it] } == true
                val likeCount = comment.likes.size

                Text(
                    text = MeskotStrings.get("like", currentLanguage) + if (likeCount > 0) " · $likeCount" else "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiked) CrossRed else MutedText,
                    modifier = Modifier.clickable { onLike() }
                )

                if (!isReply) {
                    Text(
                        text = MeskotStrings.get("reply", currentLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        modifier = Modifier.clickable { onReply() }
                    )
                }

                Text(
                    text = MeskotStrings.timeAgo(comment.createdAt, currentLanguage),
                    fontSize = 11.sp,
                    color = MutedText
                )
            }
        }
    }
}

@Composable
fun MediaGrid(urls: List<String>) {
    if (urls.size == 1) {
        AsyncImage(
            model = urls.first(),
            contentDescription = "Media",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            urls.take(2).forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Media",
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun SharedPostBox(
    shared: SharedPostPreview,
    currentLanguage: AppLanguage,
    onAuthorClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = Paper2.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(photoUrl = shared.authorPhoto, name = shared.authorName, size = 32)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = shared.authorName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(text = MeskotStrings.timeAgo(shared.createdAt, currentLanguage), fontSize = 11.sp, color = MutedText)
                }
            }

            if (shared.text.isNotBlank()) {
                Text(
                    text = shared.text,
                    fontSize = 13.sp,
                    color = Ink,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (shared.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = shared.mediaUrls.first(),
                    contentDescription = "Shared Media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
