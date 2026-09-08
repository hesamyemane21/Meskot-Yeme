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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.GroupItem
import com.example.data.MeskotStrings
import com.example.data.Post
import com.example.data.User
import com.example.ui.MeskotViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CardBg
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper2

enum class DashboardTab {
    HOME,
    INSIGHTS,
    CONTENT,
    ENGAGEMENT
}

@Composable
fun DashboardScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    myPosts: List<Post>,
    friendsCount: Int,
    currentLanguage: AppLanguage
) {
    var activeTab by remember { mutableStateOf(DashboardTab.HOME) }

    val totalLikes = myPosts.sumOf { it.reactions.size }
    val totalComments = myPosts.sumOf { it.commentCount }
    val totalTips = myPosts.sumOf { it.tipTotal }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp)) {
            Text(
                text = "Professional Dashboard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Ink
            )
            Text(
                text = "Track your engagement, audience growth, and community tips",
                fontSize = 12.sp,
                color = MutedText
            )
        }

        // Subtabs
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Ink else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MutedText
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            when (activeTab) {
                DashboardTab.HOME -> {
                    item {
                        // 4 Big Stat Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Posts",
                                value = myPosts.size.toString(),
                                emoji = "📝",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Friends",
                                value = friendsCount.toString(),
                                emoji = "👥",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Total Likes",
                                value = totalLikes.toString(),
                                emoji = "❤️",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Comments",
                                value = totalComments.toString(),
                                emoji = "💬",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Creator Support ETB Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Paper2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gold)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Creator Earnings via Chapa", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                    Text(text = "${totalTips.toInt()} ETB", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GoldDeep)
                                }
                                Text(text = "💰", fontSize = 32.sp)
                            }
                        }
                    }
                }

                DashboardTab.INSIGHTS -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Last 28 Days Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                                Spacer(modifier = Modifier.height(8.dp))
                                InsightRow(label = "Post Reach", value = "+128% vs previous month", isPositive = true)
                                InsightRow(label = "Profile Views", value = "+84 views this week", isPositive = true)
                                InsightRow(label = "Interaction Rate", value = "9.4% (Above average)", isPositive = true)
                                InsightRow(label = "Top Language", value = "Amharic (64%), English (36%)", isPositive = true)
                            }
                        }
                    }
                }

                DashboardTab.CONTENT -> {
                    if (myPosts.isEmpty()) {
                        item {
                            EmptyNotice(text = "You haven't posted yet.")
                        }
                    } else {
                        items(myPosts.sortedByDescending { it.reactions.size + it.commentCount }) { post ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = post.text.ifBlank { "Photo Post" },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Ink
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(text = "❤️ ${post.reactions.size} reactions", fontSize = 11.sp, color = MutedText)
                                        Text(text = "💬 ${post.commentCount} comments", fontSize = 11.sp, color = MutedText)
                                        if (post.tipTotal > 0) {
                                            Text(text = "💰 ${post.tipTotal.toInt()} ETB", fontSize = 11.sp, color = GoldDeep, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardTab.ENGAGEMENT -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Audience Demographics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Top Cities:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(text = "1. Addis Ababa (52%)\n2. Washington D.C. (18%)\n3. Hawassa (12%)\n4. Toronto (8%)", fontSize = 12.sp, color = MutedText, lineHeight = 20.sp)
                            }
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
fun StatCard(
    title: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = MutedText, fontWeight = FontWeight.Medium)
                Text(text = emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
fun InsightRow(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MutedText)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) GoldDeep else CrossRed
        )
    }
}

enum class AdminTab {
    OVERVIEW,
    USERS,
    POSTS,
    GROUPS
}

@Composable
fun AdminScreen(
    viewModel: MeskotViewModel,
    users: List<User>,
    posts: List<Post>,
    groups: List<GroupItem>,
    currentLanguage: AppLanguage
) {
    var adminTab by remember { mutableStateOf(AdminTab.OVERVIEW) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_screen")
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp)) {
            Text(
                text = "🛡️ " + MeskotStrings.get("adminPanel", currentLanguage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Ink
            )
            Text(
                text = MeskotStrings.get("systemManagement", currentLanguage),
                fontSize = 12.sp,
                color = MutedText
            )
        }

        // Subtabs
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                AdminTab.values().forEach { tab ->
                    val isSelected = adminTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Ink else Color.Transparent)
                            .clickable { adminTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MutedText
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            when (adminTab) {
                AdminTab.OVERVIEW -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(title = "Total Users", value = users.size.toString(), emoji = "👥", modifier = Modifier.weight(1f))
                            StatCard(title = "Active Posts", value = posts.size.toString(), emoji = "📝", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(title = "Total Groups", value = groups.size.toString(), emoji = "👪", modifier = Modifier.weight(1f))
                            val totalChapaTips = posts.sumOf { it.tipTotal }
                            StatCard(title = "Chapa Tips", value = "${totalChapaTips.toInt()} ETB", emoji = "💰", modifier = Modifier.weight(1f))
                        }
                    }
                }

                AdminTab.USERS -> {
                    items(users) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(photoUrl = user.photoUrl, name = user.displayName, size = 44)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = user.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                                        if (user.isAdmin) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "🛡️ Admin", fontSize = 10.sp, color = GoldDeep, fontWeight = FontWeight.Bold)
                                        }
                                        if (user.isSuspended) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "🚫 Suspended", fontSize = 10.sp, color = CrossRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(text = user.email, fontSize = 11.sp, color = MutedText)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.adminToggleSuspend(user.uid) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = if (user.isSuspended) "Restore" else "Suspend",
                                            fontSize = 10.sp,
                                            color = if (user.isSuspended) GoldDeep else CrossRed
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.adminToggleAdmin(user.uid) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = if (user.isAdmin) "Revoke" else "Make Admin",
                                            fontSize = 10.sp,
                                            color = Ink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.POSTS -> {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = post.authorName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text(
                                        text = post.text.ifBlank { "[Media attachment]" },
                                        fontSize = 12.sp,
                                        color = MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { viewModel.deletePost(post.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrossRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = "Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AdminTab.GROUPS -> {
                    items(groups) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = group.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text(text = "${group.memberCount} members", fontSize = 12.sp, color = MutedText)
                                }

                                Button(
                                    onClick = { viewModel.adminDeleteGroup(group.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrossRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = "Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
