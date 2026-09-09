package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    // Collections
    const val COL_FRIEND_REQUESTS = "friend_requests"
    const val COL_MESSAGES = "messages"

    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val existingApps = FirebaseApp.getApps(context)
            val app = if (existingApps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyD9j6Emef7z1sxtGuqgEePwIJetPo4ZhQQ")
                    .setApplicationId("1:1083041536701:web:4654f20e5ae08a2d0435d4")
                    .setProjectId("meskot-b1a69")
                    .setStorageBucket("meskot-b1a69.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                existingApps.first()
            }

            auth = FirebaseAuth.getInstance(app)
            firestore = FirebaseFirestore.getInstance(app)
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully with project meskot-b1a69")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization fallback (offline/cached mode): ${e.message}")
        }
    }

    fun isConfigured(): Boolean = isInitialized && auth != null && firestore != null

    fun listenToFriendRequests(
        userId: String,
        onRequestsUpdated: (List<FriendRequest>) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return db.collection(COL_FRIEND_REQUESTS)
            .where(
                Filter.or(
                    Filter.equalTo("receiverId", userId),
                    Filter.equalTo("senderId", userId)
                )
            )
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen to friend requests failed", error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onRequestsUpdated(requests)
            }
    }

    fun sendFriendRequest(senderId: String, receiverId: String, onComplete: (Boolean) -> Unit = {}) {
        val db = firestore ?: run { onComplete(false); return }
        val req = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )
        db.collection(COL_FRIEND_REQUESTS)
            .add(req)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun acceptFriendRequest(requestId: String, onComplete: (Boolean) -> Unit = {}) {
        val db = firestore ?: run { onComplete(false); return }
        db.collection(COL_FRIEND_REQUESTS)
            .document(requestId)
            .update("status", "accepted")
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun rejectFriendRequest(requestId: String, onComplete: (Boolean) -> Unit = {}) {
        val db = firestore ?: run { onComplete(false); return }
        db.collection(COL_FRIEND_REQUESTS)
            .document(requestId)
            .update("status", "rejected")
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun listenToMessages(
        convId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return db.collection(COL_MESSAGES)
            .whereEqualTo("convId", convId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen to messages failed", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onMessagesUpdated(messages)
            }
    }

    fun sendMessage(
        convId: String,
        fromUid: String,
        text: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val db = firestore ?: run { onComplete(false); return }
        val msg = mapOf(
            "convId" to convId,
            "fromUid" to fromUid,
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection(COL_MESSAGES)
            .add(msg)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
