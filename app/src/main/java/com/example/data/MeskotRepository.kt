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

    // Demo / Default Users
    val demoUsers = listOf(
        User(
            uid = "user_sara",
            displayName = "Sara Tekle",
            email = "sara@meskot.et",
            bio = "Product Designer & Habesha Art enthusiast · Addis Ababa 🇪🇹",
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
            isAdmin = true,
            createdAt = System.currentTimeMillis() - 86400000L * 90
        ),
        User(
            uid = "user_dawit",
            displayName = "Dawit Bekele",
            email = "dawit@meskot.et",
            bio = "Software Engineer & Coffee lover ☕️ · Bole, Addis",
            photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
            isAdmin = false,
            createdAt = System.currentTimeMillis() - 86400000L * 60
        ),
        User(
            uid = "user_helen",
            displayName = "Helen Assefa",
            email = "helen@meskot.et",
            bio = "Photographer capturing Ethiopian heritage and daily moments 📸",
            photoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
            isAdmin = false,
            createdAt = System.currentTimeMillis() - 86400000L * 45
        ),
        User(
            uid = "user_yohannes",
            displayName = "Yohannes Haile",
            email = "yohannes@meskot.et",
            bio = "Cultural historian studying Ge'ez literature and Axumite art 📜",
            photoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
            isAdmin = false,
            createdAt = System.currentTimeMillis() - 86400000L * 30
        ),
        User(
            uid = "user_eden",
            displayName = "Eden Girma",
            email = "eden@meskot.et",
            bio = "Culinary artist celebrating Ethiopian dishes & spice blends 🌶️",
            photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
            isAdmin = false,
            createdAt = System.currentTimeMillis() - 86400000L * 20
        )
    )

    // Current logged-in user (defaults to Sara Tekle for immediate rich interactivity)
    private val _currentUser = MutableStateFlow<User?>(demoUsers[0])
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Users list
    private val _users = MutableStateFlow<List<User>>(demoUsers)
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Posts list
    private val _posts = MutableStateFlow<List<Post>>(createInitialPosts())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // Comments map: postId -> List<Comment>
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(createInitialComments())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    // Friendships: Set of (fromUid, toUid)
    private val _friends = MutableStateFlow<Set<String>>(setOf("user_dawit", "user_helen"))
    val friends: StateFlow<Set<String>> = _friends.asStateFlow()

    // Pending incoming friend requests
    private val _incomingRequests = MutableStateFlow<List<User>>(listOf(demoUsers[3])) // Yohannes
    val incomingRequests: StateFlow<List<User>> = _incomingRequests.asStateFlow()

    // Pending outgoing friend requests
    private val _outgoingRequests = MutableStateFlow<Set<String>>(emptySet())
    val outgoingRequests: StateFlow<Set<String>> = _outgoingRequests.asStateFlow()

    // Groups
    private val _groups = MutableStateFlow<List<GroupItem>>(createInitialGroups())
    val groups: StateFlow<List<GroupItem>> = _groups.asStateFlow()

    // Group posts map: groupId -> List<Post>
    private val _groupPosts = MutableStateFlow<Map<String, List<Post>>>(createInitialGroupPosts())
    val groupPosts: StateFlow<Map<String, List<Post>>> = _groupPosts.asStateFlow()

    // Albums
    private val _albums = MutableStateFlow<List<AlbumItem>>(createInitialAlbums())
    val albums: StateFlow<List<AlbumItem>> = _albums.asStateFlow()

    // Conversations map: otherUid -> List<ChatMessage>
    private val _conversations = MutableStateFlow<Map<String, List<ChatMessage>>>(createInitialChatMessages())
    val conversations: StateFlow<Map<String, List<ChatMessage>>> = _conversations.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(createInitialNotifications())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Saved post IDs
    private val _savedPostIds = MutableStateFlow<Set<String>>(setOf("post_1"))
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()

    // Hidden post IDs
    private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPostIds: StateFlow<Set<String>> = _hiddenPostIds.asStateFlow()

    init {
        FirebaseManager.initialize(context)
        setupFirebaseListeners()
    }

    private fun setupFirebaseListeners() {
        try {
            FirebaseManager.listenToPosts { livePosts ->
                val liveIds = livePosts.map { it.id }.toSet()
                val remainingLocal = _posts.value.filterNot { it.id in liveIds }
                _posts.value = (livePosts + remainingLocal).sortedByDescending { it.createdAt }
            }

            FirebaseManager.listenToComments { liveComments ->
                val current = _comments.value.toMutableMap()
                liveComments.forEach { (postId, comments) ->
                    current[postId] = comments
                }
                _comments.value = current
            }

            FirebaseManager.listenToUsers { liveUsers ->
                val liveMap = liveUsers.associateBy { it.uid }
                val updated = _users.value.map { liveMap[it.uid] ?: it } +
                    liveUsers.filterNot { lu -> _users.value.any { it.uid == lu.uid } }
                _users.value = updated
            }
        } catch (e: Exception) {
            android.util.Log.e("MeskotRepository", "Could not setup Firebase listeners: ${e.message}")
        }
    }

    // AUTH METHODS
    fun login(email: String, pass: String): Boolean {
        val found = _users.value.find { it.email.equals(email.trim(), ignoreCase = true) }
        if (found != null) {
            _currentUser.value = found
            FirebaseManager.signInWithEmail(email, pass, onSuccess = { fbUser ->
                _currentUser.value = fbUser
            }, onFailure = {})
            return true
        }
        FirebaseManager.signInWithEmail(email, pass, onSuccess = { fbUser ->
            _currentUser.value = fbUser
            if (_users.value.none { it.uid == fbUser.uid }) {
                _users.value = _users.value + fbUser
            }
        }, onFailure = {})

        val newUser = User(
            uid = "user_" + UUID.randomUUID().toString().take(6),
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email.trim(),
            bio = "Member of Meskot community"
        )
        _users.value = _users.value + newUser
        _currentUser.value = newUser
        FirebaseManager.saveUser(newUser)
        return true
    }

    fun signup(fullName: String, email: String, pass: String): Boolean {
        val newUser = User(
            uid = "user_" + UUID.randomUUID().toString().take(6),
            displayName = fullName.trim(),
            email = email.trim(),
            bio = "New member of Meskot community"
        )
        _users.value = _users.value + newUser
        _currentUser.value = newUser

        FirebaseManager.signUpWithEmail(fullName, email, pass, onSuccess = { fbUser ->
            _currentUser.value = fbUser
            _users.value = _users.value.map { if (it.uid == newUser.uid) fbUser else it }
        }, onFailure = {
            FirebaseManager.saveUser(newUser)
        })
        return true
    }

    fun switchUser(user: User) {
        _currentUser.value = user
    }

    fun logout() {
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
    private var friendRequestsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var messagesListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    fun listenToFriendRequests() {
        val user = _currentUser.value ?: return
        friendRequestsListener?.remove()
        friendRequestsListener = FirebaseManager.listenToFriendRequests(user.uid) { requests ->
            val pendingIncomingSenderIds = requests
                .filter { it.receiverId == user.uid && it.status == "pending" }
                .map { it.senderId }
                .toSet()
            _incomingRequests.value = _users.value.filter { it.uid in pendingIncomingSenderIds }

            val pendingOutgoingReceiverIds = requests
                .filter { it.senderId == user.uid && it.status == "pending" }
                .map { it.receiverId }
                .toSet()
            _outgoingRequests.value = pendingOutgoingReceiverIds

            val acceptedPairs = requests.filter { it.status == "accepted" }
            val newFriends = acceptedPairs.flatMap { listOf(it.senderId, it.receiverId) }
                .filter { it != user.uid }
                .toSet()
            if (newFriends.isNotEmpty()) {
                _friends.value = _friends.value + newFriends
            }
        }
    }

    fun listenToMessages(otherUid: String) {
        val user = _currentUser.value ?: return
        val convId = getConversationId(user.uid, otherUid)
        messagesListener?.remove()
        messagesListener = FirebaseManager.listenToMessages(convId) { firestoreMessages ->
            val currentMsgs = _conversations.value[otherUid] ?: emptyList()
            val merged = (currentMsgs + firestoreMessages).distinctBy { it.id }
            _conversations.value = _conversations.value + (otherUid to merged)
        }
    }

    fun sendFriendRequest(toUid: String) {
        val user = _currentUser.value ?: return
        _outgoingRequests.value = _outgoingRequests.value + toUid
        FirebaseManager.sendFriendRequest(user.uid, toUid)
        addNotification(
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            type = "friend_request"
        )
    }

    fun cancelFriendRequest(toUid: String) {
        _outgoingRequests.value = _outgoingRequests.value - toUid
    }

    fun acceptFriendRequest(fromUid: String) {
        val user = _currentUser.value ?: return
        _friends.value = _friends.value + fromUid
        _incomingRequests.value = _incomingRequests.value.filterNot { it.uid == fromUid }
        addNotification(
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            type = "friend_accept"
        )
    }

    fun declineFriendRequest(fromUid: String) {
        _incomingRequests.value = _incomingRequests.value.filterNot { it.uid == fromUid }
    }

    fun unfriend(uid: String) {
        _friends.value = _friends.value - uid
    }

    // MESSAGES & CHAT
    fun getMessages(otherUid: String): List<ChatMessage> {
        return _conversations.value[otherUid] ?: emptyList()
    }

    fun sendMessage(otherUid: String, text: String) {
        val user = _currentUser.value ?: return
        val convId = getConversationId(user.uid, otherUid)
        val newMsg = ChatMessage(
            id = "msg_" + System.currentTimeMillis(),
            convoId = convId,
            fromUid = user.uid,
            text = text,
            createdAt = System.currentTimeMillis()
        )
        val currentMsgs = _conversations.value[otherUid] ?: emptyList()
        _conversations.value = _conversations.value + (otherUid to (currentMsgs + newMsg))
        FirebaseManager.sendMessage(newMsg)
        addNotification(
            fromUid = user.uid,
            fromName = user.displayName,
            fromPhoto = user.photoUrl,
            type = "message",
            targetId = otherUid
        )
    }

    fun logCall(otherUid: String, callType: String, callStatus: String, durationSec: Int) {
        val user = _currentUser.value ?: return
        val newMsg = ChatMessage(
            id = "call_" + System.currentTimeMillis(),
            convoId = otherUid,
            fromUid = user.uid,
            text = if (callType == "video") "Video call" else "Audio call",
            isCallLog = true,
            callType = callType,
            callStatus = callStatus,
            callDurationSec = durationSec,
            createdAt = System.currentTimeMillis()
        )
        val currentMsgs = _conversations.value[otherUid] ?: emptyList()
        _conversations.value = _conversations.value + (otherUid to (currentMsgs + newMsg))
    }

    fun editMessage(otherUid: String, msgId: String, newText: String) {
        val currentMsgs = _conversations.value[otherUid] ?: return
        _conversations.value = _conversations.value + (otherUid to currentMsgs.map {
            if (it.id == msgId) it.copy(text = newText, editedAt = System.currentTimeMillis()) else it
        })
    }

    fun deleteMessage(otherUid: String, msgId: String) {
        val currentMsgs = _conversations.value[otherUid] ?: return
        _conversations.value = _conversations.value + (otherUid to currentMsgs.filterNot { it.id == msgId })
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

    // INITIAL SAMPLE DATA
    private fun createInitialPosts(): List<Post> {
        val now = System.currentTimeMillis()
        return listOf(
            Post(
                id = "post_1",
                uid = "user_sara",
                authorName = "Sara Tekle",
                authorPhoto = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                text = "እንኳን ወደ መስኮት (Meskot) በደህና መጣችሁ! 🌿✨\n\nMeskot is designed as our authentic community window—connecting Ethiopian and Habesha creatives, thinkers, and friends worldwide. Share your thoughts, art, memories, and stories with your circle!",
                mediaUrls = listOf("https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=800&auto=format&fit=crop&q=80"),
                reactions = mapOf("user_dawit" to "love", "user_helen" to "like", "user_yohannes" to "love"),
                commentCount = 3,
                tipTotal = 50.0,
                createdAt = now - 3600000L * 2
            ),
            Post(
                id = "post_2",
                uid = "user_eden",
                authorName = "Eden Girma",
                authorPhoto = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
                text = "የእሁድ የቡና ስነ-ስርዓት ከቤተሰብ ጋር! ☕️ Traditional Ethiopian Jebena Buna roasted fresh with frankincense aroma filling the room. Buna tetu!",
                mediaUrls = listOf("https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&auto=format&fit=crop&q=80"),
                reactions = mapOf("user_sara" to "love", "user_dawit" to "like"),
                commentCount = 2,
                tipTotal = 25.0,
                createdAt = now - 3600000L * 5
            ),
            Post(
                id = "post_3",
                uid = "user_dawit",
                authorName = "Dawit Bekele",
                authorPhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                text = "Building technology rooted in African identity is the most exciting frontier of our generation. 🚀",
                bgColorIndex = 1, // Red & Gold gradient card
                reactions = mapOf("user_sara" to "like", "user_yohannes" to "wow"),
                commentCount = 1,
                createdAt = now - 3600000L * 10
            ),
            Post(
                id = "post_4",
                uid = "user_yohannes",
                authorName = "Yohannes Haile",
                authorPhoto = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                text = "Exploring the architectural geometry of Bet Giyorgis in Lalibela. Carved out of monolithic red volcanic rock in the 12th century—a testament to architectural mastery and spiritual devotion.",
                mediaUrls = listOf("https://images.unsplash.com/photo-1578922746465-3a80a228f223?w=800&auto=format&fit=crop&q=80"),
                reactions = mapOf("user_helen" to "love", "user_sara" to "like", "user_eden" to "wow"),
                commentCount = 2,
                tipTotal = 100.0,
                createdAt = now - 86400000L
            )
        )
    }

    private fun createInitialComments(): Map<String, List<Comment>> {
        val now = System.currentTimeMillis()
        return mapOf(
            "post_1" to listOf(
                Comment(
                    id = "c_1",
                    postId = "post_1",
                    uid = "user_dawit",
                    authorName = "Dawit Bekele",
                    authorPhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                    text = "እንኳን ደስ አላችሁ! Proud to see a digital space tailored specifically for our community.",
                    likes = mapOf("user_sara" to true),
                    createdAt = now - 3600000L
                ),
                Comment(
                    id = "c_2",
                    postId = "post_1",
                    uid = "user_helen",
                    authorName = "Helen Assefa",
                    authorPhoto = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                    text = "The bilingual Amharic and English interface looks so elegant! Love the lattice window motif.",
                    likes = mapOf("user_sara" to true, "user_dawit" to true),
                    createdAt = now - 1800000L
                ),
                Comment(
                    id = "c_3",
                    postId = "post_1",
                    uid = "user_sara",
                    authorName = "Sara Tekle",
                    authorPhoto = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                    text = "Thank you everyone! Looking forward to hearing all of your voices here. ❤️",
                    parentId = "c_2",
                    createdAt = now - 900000L
                )
            ),
            "post_2" to listOf(
                Comment(
                    id = "c_4",
                    postId = "post_2",
                    uid = "user_sara",
                    authorName = "Sara Tekle",
                    authorPhoto = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                    text = "Nothing compares to Sunday buna! Especially with fresh popcorn (fendisha). ☕️🍿",
                    createdAt = now - 7200000L
                )
            )
        )
    }

    private fun createInitialGroups(): List<GroupItem> {
        return listOf(
            GroupItem(
                id = "group_tech",
                name = "Addis Tech & Startups (አዲስ ቴክ)",
                description = "Ethiopian founders, developers, engineers, and digital creators building products for Africa and the diaspora.",
                createdBy = "user_dawit",
                memberCount = 142,
                isJoined = true,
                coverColorHex = "#2A4838"
            ),
            GroupItem(
                id = "group_culture",
                name = "Habesha Food & Culinary Arts (የባህል ምግብ)",
                description = "Sharing traditional recipes, spice blends (berbere, mitmita), injera techniques, and diaspora cooking secrets.",
                createdBy = "user_eden",
                memberCount = 89,
                isJoined = true,
                coverColorHex = "#8C2F39"
            ),
            GroupItem(
                id = "group_art",
                name = "Ethiopian Photography & Travel",
                description = "Visual journey across Simien Mountains, Danakil Depression, Omo Valley, Harar Jugol, and modern Addis nightlife.",
                createdBy = "user_helen",
                memberCount = 64,
                isJoined = false,
                coverColorHex = "#B8863A"
            ),
            GroupItem(
                id = "group_literature",
                name = "Ge'ez & Ethiopian Literature",
                description = "Appreciating classic Ethiopian manuscripts, poetry (Qene), novels, and contemporary Habesha authors.",
                createdBy = "user_yohannes",
                memberCount = 38,
                isJoined = false,
                coverColorHex = "#4A3B5C"
            )
        )
    }

    private fun createInitialGroupPosts(): Map<String, List<Post>> {
        val now = System.currentTimeMillis()
        return mapOf(
            "group_tech" to listOf(
                Post(
                    id = "gp_1",
                    uid = "user_dawit",
                    authorName = "Dawit Bekele",
                    authorPhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                    text = "Excited to announce our upcoming Addis Mobile Dev meetup! We'll be discussing local offline-first architecture and Kotlin Multiplatform.",
                    createdAt = now - 3600000L * 4
                )
            ),
            "group_culture" to listOf(
                Post(
                    id = "gp_2",
                    uid = "user_eden",
                    authorName = "Eden Girma",
                    authorPhoto = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
                    text = "Secret to a rich Doro Wat: cooking the red onions down for at least 45 minutes until caramelized before adding the niter kibbeh and berbere! 🍗",
                    createdAt = now - 3600000L * 8
                )
            )
        )
    }

    private fun createInitialAlbums(): List<AlbumItem> {
        return listOf(
            AlbumItem(
                id = "album_1",
                uid = "user_sara",
                title = "Addis Skylines & Entoto Hills",
                count = 4,
                coverUrl = "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=500&auto=format&fit=crop&q=80",
                photos = listOf(
                    "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1448375240586-882707db888b?w=800&auto=format&fit=crop&q=80"
                )
            ),
            AlbumItem(
                id = "album_2",
                uid = "user_sara",
                title = "Ethiopian Coffee Ceremonies",
                count = 3,
                coverUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500&auto=format&fit=crop&q=80",
                photos = listOf(
                    "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=800&auto=format&fit=crop&q=80"
                )
            )
        )
    }

    private fun createInitialChatMessages(): Map<String, List<ChatMessage>> {
        val now = System.currentTimeMillis()
        return mapOf(
            "user_dawit" to listOf(
                ChatMessage(
                    id = "m_1",
                    convoId = "user_dawit",
                    fromUid = "user_dawit",
                    text = "Selam Sara! How is the new Meskot design coming along?",
                    createdAt = now - 3600000L * 3
                ),
                ChatMessage(
                    id = "m_2",
                    convoId = "user_dawit",
                    fromUid = "user_sara",
                    text = "Selam Dawit! It's looking really sharp. The Amharic typography and lattice window identity are integrated beautifully.",
                    createdAt = now - 3600000L * 2
                ),
                ChatMessage(
                    id = "m_3",
                    convoId = "user_dawit",
                    fromUid = "user_dawit",
                    text = "Fantastic! Let's connect on a quick call later today.",
                    createdAt = now - 3600000L
                ),
                ChatMessage(
                    id = "m_4",
                    convoId = "user_dawit",
                    fromUid = "user_dawit",
                    text = "Audio call",
                    isCallLog = true,
                    callType = "audio",
                    callStatus = "completed",
                    callDurationSec = 145,
                    createdAt = now - 1800000L
                )
            ),
            "user_helen" to listOf(
                ChatMessage(
                    id = "m_5",
                    convoId = "user_helen",
                    fromUid = "user_helen",
                    text = "Hi Sara! Loved your latest post on the heritage photography.",
                    createdAt = now - 86400000L
                )
            )
        )
    }

    private fun createInitialNotifications(): List<NotificationItem> {
        val now = System.currentTimeMillis()
        return listOf(
            NotificationItem(
                id = "n_1",
                fromUid = "user_dawit",
                fromName = "Dawit Bekele",
                fromPhoto = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                text = "Dawit Bekele liked your post",
                type = "like",
                targetId = "post_1",
                isRead = false,
                createdAt = now - 1800000L
            ),
            NotificationItem(
                id = "n_2",
                fromUid = "user_helen",
                fromName = "Helen Assefa",
                fromPhoto = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                text = "Helen Assefa commented: 'Amen! Betam yastemiral.'",
                type = "comment",
                targetId = "post_1",
                isRead = false,
                createdAt = now - 3600000L
            ),
            NotificationItem(
                id = "n_3",
                fromUid = "user_yohannes",
                fromName = "Yohannes Haile",
                fromPhoto = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                text = "Yohannes Haile sent you a friend request",
                type = "friend_request",
                isRead = true,
                createdAt = now - 86400000L
            )
        )
    }
}
