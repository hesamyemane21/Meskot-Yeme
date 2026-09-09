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
    const val COL_FRIEND_REQUESTS = "friendRequests"

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
                        if (users.isNotEmpty()) {
                            onUsersUpdated(users)
                        }
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
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseChatMessage(doc.id, it) }
                        }
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

    fun sendMessage(msg: ChatMessage) {
        val db = firestore ?: return
        val map = chatMessageToMap(msg)
        db.collection(COL_MESSAGES).document(msg.id).set(map, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Failed to send message: ${it.message}") }
    }

    // FIRESTORE: FRIEND REQUESTS
    fun sendFriendRequest(fromUid: String, toUid: String) {
        val db = firestore ?: return
        val id = "${fromUid}_$toUid"
        val map = mapOf(
            "id" to id, "fromUid" to fromUid, "toUid" to toUid,
            "status" to "pending", "createdAt" to System.currentTimeMillis()
        )
        db.collection(COL_FRIEND_REQUESTS).document(id).set(map, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Failed to send friend request: ${it.message}") }
    }

    fun respondToFriendRequest(fromUid: String, toUid: String, accept: Boolean) {
        val db = firestore ?: return
        db.collection(COL_FRIEND_REQUESTS).document("${fromUid}_$toUid")
            .update("status", if (accept) "accepted" else "declined")
            .addOnFailureListener { Log.e(TAG, "Failed to respond to friend request: ${it.message}") }
    }

    fun listenToFriendRequests(onUpdated: (List<Map<String, Any?>>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return db.collection(COL_FRIEND_REQUESTS).addSnapshotListener { snapshot, error ->
            if (error != null) { Log.e(TAG, "Error listening to friend requests: ${error.message}"); return@addSnapshotListener }
            snapshot?.let { onUpdated(it.documents.mapNotNull { d -> d.data }) }
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
        text = d["text"] as? String ?: "",
        isCallLog = d["isCallLog"] as? Boolean ?: false,
        callType = d["callType"] as? String ?: "audio",
        callStatus = d["callStatus"] as? String ?: "completed",
        callDurationSec = (d["callDurationSec"] as? Number)?.toInt() ?: 0,
        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        editedAt = (d["editedAt"] as? Number)?.toLong()
    )
}
