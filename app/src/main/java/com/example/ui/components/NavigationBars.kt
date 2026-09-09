package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.data.User
import com.example.ui.ScreenTab
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper
import com.example.ui.theme.Paper2

@Composable
fun TopNavBar(
    currentUser: User?,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onOpenComposer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMenu: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.96f),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Brand Logo & Title + Language Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Brand Logo & Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onProfileClick() }
                        .padding(end = 2.dp)
                ) {
                    // Arched Meskot brand mark
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(Brush.linearGradient(listOf(Gold, GoldDeep)))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Window lattice crossbars
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(22.dp)
                                .background(Color.White.copy(alpha = 0.75f))
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(1.5.dp)
                                .background(Color.White.copy(alpha = 0.75f))
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "Meskot",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Ink,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "· መስኮትህ ራስህ",
                            fontSize = 9.5.sp,
                            color = MutedText,
                            lineHeight = 11.sp
                        )
                    }
                }

                // Language toggle [ EN | አማ ] next to the brand
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Paper2)
                        .border(1.dp, LineBorder, RoundedCornerShape(18.dp))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentLanguage == AppLanguage.EN) Ink else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = "EN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentLanguage == AppLanguage.EN) Color.White else MutedText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentLanguage == AppLanguage.AM) Ink else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = "አማ",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentLanguage == AppLanguage.AM) Color.White else MutedText
                        )
                    }
                }
            }

            // Right Action Controls (circled in red): [+] [🔍] [☰] [Log out]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // [+] Create Post button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .clickable { onOpenComposer() }
                        .testTag("create_post_nav_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Post",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // [🔍] Search button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .clickable { onOpenSearch() }
                        .testTag("search_nav_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // [☰] Menu button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .clickable { onOpenMenu() }
                        .testTag("menu_nav_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // [Log out] button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .widthIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .clickable { onLogout() }
                        .padding(horizontal = 6.dp)
                        .testTag("logout_nav_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.AM) "ውጣ" else "Log\nout",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E),
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IconNavBar(
    currentTab: ScreenTab,
    unreadReqCount: Int,
    unreadMsgCount: Int,
    unreadNotifCount: Int,
    isAdmin: Boolean,
    onTabSelected: (ScreenTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Paper.copy(alpha = 0.98f),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIconButton(
                emoji = "🏠",
                label = "Feed",
                isSelected = currentTab == ScreenTab.FEED,
                onClick = { onTabSelected(ScreenTab.FEED) },
                testTag = "tab_feed"
            )

            NavIconButton(
                emoji = "👥",
                label = "Friends",
                badgeCount = unreadReqCount,
                isSelected = currentTab == ScreenTab.FRIENDS,
                onClick = { onTabSelected(ScreenTab.FRIENDS) },
                testTag = "tab_friends"
            )

            NavIconButton(
                emoji = "💬",
                label = "Messages",
                badgeCount = unreadMsgCount,
                isSelected = currentTab == ScreenTab.MESSAGES || currentTab == ScreenTab.CHAT,
                onClick = { onTabSelected(ScreenTab.MESSAGES) },
                testTag = "tab_messages"
            )

            NavIconButton(
                emoji = "👪",
                label = "Groups",
                isSelected = currentTab == ScreenTab.GROUPS || currentTab == ScreenTab.GROUP_DETAIL,
                onClick = { onTabSelected(ScreenTab.GROUPS) },
                testTag = "tab_groups"
            )

            NavIconButton(
                emoji = "🖼️",
                label = "Photos",
                isSelected = currentTab == ScreenTab.PHOTOS || currentTab == ScreenTab.ALBUM_DETAIL,
                onClick = { onTabSelected(ScreenTab.PHOTOS) },
                testTag = "tab_photos"
            )

            NavIconButton(
                emoji = "🔔",
                label = "Notifications",
                badgeCount = unreadNotifCount,
                isSelected = currentTab == ScreenTab.NOTIFICATIONS,
                onClick = { onTabSelected(ScreenTab.NOTIFICATIONS) },
                testTag = "tab_notifications"
            )

            NavIconButton(
                emoji = "📊",
                label = "Dashboard",
                isSelected = currentTab == ScreenTab.DASHBOARD,
                onClick = { onTabSelected(ScreenTab.DASHBOARD) },
                testTag = "tab_dashboard"
            )

            if (isAdmin) {
                NavIconButton(
                    emoji = "🛡️",
                    label = "Admin",
                    isSelected = currentTab == ScreenTab.ADMIN,
                    onClick = { onTabSelected(ScreenTab.ADMIN) },
                    testTag = "tab_admin"
                )
            }
        }
    }
}

@Composable
private fun NavIconButton(
    emoji: String,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = CrossRed,
                        contentColor = Color.White
                    ) {
                        Text(text = badgeCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Active bottom bar indicator
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) Gold else Color.Transparent)
        )
    }
}

@Composable
fun UserAvatar(
    photoUrl: String?,
    name: String,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(GoldDeep, Gold)))
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initials = name.trim().split("\\s+".toRegex()).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
            Text(
                text = if (initials.isNotBlank()) initials else "?",
                color = Color.White,
                fontSize = (size * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
    }
}
