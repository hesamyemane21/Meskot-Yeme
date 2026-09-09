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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.ChatMessage
import com.example.data.MeskotStrings
import com.example.data.User
import com.example.ui.MeskotViewModel
import com.example.ui.ScreenTab
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
fun MessagesScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    allUsers: List<User>,
    friendUids: Set<String>,
    currentLanguage: AppLanguage
) {
    val conversations by viewModel.conversations.collectAsState()
    val friendsList = allUsers.filter { friendUids.contains(it.uid) }

    // Combine all sources of chat partner UIDs: direct keys, partner uids from all messages, and friend lists
    val chatPartnersFromMessages = remember(conversations, currentUser?.uid) {
        conversations.values.flatten()
            .mapNotNull { msg ->
                when {
                    msg.fromUid == currentUser?.uid && msg.toUid.isNotBlank() -> msg.toUid
                    msg.toUid == currentUser?.uid && msg.fromUid.isNotBlank() -> msg.fromUid
                    else -> null
                }
            }
    }
    val activeChatUids = remember(friendUids, conversations, chatPartnersFromMessages, currentUser?.uid) {
        val nonConvoKeys = conversations.filter { it.value.isNotEmpty() }.keys.filter { !it.contains("_") }
        (friendUids + nonConvoKeys + chatPartnersFromMessages)
            .filter { it.isNotBlank() && it != currentUser?.uid }
            .distinct()
    }
    val activeChatUsers = remember(activeChatUids, allUsers, conversations) {
        activeChatUids.map { uid ->
            allUsers.find { it.uid == uid } ?: User(uid = uid, displayName = "Meskot User", bio = "")
        }.sortedByDescending { user ->
            viewModel.getMessagesForUser(user.uid).lastOrNull()?.createdAt ?: 0L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("messages_screen")
    ) {
        Text(
            text = MeskotStrings.get("navMessages", currentLanguage),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = Ink,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
        )

        // Friends Instant Chat Story Strip
        if (friendsList.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(friendsList) { friend ->
                    StoryAvatarItem(
                        user = friend,
                        onClick = { viewModel.openChat(friend) }
                    )
                }
            }
            HorizontalDivider(color = LineBorder, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }

        // Conversations List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            if (activeChatUsers.isEmpty()) {
                item {
                    EmptyNotice(text = MeskotStrings.get("noConvos", currentLanguage))
                }
            } else {
                items(activeChatUsers) { user ->
                    val messages = remember(conversations, user.uid, currentUser?.uid) {
                        viewModel.getMessagesForUser(user.uid)
                    }
                    val lastMessage = messages.lastOrNull()
                    val isLastFromPartner = lastMessage != null && lastMessage.fromUid == user.uid

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.openChat(user) },
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
                            UserAvatar(photoUrl = user.photoUrl, name = user.displayName, size = 48)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = lastMessage?.text ?: "Say hello 👋",
                                    fontSize = 13.sp,
                                    color = if (isLastFromPartner) Ink else MutedText,
                                    fontWeight = if (isLastFromPartner) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (lastMessage != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MeskotStrings.timeAgo(lastMessage.createdAt, currentLanguage),
                                        fontSize = 11.sp,
                                        color = MutedText
                                    )
                                    if (isLastFromPartner) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(com.example.ui.theme.GoldDeep)
                                        )
                                    }
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

@Composable
fun ChatScreen(
    viewModel: MeskotViewModel,
    currentUser: User?,
    recipient: User,
    currentLanguage: AppLanguage
) {
    val conversations by viewModel.conversations.collectAsState()
    val messages = remember(conversations, recipient.uid, currentUser?.uid) {
        viewModel.getMessagesForUser(recipient.uid)
    }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(recipient.uid) {
        viewModel.markConversationAsRead(recipient.uid)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        viewModel.markConversationAsRead(recipient.uid)
    }

    val callPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handled gracefully */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .testTag("chat_screen")
    ) {
        // Chat Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(ScreenTab.MESSAGES) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
                    }

                    UserAvatar(photoUrl = recipient.photoUrl, name = recipient.displayName, size = 38)

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(text = recipient.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(text = "Active now", fontSize = 11.sp, color = MutedText)
                    }
                }

                // Call Action Buttons (Audio Call 📞, Video Call 📹)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            callPermissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                            viewModel.startCall(recipient, "audio")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Paper2)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Audio call", tint = Ink, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            callPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECORD_AUDIO,
                                    android.Manifest.permission.CAMERA
                                )
                            )
                            viewModel.startCall(recipient, "video")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Paper2)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video call", tint = Ink, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Messages Bubble List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Say hello to ${recipient.displayName}! 👋", color = MutedText, fontSize = 14.sp)
                    }
                }
            } else {
                items(messages) { msg ->
                    val isMine = msg.fromUid == currentUser?.uid
                    if (msg.isCallLog) {
                        CallLogBubble(
                            msg = msg,
                            currentLanguage = currentLanguage,
                            onCallBack = {
                                if (msg.callType == "video") {
                                    callPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.RECORD_AUDIO,
                                            android.Manifest.permission.CAMERA
                                        )
                                    )
                                } else {
                                    callPermissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                                }
                                viewModel.startCall(recipient, msg.callType)
                            }
                        )
                    } else {
                        ChatMessageBubble(
                            msg = msg,
                            isMine = isMine,
                            onDelete = { viewModel.deleteMessage(recipient.uid, msg.id) }
                        )
                    }
                }
            }
        }

        // Chat Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text(MeskotStrings.get("typeMessage", currentLanguage), fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(recipient.uid, textInput)
                            textInput = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(recipient.uid, textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Ink)
                        .testTag("chat_send_btn")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    msg: ChatMessage,
    isMine: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) Ink else Color.White,
            border = if (isMine) null else androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = msg.text,
                    color = if (isMine) Color.White else Ink,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = MeskotStrings.timeAgo(msg.createdAt, AppLanguage.EN),
                        color = if (isMine) Color.White.copy(alpha = 0.6f) else MutedText,
                        fontSize = 10.sp
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogBubble(
    msg: ChatMessage,
    currentLanguage: AppLanguage,
    onCallBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Paper2,
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
            modifier = Modifier
                .clickable { onCallBack() }
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (msg.callType == "video") "📹" else "📞", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    val label = if (msg.callStatus == "missed") {
                        if (msg.callType == "video") "Missed video call" else "Missed audio call"
                    } else {
                        val m = msg.callDurationSec / 60
                        val s = msg.callDurationSec % 60
                        "${if (msg.callType == "video") "Video call" else "Audio call"} (${String.format("%02d:%02d", m, s)})"
                    }
                    Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Ink)
                    Text(text = "Tap to call back", fontSize = 10.sp, color = GoldDeep)
                }
            }
        }
    }
}
