package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set

    // Collections
    const val COL_USERS = "users"
    const val COL_POSTS = "posts"
    const val COL_COMMENTS = "comments"
    const val COL_GROUPS = "groups"
    const val COL_ALBUMS = "albums"
    const val COL_MESSAGES = "messages"
    const val COL_NOTIFICATIONS = "notifications"
    const val COL_FRIEND_REQUESTS = "friend_requests"
    const val COL_FRIENDSHIPS = "friendships"
    const val COL_CALLS = "calls"

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val existingApps = FirebaseApp.getApps(context)
            val app = if (existingApps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyD9j6Emef7z1sxtGuqgEePwIJetPo4ZhQQ")
                    .setApplicationId("1:1083041536701:android:4654f20e5ae08a2d0435d4")
                    .setProjectId("meskot-b1a69")
                    .setStorageBucket("meskot-b1a69.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                existingApps.first()
            }

            auth = FirebaseAuth.getInstance(app)
            val db = FirebaseFirestore.getInstance(app)
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            } catch (se: Exception) {
                Log.w(TAG, "Could not apply custom persistence settings: ${se.message}")
            }
            firestore = db
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully with project meskot-b1a69")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization fallback (offline/cached mode): ${e.message}")
        }
    }

    fun isConfigured(): Boolean = isInitialized && auth != null && firestore != null

    // AUTH METHODS
    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fbAuth = auth
        if (fbAuth == null) {
            onFailure("Firebase Auth not initialized")
            return
        }
        fbAuth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser != null) {
                    val uid = fbUser.uid
                    // Fetch user document from Firestore
                    firestore?.collection(COL_USERS)?.document(uid)?.get()
                        ?.addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                val user = parseUser(doc.id, doc.data ?: emptyMap())
                                onSuccess(user)
                            } else {
                                val newUser = User(
                                    uid = uid,
                                    displayName = fbUser.displayName ?: email.substringBefore("@"),
                                    email = fbUser.email ?: email,
                                    photoUrl = fbUser.photoUrl?.toString() ?: ""
                                )
                                saveUser(newUser)
                                onSuccess(newUser)
                            }
                        }
                        ?.addOnFailureListener {
                            val user = User(
                                uid = uid,
                                displayName = fbUser.displayName ?: email.substringBefore("@"),
                                email = fbUser.email ?: email,
                                photoUrl = fbUser.photoUrl?.toString() ?: ""
                            )
                            onSuccess(user)
                        }
                } else {
                    onFailure("No user returned")
                }
            }
            .addOnFailureListener { err ->
                Log.e(TAG, "Sign in failed: ${err.message}")
                onFailure(err.localizedMessage ?: "Sign in failed")
            }
    }

    fun signUpWithEmail(
        fullName: String,
        email: String,
        pass: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fbAuth = auth
        if (fbAuth == null) {
            onFailure("Firebase Auth not initialized")
            return
        }
        fbAuth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()
                    fbUser.updateProfile(profileUpdates)

                    val newUser = User(
                        uid = fbUser.uid,
                        displayName = fullName.trim(),
                        email = fbUser.email ?: email.trim(),
                        bio = "Member of Meskot community",
                        photoUrl = ""
                    )
                    saveUser(newUser)
                    onSuccess(newUser)
                } else {
                    onFailure("No user created")
                }
            }
            .addOnFailureListener { err ->
                Log.e(TAG, "Sign up failed: ${err.message}")
                onFailure(err.localizedMessage ?: "Sign up failed")
            }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth?.currentUser

    // FIRESTORE: POSTS
    fun listenToPosts(onPostsUpdated: (List<Post>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to posts: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val posts = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parsePost(doc.id, it) }
                        }
                        if (posts.isNotEmpty()) {
                            onPostsUpdated(posts)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach posts listener: ${e.message}")
            null
        }
    }

    fun createPost(post: Post, onComplete: (Boolean) -> Unit = {}) {
        val db = firestore ?: run { onComplete(false); return }
        val map = postToMap(post)
        db.collection(COL_POSTS).document(post.id).set(map, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                Log.e(TAG, "Failed to create post in Firestore: ${it.message}")
                onComplete(false)
            }
    }

    fun updatePostReactions(postId: String, reactions: Map<String, String>) {
        val db = firestore ?: return
        db.collection(COL_POSTS).document(postId).update("reactions", reactions)
            .addOnFailureListener { Log.e(TAG, "Failed to update reactions: ${it.message}") }
    }

    fun updatePostTip(postId: String, newTipTotal: Double) {
        val db = firestore ?: return
        db.collection(COL_POSTS).document(postId).update("tipTotal", newTipTotal)
            .addOnFailureListener { Log.e(TAG, "Failed to update tipTotal: ${it.message}") }
    }

    fun updatePostText(postId: String, newText: String) {
        val db = firestore ?: return
        db.collection(COL_POSTS).document(postId).update(
            mapOf("text" to newText, "editedAt" to System.currentTimeMillis())
        ).addOnFailureListener { Log.e(TAG, "Failed to update post text: ${it.message}") }
    }

    fun deletePost(postId: String) {
        val db = firestore ?: return
        db.collection(COL_POSTS).document(postId).delete()
            .addOnFailureListener { Log.e(TAG, "Failed to delete post: ${it.message}") }
    }

    // FIRESTORE: COMMENTS
    fun listenToComments(onCommentsUpdated: (Map<String, List<Comment>>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to comments: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val comments = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseComment(doc.id, it) }
                        }
                        if (comments.isNotEmpty()) {
                            val grouped = comments.groupBy { it.postId }
                            onCommentsUpdated(grouped)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach comments listener: ${e.message}")
            null
        }
    }

    fun addComment(comment: Comment, onComplete: (Boolean) -> Unit = {}) {
        val db = firestore ?: run { onComplete(false); return }
        val map = commentToMap(comment)
        db.collection(COL_COMMENTS).document(comment.id).set(map, SetOptions.merge())
            .addOnSuccessListener {
                // Increment comment count on post
                db.collection(COL_POSTS).document(comment.postId).get().addOnSuccessListener { postDoc ->
                    val cur = (postDoc.getLong("commentCount") ?: 0L).toInt()
                    db.collection(COL_POSTS).document(comment.postId).update("commentCount", cur + 1)
                }
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to add comment: ${it.message}")
                onComplete(false)
            }
    }

    fun deleteComment(postId: String, commentId: String) {
        val db = firestore ?: return
        db.collection(COL_COMMENTS).document(commentId).delete()
            .addOnSuccessListener {
                db.collection(COL_POSTS).document(postId).get().addOnSuccessListener { postDoc ->
                    val cur = (postDoc.getLong("commentCount") ?: 1L).toInt()
                    db.collection(COL_POSTS).document(postId).update("commentCount", maxOf(0, cur - 1))
                }
            }
            .addOnFailureListener { Log.e(TAG, "Failed to delete comment: ${it.message}") }
    }

    // FIRESTORE: USERS
    fun listenToUsers(onUsersUpdated: (List<User>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_USERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to users: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val users = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseUser(doc.id, it) }
                        }
                        onUsersUpdated(users)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach users listener: ${e.message}")
            null
        }
    }

    fun saveUser(user: User) {
        val db = firestore ?: return
        val map = userToMap(user)
        db.collection(COL_USERS).document(user.uid).set(map, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Failed to save user in Firestore: ${it.message}") }
    }

    // FIRESTORE: MESSAGES
    fun listenToMessages(onMessagesUpdated: (List<ChatMessage>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_MESSAGES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseChatMessage(doc.id, it) }
                        }.sortedBy { it.createdAt }
                        if (msgs.isNotEmpty()) {
                            onMessagesUpdated(msgs)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach messages listener: ${e.message}")
            null
        }
    }

    // Direct high-priority listener for messages addressed to or participated in by a specific user
    fun listenToUserMessages(uid: String, onMessagesUpdated: (List<ChatMessage>) -> Unit): List<ListenerRegistration> {
        val db = firestore ?: return emptyList()
        val registrations = mutableListOf<ListenerRegistration>()
        try {
            // 1. Direct incoming messages
            val regIncoming = db.collection(COL_MESSAGES)
                .whereEqualTo("toUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in incoming message listener for $uid: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseChatMessage(doc.id, it) }
                        }.sortedBy { it.createdAt }
                        if (msgs.isNotEmpty()) {
                            onMessagesUpdated(msgs)
                        }
                    }
                }
            registrations.add(regIncoming)

            // 2. Direct outgoing messages (for real-time multi-device sync)
            val regOutgoing = db.collection(COL_MESSAGES)
                .whereEqualTo("fromUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in outgoing message listener for $uid: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseChatMessage(doc.id, it) }
                        }.sortedBy { it.createdAt }
                        if (msgs.isNotEmpty()) {
                            onMessagesUpdated(msgs)
                        }
                    }
                }
            registrations.add(regOutgoing)

            // 3. User participant array
            val regUsers = db.collection(COL_MESSAGES)
                .whereArrayContains("users", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in participant message listener for $uid: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseChatMessage(doc.id, it) }
                        }.sortedBy { it.createdAt }
                        if (msgs.isNotEmpty()) {
                            onMessagesUpdated(msgs)
                        }
                    }
                }
            registrations.add(regUsers)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach user message listeners: ${e.message}")
        }
        return registrations
    }

    fun sendMessage(msg: ChatMessage, onComplete: ((Boolean) -> Unit)? = null) {
        val db = firestore ?: run {
            onComplete?.invoke(false)
            return
        }
        val map = chatMessageToMap(msg)
        db.collection(COL_MESSAGES).document(msg.id).set(map, SetOptions.merge())
            .addOnSuccessListener {
                onComplete?.invoke(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to send message: ${it.message}")
                onComplete?.invoke(false)
            }
    }

    fun updateMessageText(msgId: String, newText: String) {
        val db = firestore ?: return
        db.collection(COL_MESSAGES).document(msgId)
            .update(mapOf("text" to newText, "editedAt" to System.currentTimeMillis()))
            .addOnFailureListener { Log.e(TAG, "Failed to update message: ${it.message}") }
    }

    fun deleteMessage(msgId: String) {
        val db = firestore ?: return
        db.collection(COL_MESSAGES).document(msgId).delete()
            .addOnFailureListener { Log.e(TAG, "Failed to delete message: ${it.message}") }
    }

    // FIRESTORE: FRIEND REQUESTS
    fun sendFriendRequest(req: FriendRequest) {
        val db = firestore ?: return
        val map = friendRequestToMap(req)
        db.collection(COL_FRIEND_REQUESTS).document(req.id).set(map, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Failed to send friend request: ${it.message}") }
    }

    fun updateFriendRequestStatus(reqId: String, status: String) {
        val db = firestore ?: return
        db.collection(COL_FRIEND_REQUESTS).document(reqId).update("status", status)
            .addOnFailureListener { Log.e(TAG, "Failed to update friend request status: ${it.message}") }
    }

    fun deleteFriendRequest(reqId: String) {
        val db = firestore ?: return
        db.collection(COL_FRIEND_REQUESTS).document(reqId).delete()
            .addOnFailureListener { Log.e(TAG, "Failed to delete friend request: ${it.message}") }
    }

    fun listenToFriendRequests(onRequestsUpdated: (List<FriendRequest>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_FRIEND_REQUESTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friend requests: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val reqs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseFriendRequest(doc.id, it) }
                        }
                        onRequestsUpdated(reqs)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach friend requests listener: ${e.message}")
            null
        }
    }

    // FIRESTORE: FRIENDSHIPS
    fun addFriendship(uid1: String, uid2: String) {
        val db = firestore ?: return
        val docId = if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
        val map = mapOf(
            "users" to listOf(uid1, uid2),
            "uid1" to if (uid1 < uid2) uid1 else uid2,
            "uid2" to if (uid1 < uid2) uid2 else uid1,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection(COL_FRIENDSHIPS).document(docId).set(map, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Failed to record friendship: ${it.message}") }
    }

    fun removeFriendship(uid1: String, uid2: String) {
        val db = firestore ?: return
        val docId = if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
        db.collection(COL_FRIENDSHIPS).document(docId).delete()
            .addOnFailureListener { Log.e(TAG, "Failed to remove friendship: ${it.message}") }
    }

    fun listenToFriendships(onFriendshipsUpdated: (List<Pair<String, String>>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_FRIENDSHIPS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val pairs = snapshot.documents.mapNotNull { doc ->
                            val d = doc.data ?: return@mapNotNull null
                            val users = (d["users"] as? List<*>)?.filterIsInstance<String>()
                            if (users != null && users.size >= 2) {
                                users[0] to users[1]
                            } else null
                        }
                        onFriendshipsUpdated(pairs)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach friendships listener: ${e.message}")
            null
        }
    }

    // FIRESTORE: NOTIFICATIONS
    fun sendNotification(notif: NotificationItem) {
        val db = firestore ?: return
        val map = notificationToMap(notif)
        db.collection(COL_NOTIFICATIONS).document(notif.id).set(map, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Failed to send notification: ${it.message}") }
    }

    fun markNotificationRead(notifId: String) {
        val db = firestore ?: return
        db.collection(COL_NOTIFICATIONS).document(notifId).update("isRead", true)
            .addOnFailureListener { Log.e(TAG, "Failed to mark notification as read: ${it.message}") }
    }

    fun listenToNotifications(forUid: String, onNotificationsUpdated: (List<NotificationItem>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("toUid", forUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to notifications: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val notifs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseNotification(doc.id, it) }
                        }.sortedByDescending { it.createdAt }
                        onNotificationsUpdated(notifs)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach notifications listener: ${e.message}")
            null
        }
    }

    // SERIALIZATION HELPERS
    private fun postToMap(p: Post): Map<String, Any?> = mapOf(
        "id" to p.id,
        "uid" to p.uid,
        "authorName" to p.authorName,
        "authorPhoto" to p.authorPhoto,
        "text" to p.text,
        "mediaUrls" to p.mediaUrls,
        "bgColorIndex" to p.bgColorIndex,
        "visibility" to p.visibility,
        "reactions" to p.reactions,
        "commentCount" to p.commentCount,
        "tipTotal" to p.tipTotal,
        "createdAt" to p.createdAt,
        "editedAt" to p.editedAt
    )

    private fun parsePost(id: String, d: Map<String, Any?>): Post = Post(
        id = id,
        uid = d["uid"] as? String ?: "",
        authorName = d["authorName"] as? String ?: "Anonymous",
        authorPhoto = d["authorPhoto"] as? String ?: "",
        text = d["text"] as? String ?: "",
        mediaUrls = (d["mediaUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        bgColorIndex = (d["bgColorIndex"] as? Number)?.toInt() ?: 0,
        visibility = d["visibility"] as? String ?: "public",
        reactions = (d["reactions"] as? Map<*, *>)?.mapNotNull { (k, v) ->
            if (k is String && v is String) k to v else null
        }?.toMap() ?: emptyMap(),
        commentCount = (d["commentCount"] as? Number)?.toInt() ?: 0,
        tipTotal = (d["tipTotal"] as? Number)?.toDouble() ?: 0.0,
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        editedAt = (d["editedAt"] as? Number)?.toLong()
    )

    private fun commentToMap(c: Comment): Map<String, Any?> = mapOf(
        "id" to c.id,
        "postId" to c.postId,
        "uid" to c.uid,
        "authorName" to c.authorName,
        "authorPhoto" to c.authorPhoto,
        "text" to c.text,
        "parentId" to c.parentId,
        "likes" to c.likes,
        "createdAt" to c.createdAt,
        "editedAt" to c.editedAt
    )

    private fun parseComment(id: String, d: Map<String, Any?>): Comment = Comment(
        id = id,
        postId = d["postId"] as? String ?: "",
        uid = d["uid"] as? String ?: "",
        authorName = d["authorName"] as? String ?: "Anonymous",
        authorPhoto = d["authorPhoto"] as? String ?: "",
        text = d["text"] as? String ?: "",
        parentId = d["parentId"] as? String,
        likes = (d["likes"] as? Map<*, *>)?.mapNotNull { (k, v) ->
            if (k is String && v is Boolean) k to v else null
        }?.toMap() ?: emptyMap(),
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        editedAt = (d["editedAt"] as? Number)?.toLong()
    )

    private fun userToMap(u: User): Map<String, Any?> = mapOf(
        "uid" to u.uid,
        "displayName" to u.displayName,
        "email" to u.email,
        "bio" to u.bio,
        "photoUrl" to u.photoUrl,
        "isAdmin" to u.isAdmin,
        "isSuspended" to u.isSuspended,
        "lastSeen" to u.lastSeen,
        "createdAt" to u.createdAt
    )

    private fun parseUser(uid: String, d: Map<String, Any?>): User = User(
        uid = uid,
        displayName = d["displayName"] as? String ?: "User",
        email = d["email"] as? String ?: "",
        bio = d["bio"] as? String ?: "",
        photoUrl = d["photoUrl"] as? String ?: "",
        isAdmin = d["isAdmin"] as? Boolean ?: false,
        isSuspended = d["isSuspended"] as? Boolean ?: false,
        lastSeen = (d["lastSeen"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    private fun chatMessageToMap(m: ChatMessage): Map<String, Any?> = mapOf(
        "id" to m.id,
        "convoId" to m.convoId,
        "fromUid" to m.fromUid,
        "toUid" to m.toUid,
        "users" to listOf(m.fromUid, m.toUid).filter { it.isNotBlank() },
        "text" to m.text,
        "isCallLog" to m.isCallLog,
        "callType" to m.callType,
        "callStatus" to m.callStatus,
        "callDurationSec" to m.callDurationSec,
        "createdAt" to m.createdAt,
        "editedAt" to m.editedAt
    )

    private fun parseChatMessage(id: String, d: Map<String, Any?>): ChatMessage = ChatMessage(
        id = id,
        convoId = d["convoId"] as? String ?: "",
        fromUid = d["fromUid"] as? String ?: "",
        toUid = d["toUid"] as? String ?: "",
        text = d["text"] as? String ?: "",
        isCallLog = d["isCallLog"] as? Boolean ?: false,
        callType = d["callType"] as? String ?: "audio",
        callStatus = d["callStatus"] as? String ?: "completed",
        callDurationSec = (d["callDurationSec"] as? Number)?.toInt() ?: 0,
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        editedAt = (d["editedAt"] as? Number)?.toLong()
    )

    private fun friendRequestToMap(r: FriendRequest): Map<String, Any?> = mapOf(
        "id" to r.id,
        "fromUid" to r.fromUid,
        "fromName" to r.fromName,
        "fromPhoto" to r.fromPhoto,
        "toUid" to r.toUid,
        "status" to r.status,
        "createdAt" to r.createdAt
    )

    private fun parseFriendRequest(id: String, d: Map<String, Any?>): FriendRequest = FriendRequest(
        id = id,
        fromUid = d["fromUid"] as? String ?: "",
        fromName = d["fromName"] as? String ?: "User",
        fromPhoto = d["fromPhoto"] as? String ?: "",
        toUid = d["toUid"] as? String ?: "",
        status = d["status"] as? String ?: "pending",
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    private fun notificationToMap(n: NotificationItem): Map<String, Any?> = mapOf(
        "id" to n.id,
        "fromUid" to n.fromUid,
        "fromName" to n.fromName,
        "fromPhoto" to n.fromPhoto,
        "toUid" to n.toUid,
        "text" to n.text,
        "type" to n.type,
        "targetId" to n.targetId,
        "reactionType" to n.reactionType,
        "amount" to n.amount,
        "isRead" to n.isRead,
        "createdAt" to n.createdAt
    )

    private fun parseNotification(id: String, d: Map<String, Any?>): NotificationItem = NotificationItem(
        id = id,
        fromUid = d["fromUid"] as? String ?: "",
        fromName = d["fromName"] as? String ?: "User",
        fromPhoto = d["fromPhoto"] as? String ?: "",
        toUid = d["toUid"] as? String ?: "",
        text = d["text"] as? String ?: "",
        type = d["type"] as? String ?: "interaction",
        targetId = d["targetId"] as? String,
        reactionType = d["reactionType"] as? String,
        amount = (d["amount"] as? Number)?.toDouble(),
        isRead = d["isRead"] as? Boolean ?: false,
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    // FIRESTORE: CALLS & SIGNALING
    fun createCall(session: CallSession, onComplete: ((Boolean) -> Unit)? = null) {
        val db = firestore ?: run {
            onComplete?.invoke(false)
            return
        }
        val map = callSessionToMap(session)
        db.collection(COL_CALLS).document(session.callId).set(map)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener {
                Log.e(TAG, "Failed to create call doc: ${it.message}")
                onComplete?.invoke(false)
            }
    }

    fun updateCallStatus(callId: String, status: String, startedAt: Long? = null, endedAt: Long? = null) {
        val db = firestore ?: return
        val updates = mutableMapOf<String, Any>("status" to status)
        if (startedAt != null) updates["startedAt"] = startedAt
        if (endedAt != null) updates["endedAt"] = endedAt
        db.collection(COL_CALLS).document(callId).update(updates)
            .addOnFailureListener { Log.e(TAG, "Failed to update call status: ${it.message}") }
    }

    fun listenToCall(callId: String, onUpdate: (CallSession?) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_CALLS).document(callId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to call $callId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val session = snapshot.data?.let { parseCallSession(snapshot.id, it) }
                        onUpdate(session)
                    } else {
                        onUpdate(null)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach call listener: ${e.message}")
            null
        }
    }

    fun listenToIncomingCalls(userUid: String, onIncomingCall: (CallSession) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COL_CALLS)
                .whereEqualTo("receiverUid", userUid)
                .whereEqualTo("status", "ringing")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to incoming calls: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val d = doc.data ?: continue
                            val session = parseCallSession(doc.id, d)
                            if (System.currentTimeMillis() - session.createdAt < 60000L) {
                                onIncomingCall(session)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach incoming calls listener: ${e.message}")
            null
        }
    }

    private fun callSessionToMap(s: CallSession): Map<String, Any?> = mapOf(
        "callId" to s.callId,
        "callerUid" to s.callerUid,
        "callerName" to s.callerName,
        "callerPhoto" to s.callerPhoto,
        "receiverUid" to s.receiverUid,
        "receiverName" to s.receiverName,
        "receiverPhoto" to s.receiverPhoto,
        "callType" to s.callType,
        "status" to s.status,
        "roomUrl" to s.roomUrl,
        "createdAt" to s.createdAt,
        "startedAt" to s.startedAt,
        "endedAt" to s.endedAt
    )

    private fun parseCallSession(id: String, d: Map<String, Any?>): CallSession = CallSession(
        callId = id,
        callerUid = d["callerUid"] as? String ?: "",
        callerName = d["callerName"] as? String ?: "User",
        callerPhoto = d["callerPhoto"] as? String ?: "",
        receiverUid = d["receiverUid"] as? String ?: "",
        receiverName = d["receiverName"] as? String ?: "User",
        receiverPhoto = d["receiverPhoto"] as? String ?: "",
        callType = d["callType"] as? String ?: "audio",
        status = d["status"] as? String ?: "ringing",
        roomUrl = d["roomUrl"] as? String ?: "",
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        startedAt = (d["startedAt"] as? Number)?.toLong(),
        endedAt = (d["endedAt"] as? Number)?.toLong()
    )
}
