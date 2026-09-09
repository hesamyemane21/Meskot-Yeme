package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeskotRepository(private val context: Context? = null) {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    init {
        context?.let { FirebaseManager.initialize(it) }
    }

    // AUTH
    fun login(email: String, pass: String, onSuccess: (User) -> Unit = {}, onFailure: (String) -> Unit = {}) {
        FirebaseManager.signInWithEmail(email, pass, onSuccess = { user ->
            _currentUser.value = user
            onSuccess(user)
        }, onFailure = onFailure)
    }

    fun signup(fullName: String, email: String, pass: String, onSuccess: (User) -> Unit = {}, onFailure: (String) -> Unit = {}) {
        FirebaseManager.signUpWithEmail(fullName, email, pass, onSuccess = { user ->
            _currentUser.value = user
            onSuccess(user)
        }, onFailure = onFailure)
    }

    fun logout() {
        FirebaseManager.signOut()
        _currentUser.value = null
    }

    fun switchUser(userId: String) {
        _users.value.find { it.uid == userId }?.let {
            _currentUser.value = it
        }
    }

    fun updateProfile(displayName: String, bio: String) {
        val curr = _currentUser.value ?: return
        val updated = curr.copy(displayName = displayName, bio = bio)
        _currentUser.value = updated
        FirebaseManager.saveUser(updated)
    }

    // MESSAGING
    fun getMessages(onUpdated: (List<ChatMessage>) -> Unit) {
        FirebaseManager.listenToMessages(onUpdated)
    }

    fun sendMessage(msg: ChatMessage) {
        FirebaseManager.sendMessage(msg)
    }

    fun deleteMessage(messageId: String) {
        // Handled via FirebaseManager if needed
    }

    fun logCall(convoId: String, type: String, status: String, durationSec: Int) {
        val user = _currentUser.value ?: return
        val callMsg = ChatMessage(
            id = "call_" + System.currentTimeMillis(),
            convoId = convoId,
            fromUid = user.uid,
            text = if (type == "video") "Video Call" else "Audio Call",
            isCallLog = true,
            callType = type,
            callStatus = status,
            callDurationSec = durationSec,
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.sendMessage(callMsg)
    }

    // GROUPS & ALBUMS
    fun toggleGroupJoin(groupId: String) {}

    fun createGroup(name: String, description: String) {}

    fun createAlbum(title: String) {}

    fun addPhotoToAlbum(albumId: String, photoUrl: String) {}

    // NOTIFICATIONS & ADMIN
    fun markAllNotificationsRead() {}

    fun adminToggleSuspend(userId: String) {}

    fun adminToggleAdmin(userId: String) {}

    fun adminDeleteGroup(groupId: String) {}
}
