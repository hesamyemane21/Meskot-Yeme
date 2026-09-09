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

    private val _groupPosts = MutableStateFlow<Map<String, List<Post>>>(emptyMap())
    val groupPosts: StateFlow<Map<String, List<Post>>> = _groupPosts.asStateFlow()

    init {
        context?.let { FirebaseManager.initialize(it) }
    }

    // LANGUAGE
    fun setLanguage(lang: Any) {}

    // AUTH
    fun login(email: String, pass: String): Boolean {
        FirebaseManager.signInWithEmail(email, pass, onSuccess = { user ->
            _currentUser.value = user
        }, onFailure = {})
        return true
    }

    fun signup(fullName: String, email: String, pass: String): Boolean {
        FirebaseManager.signUpWithEmail(fullName, email, pass, onSuccess = { user ->
            _currentUser.value = user
        }, onFailure = {})
        return true
    }

    fun logout() {
        FirebaseManager.signOut()
        _currentUser.value = null
    }

    fun switchUser(user: User) {
        _currentUser.value = user
    }

    fun switchUser(userId: String) {
        _users.value.find { it.uid == userId }?.let {
            _currentUser.value = it
        }
    }

    fun updateProfile(displayName: String, bio: String, photoUrl: String = "") {
        val curr = _currentUser.value ?: return
        val updated = curr.copy(
            displayName = displayName.ifBlank { curr.displayName },
            bio = bio,
            photoUrl = photoUrl.ifBlank { curr.photoUrl }
        )
        _currentUser.value = updated
        FirebaseManager.saveUser(updated)
    }

    // MESSAGING
    fun getMessages(otherUid: String): List<ChatMessage> = emptyList()

    fun getMessages(onUpdated: (List<ChatMessage>) -> Unit) {
        FirebaseManager.listenToMessages(onUpdated)
    }

    fun sendMessage(otherUid: String, text: String) {
        val user = _currentUser.value ?: return
        val msg = ChatMessage(
            id = "msg_" + System.currentTimeMillis(),
            convoId = otherUid,
            fromUid = user.uid,
            text = text,
            createdAt = System.currentTimeMillis()
        )
        FirebaseManager.sendMessage(msg)
    }

    fun sendMessage(msg: ChatMessage) {
        FirebaseManager.sendMessage(msg)
    }

    fun deleteMessage(otherUid: String, messageId: String) {}

    fun deleteMessage(messageId: String) {}

    fun logCall(otherUid: String, type: String, status: String, durationSec: Int) {
        val user = _currentUser.value ?: return
        val callMsg = ChatMessage(
            id = "call_" + System.currentTimeMillis(),
            convoId = otherUid,
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
