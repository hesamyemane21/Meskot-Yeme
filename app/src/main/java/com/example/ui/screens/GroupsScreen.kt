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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AppLanguage
import com.example.data.GroupItem
import com.example.data.MeskotStrings
import com.example.data.User
import com.example.ui.MeskotViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.PostCard
import com.example.ui.theme.CardBg
import com.example.ui.theme.Gold
import com.example.ui.theme.Ink
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MutedText
import com.example.ui.theme.Paper
import com.example.ui.theme.Paper2

enum class GroupsSubTab {
    DISCOVER,
    MY_GROUPS
}

@Composable
fun GroupsScreen(
    viewModel: MeskotViewModel,
    groups: List<GroupItem>,
    currentLanguage: AppLanguage
) {
    var subTab by remember { mutableStateOf(GroupsSubTab.DISCOVER) }
    var searchQuery by remember { mutableStateOf("") }
    var isCreateGroupDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("groups_screen")
    ) {
        // Tab switcher
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            color = CardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                TabButton(
                    title = MeskotStrings.get("tabDiscover", currentLanguage),
                    isSelected = subTab == GroupsSubTab.DISCOVER,
                    onClick = { subTab = GroupsSubTab.DISCOVER },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    title = MeskotStrings.get("tabMyGroups", currentLanguage),
                    isSelected = subTab == GroupsSubTab.MY_GROUPS,
                    onClick = { subTab = GroupsSubTab.MY_GROUPS },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search & Create Group Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedText) },
                placeholder = { Text(MeskotStrings.get("searchGroups", currentLanguage), fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { isCreateGroupDialogOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text(text = MeskotStrings.get("create", currentLanguage), fontWeight = FontWeight.Bold)
            }
        }

        // Groups List
        val filtered = groups.filter {
            (subTab == GroupsSubTab.DISCOVER || it.isJoined) &&
                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyNotice(text = MeskotStrings.get("noGroups", currentLanguage))
                }
            } else {
                items(filtered) { group ->
                    GroupRowItem(
                        group = group,
                        currentLanguage = currentLanguage,
                        onClick = { viewModel.openGroupDetail(group) },
                        onToggleJoin = { viewModel.toggleGroupJoin(group) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (isCreateGroupDialogOpen) {
        CreateGroupDialog(
            currentLanguage = currentLanguage,
            onDismiss = { isCreateGroupDialogOpen = false },
            onCreate = { name, desc ->
                viewModel.createGroup(name, desc)
                isCreateGroupDialogOpen = false
            }
        )
    }
}

@Composable
fun GroupRowItem(
    group: GroupItem,
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
    onToggleJoin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
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
            // Group Cover Thumbnail with Initial
            val bgCol = try { Color(android.graphics.Color.parseColor(group.coverColorHex)) } catch (e: Exception) { Gold }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgCol),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.memberCount} ${MeskotStrings.get("members", currentLanguage)} · ${group.description}",
                    fontSize = 12.sp,
                    color = MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (group.isJoined) {
                OutlinedButton(
                    onClick = onToggleJoin,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(text = MeskotStrings.get("member", currentLanguage), fontSize = 11.sp, color = Ink)
                }
            } else {
                Button(
                    onClick = onToggleJoin,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(text = MeskotStrings.get("join", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GroupDetailScreen(
    viewModel: MeskotViewModel,
    group: GroupItem,
    currentUser: User?,
    currentLanguage: AppLanguage
) {
    val groupPosts = viewModel.getGroupPosts(group.id)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("group_detail_screen")
    ) {
        // Back Link
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ScreenTab.GROUPS) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Text(
                    text = MeskotStrings.get("backToGroups", currentLanguage),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedText
                )
            }
        }

        // Group Header Banner
        item {
            val bgCol = try { Color(android.graphics.Color.parseColor(group.coverColorHex)) } catch (e: Exception) { Gold }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .background(bgCol)
                    )

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = group.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Ink,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = group.description,
                            fontSize = 13.sp,
                            color = MutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${group.memberCount} ${MeskotStrings.get("members", currentLanguage)}",
                            fontSize = 12.sp,
                            color = MutedText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.toggleGroupJoin(group) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (group.isJoined) Paper2 else Gold,
                                contentColor = if (group.isJoined) Ink else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (group.isJoined) MeskotStrings.get("leave", currentLanguage) else MeskotStrings.get("join", currentLanguage),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Group Post Composer (if member)
        item {
            if (group.isJoined) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable { viewModel.openComposer(group.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✏️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MeskotStrings.get("whatsHappening", currentLanguage),
                            color = MutedText,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Paper2.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = MeskotStrings.get("joinToPost", currentLanguage), color = MutedText, fontSize = 13.sp)
                    }
                }
            }
        }

        // Group Posts
        if (groupPosts.isEmpty()) {
            item {
                EmptyNotice(text = MeskotStrings.get("noGroupPosts", currentLanguage))
            }
        } else {
            items(groupPosts) { post ->
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
fun CreateGroupDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = MeskotStrings.get("createGroup", currentLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(MeskotStrings.get("groupName", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(MeskotStrings.get("groupDesc", currentLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
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
                        onClick = { onCreate(name, desc) },
                        enabled = name.isNotBlank(),
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
