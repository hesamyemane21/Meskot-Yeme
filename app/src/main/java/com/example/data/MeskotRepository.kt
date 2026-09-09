package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MeskotRepository(private val context: Context) {

    // Language state
    private val _currentLanguage = MutableStateFlow(AppLanguage.EN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
    }

    // Current logged-in user (null until logged in via Firebase)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Users list (populated directly from Firebase Firestore)
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Posts list (populated directly from Firebase Firestore)
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // Comments map: postId -> List<Comment>
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    // Friendships: Set of friend uids
    private val _friends = MutableStateFlow<Set<String>>(emptySet())
    val friends: StateFlow<Set<String>> = _friends.asStateFlow()

    // Pending incoming friend requests
    private val _incomingRequests = MutableStateFlow<List<User>>(emptyList())
    val incomingRequests: StateFlow<List<User>> = _incomingRequests.asStateFlow()

    // Pending outgoing friend requests
    private val _outgoingRequests = MutableStateFlow<Set<String>>(emptySet())
    val outgoingRequests: StateFlow<Set<String>> = _outgoingRequests.asStateFlow()

    // Groups
    private val _groups = MutableStateFlow<List<GroupItem>>(createInitialGroups())
    val groups: StateFlow<List<GroupItem>> = _groups.asStateFlow()

    // Group posts map: groupId -> List<Post>
    private val _groupPosts = MutableStateFlow<Map<String, List<Post>>>(emptyMap())
    val groupPosts: StateFlow<Map<String, List<Post>>> = _groupPosts.asStateFlow()

    // Albums
    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albums: StateFlow<List<AlbumItem>> = _albums.asStateFlow()

    // Conversations map: otherUid -> List<ChatMessage>
    private val _conversations = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val conversations: StateFlow<Map<String, List<ChatMessage>>> = _conversations.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Saved post IDs
    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()

    // Hidden post IDs
    private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPostIds: StateFlow<Set<String>> = _hiddenPostIds.asStateFlow()

    private var notifListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var incomingCallsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var activeCallListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var userMessageRegistrations: List<com.google.firebase.firestore.ListenerRegistration> = emptyList()
    private var generalMessagesRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // Incoming call
    private val _incomingCall = MutableStateFlow<CallSession?>(null)
    val incomingCall: StateFlow<CallSession?> = _incomingCall.asStateFlow()

    // Real-time Incoming Message Alert (for heads-up banner / instant toast)
    private val _incomingMessageAlert = MutableStateFlow<ChatMessage?>(null)
    val incomingMessageAlert: StateFlow<ChatMessage?> = _incomingMessageAlert.asStateFlow()

    fun clearIncomingMessageAlert() {
        _incomingMessageAlert.value = null
    }

    // Unread tracking per sender
    private val _lastReadTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())

    fun markConversationAsRead(otherUid: String) {
        _lastReadTimestamps.value = _lastReadTimestamps.value + (otherUid to System.currentTimeMillis())
    }

    // Dynamic unread messages count
    val unreadMsgCount: StateFlow<Int> = kotlinx.coroutines.flow.combine(_conversations, _lastReadTimestamps, _currentUser) { convos, readMap, user ->
        if (user == null) return@combine 0
        val partnersWithUnread = convos.filterKeys { !it.contains("_") && it != user.uid }.count { (partnerUid, messages) ->
            val lastRead = readMap[partnerUid] ?: 0L
            messages.any { it.toUid == user.uid && it.fromUid == partnerUid && it.createdAt > lastRead && !it.isCallLog }
        }
        partnersWithUnread
    }.stateIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default), kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    init {
        FirebaseManager.initialize(context)
        val fbAuthUser = FirebaseManager.getCurrentFirebaseUser()
        if (fbAuthUser != null) {
            val user = User(
                uid = fbAuthUser.uid,
                displayName = fbAuthUser.displayName ?: fbAuthUser.email?.substringBefore("@") ?: "User",
                email = fbAuthUser.email ?: "",
                photoUrl = fbAuthUser.photoUrl?.toString() ?: ""
            )
            _currentUser.value = user
            setupUserSpecificListeners(user.uid)
        }
        setupFirebaseListeners()
    }

    private fun setupUserSpecificListeners(uid: String) {
        notifListenerRegistration?.remove()
        notifListenerRegistration = FirebaseManager.listenToNotifications(uid) { liveNotifs ->
            val liveIds = liveNotifs.map { it.id }.toSet()
            val remainingLocal = _notifications.value.filterNot { it.id in liveIds }
            _notifications.value = (liveNotifs + remainingLocal).sortedByDescending { it.createdAt }
        }

        incomingCallsListenerRegistration?.remove()
        incomingCallsListenerRegistration = FirebaseManager.listenToIncomingCalls(uid) { session ->
            if (_incomingCall.value?.callId != session.callId && session.status == "ringing") {
                _incomingCall.value = session
            }
        }

        // Direct real-time message streams for this user
        userMessageRegistrations.forEach { it.remove() }
        userMessageRegistrations = FirebaseManager.listenToUserMessages(uid) { liveMsgs ->
            mergeMessagesIntoConversations(liveMsgs, notifyIncoming = true)
        }
    }

    private fun setupFirebaseListeners() {
        try {
            FirebaseManager.listenToPosts { livePosts ->
                _posts.value = livePosts
            }

            FirebaseManager.listenToComments { liveComments ->
                _comments.value = liveComments
            }

            FirebaseManager.listenToUsers { liveUsers ->
                _users.value = liveUsers
                val curUid = _currentUser.value?.uid ?: FirebaseManager.getCurrentFirebaseUser()?.uid
                if (curUid != null) {
                    val matching = liveUsers.find { it.uid == curUid }
                    if (matching != null) {
                        _currentUser.value = matching
                        setupUserSpecificListeners(matching.uid)
                    }
                }
            }

            // Global real-time Messages synchronization
            generalMessagesRegistration?.remove()
            generalMessagesRegistration = FirebaseManager.listenToMessages { liveMessages ->
                mergeMessagesIntoConversations(liveMessages, notifyIncoming = true)
            }

            // Real-time Friend Requests synchronization
            FirebaseManager.listenToFriendRequests { liveRequests ->
                val currentUid = _currentUser.value?.uid
                if (currentUid != null) {
                    // Incoming pending requests
                    val incoming = liveRequests.filter { it.toUid == currentUid && it.status == "pending" }
                    val incomingUserList = incoming.map { req ->
                        _users.value.find { it.uid == req.fromUid } ?: User(
                            uid = req.fromUid,
                            displayName = req.fromName,
                            photoUrl = req.fromPhoto,
                            bio = "Meskot member"
                        )
                    }
                    _incomingRequests.value = incomingUserList

                    // Outgoing pending requests
                    val outgoing = liveRequests.filter { it.fromUid == currentUid && it.status == "pending" }
                    _outgoingRequests.value = outgoing.map { it.toUid }.toSet()

                    // Newly accepted requests involving the user
                    val accepted = liveRequests.filter {
                        (it.fromUid == currentUid || it.toUid == currentUid) && it.status == "accepted"
                    }
                    val acceptedUids = accepted.map { if (it.fromUid == currentUid) it.toUid else it.fromUid }
                    if (acceptedUids.isNotEmpty()) {
                        _friends.value = _friends.value + acceptedUids
                    }
                }
            }

            // Real-time Friendships synchronization
            FirebaseManager.listenToFriendships { liveFriendships ->
                val currentUid = _currentUser.value?.uid
                if (currentUid != null) {
                    val friendsFromPairs = liveFriendships.mapNotNull { (u1, u2) ->
                        when (currentUid) {
                            u1 -> u2
                            u2 -> u1
                            else -> null
                        }
                    }
                    if (friendsFromPairs.isNotEmpty()) {
                        _friends.value = _friends.value + friendsFromPairs
                    }
                }
            }

            // Attach user-specific notifications listener if logged in
            _currentUser.value?.uid?.let { setupUserSpecificListeners(it) }

        } catch (e: Exception) {
            android.util.Log.e("MeskotRepository", "Could not setup Firebase listeners: ${e.message}")
        }
    }

    // AUTH METHODS
    fun login(email: String, pass: String, onResult: ((Boolean, String?) -> Unit)? = null): Boolean {
        FirebaseManager.signInWithEmail(
            email = email,
            pass = pass,
            onSuccess = { fbUser ->
                _currentUser.value = fbUser
                setupUserSpecificListeners(fbUser.uid)
                setupFirebaseListeners()
                if (_users.value.none { it.uid == fbUser.uid }) {
                    _users.value = _users.value + fbUser
                }
                onResult?.invoke(true, null)
            },
            onFailure = { err ->
                onResult?.invoke(false, err)
            }
        )
        return true
    }

    fun signup(fullName: String, email: String, pass: String, onResult: ((Boolean, String?) -> Unit)? = null): Boolean {
        FirebaseManager.signUpWithEmail(
            fullName = fullName,
            email = email,
            pass = pass,
            onSuccess = { fbUser ->
                _currentUser.value = fbUser
                setupUserSpecificListeners(fbUser.uid)
                setupFirebaseListeners()
                if (_users.value.none { it.uid == fbUser.uid }) {
                    _users.value = _users.value + fbUser
                }
                onResult?.invoke(true, null)
            },
            onFailure = { err ->
                onResult?.invoke(false, err)
            }
        )
        return true
    }

    fun switchUser(user: User) {
        _currentUser.value = user
        setupUserSpecificListeners(user.uid)
        setupFirebaseListeners()
    }

    fun logout() {
        notifListenerRegistration?.remove()
        notifListenerRegistration = null
        incomingCallsListenerRegistration?.remove()
        incomingCallsListenerRegistration = null
        activeCallListenerRegistration?.remove()
        activeCallListenerRegistration = null
        userMessageRegistrations.forEach { it.remove() }
        userMessageRegistrations = emptyList()
        generalMessagesRegistration?.remove()
        generalMessagesRegistration = null
        _incomingMessageAlert.value = null
        _incomingCall.value = null
        FirebaseManager.signOut()
        _currentUser.value = null
    }

    fun updateProfile(name: String, bio: String, photoUrl: String) {
        val curr = _currentUser.value ?: return
        val updated = curr.copy(
            displayName = name.ifBlank { curr.displayName },
            bio = bio,
            photoUrl = photoUrl.ifBlank { curr.photoUrl }
        )
        _currentUser.value = updated
        _users.value = _users.value.map { if (it.uid == curr.uid) updated else it }
        FirebaseManager.saveUser(updated)
    }

    // POSTS METHODS
    fun createPost(text: String, mediaUrls: List<String> = emptyList(), bgColorIndex: Int = 0, visibility: String = "public") {
        val user = _currentUser.value ?: return
        val newPost = Post(
            id = "post_" + System.currentTimeMillis(),
            uid = user.uid,
            authorName = user.displayName,
            authorPhoto = user.photoUrl,
            text = text,
            mediaUrls = mediaUrls,
            bgColorIndex = bgColorIndex,
            visibility = visibility,
            reactions = emptyMap(),
            commentCount = 0,
            createdAt = System.currentTimeMillis()
        )
        _posts.value = listOf(newPost) + _posts.value
        FirebaseManager.createPost(newPost)
    }

    fun editPost(postId: String, newText: String) {
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(text = newText, editedAt = System.currentTimeMillis()) else it
        }
        FirebaseManager.updatePostText(postId, newText)
    }

    fun deletePost(postId: String) {
        _posts.value = _posts.value.filterNot { it.id == postId }
        FirebaseManager.deletePost(postId)
    }

    fun toggleReaction(postId: String, reactionType: String) {
        val user = _currentUser.value ?: return
        var updatedReactionsMap: Map<String, String>? = null
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val currentReaction = post.reactions[user.uid]
                val updatedReactions = post.reactions.toMutableMap()
                if (currentReaction == reactionType) {
                    updatedReactions.remove(user.uid)
                } else {
                    updatedReactions[user.uid] = reactionType
                    if (post.uid != user.uid) {
                        addNotification(
                            fromUid = user.uid,
                            fromName = user.displayName,
                            fromPhoto = user.photoUrl,
                            type = "reaction",
                            targetId = postId,
                            reactionType = reactionType
                        )
                    }
                }
                updatedReactionsMap = updatedReactions
                post.copy(reactions = updatedReactions)
            } else post
        }
        updatedReactionsMap?.let { FirebaseManager.updatePostReactions(postId, it) }
    }

    fun sharePost(postId: String) {
        val user = _currentUser.value ?: return
        val sourcePost = _posts.value.find { it.id == postId } ?: return
        val newPost = Post(
            id = "post_" + System.currentTimeMillis(),
            uid = user.uid,
            authorName = user.displayName,
            authorPhoto = user.photoUrl,
            text = "",
            sharedPost = SharedPostPreview(
                postId = sourcePost.id,
                authorName = sourcePost.authorName,
                authorPhoto = sourcePost.authorPhoto,
                text = sourcePost.text,
                mediaUrls = sourcePost.mediaUrls,
                createdAt = sourcePost.createdAt
            ),
            createdAt = System.currentTimeMillis()
        )
        _posts.value = listOf(newPost) + _posts.value
        if (sourcePost.uid != user.uid) {
            addNotification(
                fromUid = user.uid,
                fromName = user.displayName,
                fromPhoto = user.photoUrl,
                type = "share",
                targetId = postId
            )
        }
    }

    fun toggleSavePost(postId: String) {
        val currentSaved = _savedPostIds.value.toMutableSet()
        if (currentSaved.contains(postId)) {
            currentSaved.remove(postId)
        } else {
            currentSaved.add(postId)
        }
        _savedPostIds.value = currentSaved
    }

    fun hidePost(postId: String) {
        _hiddenPostIds.value = _hiddenPostIds.value + postId
    }

    fun sendTip(postId: String, amount: Double) {
        val user = _currentUser.value ?: return
        var updatedTipTotal: Double? = null
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val newTotal = post.tipTotal + amount
                updatedTipTotal = newTotal
                post.copy(tipTotal = newTotal)
            } else post
        }
        updatedTipTotal?.let { FirebaseManager.updatePostTip(postId, it) }
        val post = _posts.value.find { it.id == postId } ?: return
        if (post.uid != user.uid) {
            addNotification(
                fromUid = user.uid,
                fromName = user.displayName,
                fromPhoto = user.photoUrl,
                type = "tip",
                targetId = postId,
                amount = amount
            )
        }
    }

    // COMMENTS
    fun getCommentsForPost(postId: String): List<Comment> {
        return _comments.value[postId] ?: emptyList()
    }

    fun addComment(postId: String, text: String, parentId: String? = null) {
        val user = _currentUser.value ?: return
        val newComment = Comment(
            id = "comment_" + System.currentTimeMillis(),
            postId = postId,
            uid = user.uid,
            authorName = user.displayName,
            authorPhoto = user.photoUrl,
            text = text,
            parentId = parentId,
            createdAt = System.currentTimeMillis()
        )
        val currentList = _comments.value[postId] ?: emptyList()
        _comments.value = _comments.value + (postId to (currentList + newComment))
        // Increment post comment count
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(commentCount = it.commentCount + 1) else it
        }
        FirebaseManager.addComment(newComment)
        val post = _posts.value.find { it.id == postId }
        if (post != null && post.uid != user.uid) {
            addNotification(
                fromUid = user.uid,
                fromName = user.displayName,
                fromPhoto = user.photoUrl,
                type = "comment",
                targetId = postId
            )
        }
    }

    fun toggleCommentLike(postId: String, commentId: String) {
        val user = _currentUser.value ?: return
        val commentsList = _comments.value[postId] ?: return
        _comments.value = _comments.value + (postId to commentsList.map { comment ->
            if (comment.id == commentId) {
                val updatedLikes = comment.likes.toMutableMap()
                if (updatedLikes[user.uid] == true) {
                    updatedLikes.remove(user.uid)
                } else {
                    updatedLikes[user.uid] = true
                }
                comment.copy(likes = updatedLikes)
            } else comment
        })
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        val commentsList = _comments.value[postId] ?: return
        _comments.value = _comments.value + (postId to commentsList.map {
            if (it.id == commentId) it.copy(text = newText, editedAt = System.currentTimeMillis()) else it
        })
    }

    fun deleteComment(postId: String, commentId: String) {
        val commentsList = _comments.value[postId] ?: return
        _comments.value = _comments.value + (postId to commentsList.filterNot { it.id == commentId })
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(commentCount = maxOf(0, it.commentCount - 1)) else it
        }
        FirebaseManager.deleteComment(postId, commentId)
    }

    // FRIENDS
    fun sendFriendRequest(toUid: String) {
        val user = _currentUser.value ?: return
        _outgoingRequests.value = _outgoingRequests.value + toUid

        val req = FriendRequest(
            id = "${user.uid}_$toUid",
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            toUid = toUid,
            status = "pending",
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.sendFriendRequest(req)

        val notif = NotificationItem(
            id = "notif_" + System.currentTimeMillis(),
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            toUid = toUid,
            type = "friend_request",
            text = "${user.displayName} sent you a friend request",
            targetId = user.uid,
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.sendNotification(notif)
    }

    fun cancelFriendRequest(toUid: String) {
        val user = _currentUser.value ?: return
        _outgoingRequests.value = _outgoingRequests.value - toUid
        val reqId = "${user.uid}_$toUid"
        FirebaseManager.deleteFriendRequest(reqId)
    }

    fun acceptFriendRequest(fromUid: String) {
        val user = _currentUser.value ?: return
        _friends.value = _friends.value + fromUid
        _incomingRequests.value = _incomingRequests.value.filterNot { it.uid == fromUid }

        val reqId = "${fromUid}_${user.uid}"
        FirebaseManager.updateFriendRequestStatus(reqId, "accepted")
        FirebaseManager.addFriendship(user.uid, fromUid)

        val notif = NotificationItem(
            id = "notif_" + System.currentTimeMillis(),
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            toUid = fromUid,
            type = "friend_accept",
            text = "${user.displayName} accepted your friend request",
            targetId = user.uid,
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.sendNotification(notif)
    }

    fun declineFriendRequest(fromUid: String) {
        val user = _currentUser.value ?: return
        _incomingRequests.value = _incomingRequests.value.filterNot { it.uid == fromUid }
        val reqId = "${fromUid}_${user.uid}"
        FirebaseManager.updateFriendRequestStatus(reqId, "declined")
    }

    fun unfriend(uid: String) {
        val user = _currentUser.value ?: return
        _friends.value = _friends.value - uid
        FirebaseManager.removeFriendship(user.uid, uid)
    }

    // MESSAGES & CHAT
    fun mergeMessagesIntoConversations(liveMessages: List<ChatMessage>, notifyIncoming: Boolean = true) {
        if (liveMessages.isEmpty()) return
        val currentUid = _currentUser.value?.uid
        val currentMap = _conversations.value.toMutableMap()
        var hasNewIncoming = false
        var latestIncomingMsg: ChatMessage? = null

        liveMessages.forEach { msg ->
            val convoId = if (msg.convoId.isNotBlank()) msg.convoId else {
                if (msg.fromUid < msg.toUid) "${msg.fromUid}_${msg.toUid}" else "${msg.toUid}_${msg.fromUid}"
            }

            // Check if this is a newly arrived message addressed to current user
            if (currentUid != null && msg.toUid == currentUid && msg.fromUid != currentUid && !msg.isCallLog) {
                val existingList = currentMap[msg.fromUid] ?: emptyList()
                if (existingList.none { it.id == msg.id }) {
                    hasNewIncoming = true
                    if (latestIncomingMsg == null || msg.createdAt > latestIncomingMsg!!.createdAt) {
                        latestIncomingMsg = msg
                    }
                }
            }

            fun updateList(key: String) {
                val list = currentMap[key] ?: emptyList()
                val idx = list.indexOfFirst { it.id == msg.id }
                val updated = if (idx >= 0) {
                    list.toMutableList().apply { set(idx, msg) }
                } else {
                    (list + msg).sortedBy { it.createdAt }
                }
                currentMap[key] = updated
            }

            // 1. Index by convoId
            if (convoId.isNotBlank()) {
                updateList(convoId)
            }
            // 2. Index by toUid
            if (msg.toUid.isNotBlank()) {
                updateList(msg.toUid)
            }
            // 3. Index by fromUid
            if (msg.fromUid.isNotBlank()) {
                updateList(msg.fromUid)
            }
        }

        _conversations.value = currentMap

        // Play sound and trigger heads-up incoming notification in real time
        if (hasNewIncoming && notifyIncoming && latestIncomingMsg != null) {
            com.example.util.CallAudioManager.playMessageReceivedSound(context)
            _incomingMessageAlert.value = latestIncomingMsg
        }
    }

    fun getMessages(otherUid: String): List<ChatMessage> {
        val user = _currentUser.value
        val fromDirectKey = _conversations.value[otherUid] ?: emptyList()
        if (user != null) {
            val convoKey = if (user.uid < otherUid) "${user.uid}_$otherUid" else "${otherUid}_${user.uid}"
            val fromConvoKey = _conversations.value[convoKey] ?: emptyList()
            val combined = (fromDirectKey + fromConvoKey).distinctBy { it.id }.sortedBy { it.createdAt }
            if (combined.isNotEmpty()) return combined
        }
        return fromDirectKey
    }

    fun sendMessage(otherUid: String, text: String) {
        val user = _currentUser.value ?: return
        val convoId = if (user.uid < otherUid) "${user.uid}_$otherUid" else "${otherUid}_${user.uid}"
        val newMsg = ChatMessage(
            id = "msg_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(6),
            convoId = convoId,
            fromUid = user.uid,
            toUid = otherUid,
            text = text,
            createdAt = System.currentTimeMillis()
        )
        // Zero-latency instant local update across convoId and direct UID
        mergeMessagesIntoConversations(listOf(newMsg), notifyIncoming = false)
        com.example.util.CallAudioManager.playMessageSentSound()

        // Instant write to Firestore
        FirebaseManager.sendMessage(newMsg)

        // Instant push notification to recipient
        val notif = NotificationItem(
            id = "notif_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(6),
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            toUid = otherUid,
            type = "message",
            text = "${user.displayName}: $text",
            targetId = user.uid,
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.sendNotification(notif)
    }

    fun logCall(otherUid: String, callType: String, callStatus: String, durationSec: Int) {
        val user = _currentUser.value ?: return
        val convoId = if (user.uid < otherUid) "${user.uid}_$otherUid" else "${otherUid}_${user.uid}"
        val newMsg = ChatMessage(
            id = "call_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(6),
            convoId = convoId,
            fromUid = user.uid,
            toUid = otherUid,
            text = if (callType == "video") "Video call" else "Audio call",
            isCallLog = true,
            callType = callType,
            callStatus = callStatus,
            callDurationSec = durationSec,
            createdAt = System.currentTimeMillis()
        )
        mergeMessagesIntoConversations(listOf(newMsg), notifyIncoming = false)
        FirebaseManager.sendMessage(newMsg)
    }

    // CALL SIGNALING METHODS
    fun initiateCall(recipient: User, callType: String, onStarted: (CallSession) -> Unit) {
        val cur = _currentUser.value ?: return
        val callId = "call_${System.currentTimeMillis()}_${cur.uid.take(4)}_${recipient.uid.take(4)}"
        val cleanRoomId = "meskot_call_${callId.replace("_", "").replace("-", "").lowercase()}"
        val session = CallSession(
            callId = callId,
            callerUid = cur.uid,
            callerName = cur.displayName,
            callerPhoto = cur.photoUrl,
            receiverUid = recipient.uid,
            receiverName = recipient.displayName,
            receiverPhoto = recipient.photoUrl,
            callType = callType,
            status = "ringing",
            roomUrl = "https://meet.jit.si/$cleanRoomId#config.prejoinPageEnabled=false&config.startWithAudioMuted=false&config.startWithVideoMuted=${if (callType == "audio") "true" else "false"}&interfaceConfig.TOOLBAR_BUTTONS=[]",
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.createCall(session) { ok ->
            if (ok) {
                onStarted(session)
            }
        }
    }

    fun listenToCallSession(callId: String, onUpdate: (CallSession?) -> Unit) {
        activeCallListenerRegistration?.remove()
        activeCallListenerRegistration = FirebaseManager.listenToCall(callId) { session ->
            onUpdate(session)
        }
    }

    fun stopListeningToCallSession() {
        activeCallListenerRegistration?.remove()
        activeCallListenerRegistration = null
    }

    fun acceptIncomingCall(callId: String) {
        _incomingCall.value = null
        FirebaseManager.updateCallStatus(callId, "accepted", startedAt = System.currentTimeMillis())
    }

    fun declineIncomingCall(callId: String) {
        val inc = _incomingCall.value
        _incomingCall.value = null
        FirebaseManager.updateCallStatus(callId, "rejected", endedAt = System.currentTimeMillis())
        if (inc != null) {
            logCall(inc.callerUid, inc.callType, "missed", 0)
        }
    }

    fun dismissIncomingCall() {
        _incomingCall.value = null
    }

    fun terminateCall(callId: String, otherUid: String, callType: String, durationSec: Int) {
        stopListeningToCallSession()
        FirebaseManager.updateCallStatus(callId, "ended", endedAt = System.currentTimeMillis())
        logCall(
            otherUid = otherUid,
            callType = callType,
            callStatus = if (durationSec > 0) "completed" else "missed",
            durationSec = durationSec
        )
    }

    fun editMessage(otherUid: String, msgId: String, newText: String) {
        val currentMap = _conversations.value.toMutableMap()
        currentMap.keys.forEach { key ->
            val list = currentMap[key] ?: emptyList()
            if (list.any { it.id == msgId }) {
                currentMap[key] = list.map {
                    if (it.id == msgId) it.copy(text = newText, editedAt = System.currentTimeMillis()) else it
                }
            }
        }
        _conversations.value = currentMap
        FirebaseManager.updateMessageText(msgId, newText)
    }

    fun deleteMessage(otherUid: String, msgId: String) {
        val currentMap = _conversations.value.toMutableMap()
        currentMap.keys.forEach { key ->
            val list = currentMap[key] ?: emptyList()
            if (list.any { it.id == msgId }) {
                currentMap[key] = list.filterNot { it.id == msgId }
            }
        }
        _conversations.value = currentMap
        FirebaseManager.deleteMessage(msgId)
    }

    // GROUPS
    fun toggleGroupJoin(groupId: String) {
        _groups.value = _groups.value.map { g ->
            if (g.id == groupId) {
                val newJoined = !g.isJoined
                g.copy(
                    isJoined = newJoined,
                    memberCount = if (newJoined) g.memberCount + 1 else maxOf(1, g.memberCount - 1)
                )
            } else g
        }
    }

    fun createGroup(name: String, description: String) {
        val user = _currentUser.value ?: return
        val colors = listOf("#B8863A", "#8C2F39", "#2A4838", "#4A3B5C", "#335577")
        val color = colors[(_groups.value.size) % colors.size]
        val newGroup = GroupItem(
            id = "group_" + System.currentTimeMillis(),
            name = name,
            description = description,
            createdBy = user.uid,
            memberCount = 1,
            isJoined = true,
            coverColorHex = color
        )
        _groups.value = listOf(newGroup) + _groups.value
    }

    fun createGroupPost(groupId: String, text: String, mediaUrls: List<String> = emptyList()) {
        val user = _currentUser.value ?: return
        val newPost = Post(
            id = "gpost_" + System.currentTimeMillis(),
            uid = user.uid,
            authorName = user.displayName,
            authorPhoto = user.photoUrl,
            text = text,
            mediaUrls = mediaUrls,
            createdAt = System.currentTimeMillis()
        )
        val currentPosts = _groupPosts.value[groupId] ?: emptyList()
        _groupPosts.value = _groupPosts.value + (groupId to (listOf(newPost) + currentPosts))
    }

    // ALBUMS
    fun createAlbum(title: String) {
        val user = _currentUser.value ?: return
        val newAlbum = AlbumItem(
            id = "album_" + System.currentTimeMillis(),
            uid = user.uid,
            title = title,
            count = 0,
            coverUrl = "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=500&auto=format&fit=crop&q=80",
            photos = emptyList()
        )
        _albums.value = listOf(newAlbum) + _albums.value
    }

    fun addPhotoToAlbum(albumId: String, photoUrl: String) {
        _albums.value = _albums.value.map { album ->
            if (album.id == albumId) {
                album.copy(
                    photos = album.photos + photoUrl,
                    count = album.count + 1,
                    coverUrl = photoUrl
                )
            } else album
        }
    }

    // NOTIFICATIONS
    private fun addNotification(
        fromUid: String,
        fromName: String,
        fromPhoto: String,
        type: String,
        targetId: String? = null,
        reactionType: String? = null,
        amount: Double? = null,
        customText: String? = null
    ) {
        val notifText = customText ?: when (type) {
            "reaction" -> "$fromName reacted to your post"
            "like" -> "$fromName liked your post"
            "comment" -> "$fromName commented on your post"
            "friend_req", "friend_request" -> "$fromName sent you a friend request"
            "friend_accept" -> "$fromName accepted your friend request"
            "tip" -> "$fromName sent you a tip of ${amount?.toInt() ?: 25} ETB via Chapa!"
            "share" -> "$fromName shared your post"
            else -> "$fromName interacted with your profile"
        }

        val newNotif = NotificationItem(
            id = "notif_" + System.currentTimeMillis(),
            fromUid = fromUid,
            fromName = fromName,
            fromPhoto = fromPhoto,
            text = notifText,
            type = type,
            targetId = targetId,
            reactionType = reactionType,
            amount = amount,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )
        _notifications.value = listOf(newNotif) + _notifications.value
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    // ADMIN ACTIONS
    fun adminToggleSuspend(uid: String) {
        _users.value = _users.value.map {
            if (it.uid == uid) it.copy(isSuspended = !it.isSuspended) else it
        }
    }

    fun adminToggleAdmin(uid: String) {
        _users.value = _users.value.map {
            if (it.uid == uid) it.copy(isAdmin = !it.isAdmin) else it
        }
    }

    fun adminDeleteGroup(groupId: String) {
        _groups.value = _groups.value.filterNot { it.id == groupId }
    }

    // INITIAL DATA GENERATORS
    private fun createInitialPosts(): List<Post> = emptyList()

    private fun createInitialComments(): Map<String, List<Comment>> = emptyMap()

    private fun createInitialGroups(): List<GroupItem> {
        return listOf(
            GroupItem(
                id = "group_tech",
                name = "Addis Tech & Startups (አዲስ ቴክ)",
                description = "Ethiopian founders, developers, engineers, and digital creators building products for Africa and the diaspora.",
                createdBy = "meskot",
                memberCount = 142,
                isJoined = true,
                coverColorHex = "#2A4838"
            ),
            GroupItem(
                id = "group_culture",
                name = "Habesha Food & Culinary Arts (የባህል ምግብ)",
                description = "Sharing traditional recipes, spice blends (berbere, mitmita), injera techniques, and diaspora cooking secrets.",
                createdBy = "meskot",
                memberCount = 89,
                isJoined = true,
                coverColorHex = "#8C2F39"
            ),
            GroupItem(
                id = "group_art",
                name = "Ethiopian Photography & Travel",
                description = "Visual journey across Simien Mountains, Danakil Depression, Omo Valley, Harar Jugol, and modern Addis nightlife.",
                createdBy = "meskot",
                memberCount = 64,
                isJoined = false,
                coverColorHex = "#B8863A"
            ),
            GroupItem(
                id = "group_literature",
                name = "Ge'ez & Ethiopian Literature",
                description = "Appreciating classic Ethiopian manuscripts, poetry (Qene), novels, and contemporary Habesha authors.",
                createdBy = "meskot",
                memberCount = 38,
                isJoined = false,
                coverColorHex = "#4A3B5C"
            )
        )
    }

    private fun createInitialGroupPosts(): Map<String, List<Post>> = emptyMap()

    private fun createInitialAlbums(): List<AlbumItem> = emptyList()

    private fun createInitialChatMessages(): Map<String, List<ChatMessage>> = emptyMap()

    private fun createInitialNotifications(): List<NotificationItem> = emptyList()
}
