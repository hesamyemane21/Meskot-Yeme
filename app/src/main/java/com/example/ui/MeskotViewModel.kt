package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlbumItem
import com.example.data.AppLanguage
import com.example.data.ChatMessage
import com.example.data.Comment
import com.example.data.GroupItem
import com.example.data.MeskotRepository
import com.example.data.NotificationItem
import com.example.data.Post
import com.example.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    FEED,
    FRIENDS,
    MESSAGES,
    GROUPS,
    PHOTOS,
    NOTIFICATIONS,
    DASHBOARD,
    MENU,
    SAVED,
    ADMIN,
    PROFILE,
    CHAT,
    GROUP_DETAIL,
    ALBUM_DETAIL
}

data class ActiveCall(
    val otherUser: User,
    val callType: String, // "audio" or "video"
    val isRinging: Boolean = true,
    val durationSec: Int = 0,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false
)

class MeskotViewModel(private val repository: MeskotRepository) : ViewModel() {

    val currentLanguage: StateFlow<AppLanguage> = repository.currentLanguage
    val currentUser: StateFlow<User?> = repository.currentUser
    val users: StateFlow<List<User>> = repository.users
    val friends: StateFlow<Set<String>> = repository.friends
    val incomingRequests: StateFlow<List<User>> = repository.incomingRequests
    val outgoingRequests: StateFlow<Set<String>> = repository.outgoingRequests
    val groups: StateFlow<List<GroupItem>> = repository.groups
    val albums: StateFlow<List<AlbumItem>> = repository.albums
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val conversations: StateFlow<Map<String, List<ChatMessage>>> = repository.conversations
    val savedPostIds: StateFlow<Set<String>> = repository.savedPostIds

    // Current screen navigation
    private val _currentTab = MutableStateFlow(ScreenTab.FEED)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Navigation sub-targets
    private val _viewingUser = MutableStateFlow<User?>(null)
    val viewingUser: StateFlow<User?> = _viewingUser.asStateFlow()

    private val _chattingWithUser = MutableStateFlow<User?>(null)
    val chattingWithUser: StateFlow<User?> = _chattingWithUser.asStateFlow()

    private val _viewingGroup = MutableStateFlow<GroupItem?>(null)
    val viewingGroup: StateFlow<GroupItem?> = _viewingGroup.asStateFlow()

    private val _viewingAlbum = MutableStateFlow<AlbumItem?>(null)
    val viewingAlbum: StateFlow<AlbumItem?> = _viewingAlbum.asStateFlow()

    // Modals & Overlays
    private val _isComposerOpen = MutableStateFlow(false)
    val isComposerOpen: StateFlow<Boolean> = _isComposerOpen.asStateFlow()

    private val _composerGroupId = MutableStateFlow<String?>(null)
    val composerGroupId: StateFlow<String?> = _composerGroupId.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall: StateFlow<ActiveCall?> = _activeCall.asStateFlow()

    private val _tippingPost = MutableStateFlow<Post?>(null)
    val tippingPost: StateFlow<Post?> = _tippingPost.asStateFlow()

    private val _postMenuTarget = MutableStateFlow<Post?>(null)
    val postMenuTarget: StateFlow<Post?> = _postMenuTarget.asStateFlow()

    private val _editingPost = MutableStateFlow<Post?>(null)
    val editingPost: StateFlow<Post?> = _editingPost.asStateFlow()

    private val _isEditProfileOpen = MutableStateFlow(false)
    val isEditProfileOpen: StateFlow<Boolean> = _isEditProfileOpen.asStateFlow()

    // Toast message for user feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Filtered Feed posts
    val feedPosts: StateFlow<List<Post>> = combine(
        repository.posts,
        repository.hiddenPostIds,
        repository.currentUser,
        repository.friends
    ) { posts, hidden, user, friendsSet ->
        posts.filter { post ->
            if (hidden.contains(post.id)) return@filter false
            when (post.visibility) {
                "public" -> true
                "friends" -> post.uid == user?.uid || friendsSet.contains(post.uid)
                "onlyme" -> post.uid == user?.uid
                else -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifsCount: StateFlow<Int> = combine(repository.notifications) { notifs ->
        notifs[0].count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadMsgCount: StateFlow<Int> = MutableStateFlow(1)

    // Navigation
    fun navigateTo(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun openProfile(user: User) {
        _viewingUser.value = user
        _currentTab.value = ScreenTab.PROFILE
    }

    fun openProfileByUid(uid: String) {
        val user = repository.users.value.find { it.uid == uid } ?: return
        openProfile(user)
    }

    fun openChat(user: User) {
        _chattingWithUser.value = user
        _currentTab.value = ScreenTab.CHAT
    }

    fun openGroupDetail(group: GroupItem) {
        _viewingGroup.value = group
        _currentTab.value = ScreenTab.GROUP_DETAIL
    }

    fun openAlbumDetail(album: AlbumItem) {
        _viewingAlbum.value = album
        _currentTab.value = ScreenTab.ALBUM_DETAIL
    }

    // Composer
    fun openComposer(groupId: String? = null) {
        _composerGroupId.value = groupId
        _isComposerOpen.value = true
    }

    fun closeComposer() {
        _isComposerOpen.value = false
        _composerGroupId.value = null
    }

    fun submitPost(text: String, mediaUrls: List<String>, bgColorIndex: Int, visibility: String) {
        val gid = _composerGroupId.value
        if (gid != null) {
            repository.createGroupPost(gid, text, mediaUrls)
        } else {
            repository.createPost(text, mediaUrls, bgColorIndex, visibility)
        }
        closeComposer()
        showMessage("Posted successfully")
    }

    // Post Actions
    fun toggleReaction(postId: String, reactionType: String) {
        repository.toggleReaction(postId, reactionType)
    }

    fun sharePost(postId: String) {
        repository.sharePost(postId)
        showMessage("Post shared to your feed")
    }

    fun toggleSavePost(postId: String) {
        val wasSaved = repository.savedPostIds.value.contains(postId)
        repository.toggleSavePost(postId)
        showMessage(if (wasSaved) "Removed from Saved" else "Saved to your bookmarks")
    }

    fun hidePost(postId: String) {
        repository.hidePost(postId)
        showMessage("Post hidden")
    }

    fun reportPost(postId: String) {
        showMessage("Thank you. Post reported for review.")
    }

    fun openPostMenu(post: Post) {
        _postMenuTarget.value = post
    }

    fun closePostMenu() {
        _postMenuTarget.value = null
    }

    fun startEditingPost(post: Post) {
        _editingPost.value = post
        closePostMenu()
    }

    fun cancelEditingPost() {
        _editingPost.value = null
    }

    fun savePostEdit(postId: String, newText: String) {
        repository.editPost(postId, newText)
        _editingPost.value = null
        showMessage("Post updated")
    }

    fun deletePost(postId: String) {
        repository.deletePost(postId)
        closePostMenu()
        showMessage("Post deleted")
    }

    // Comments
    fun getComments(postId: String): List<Comment> {
        return repository.getCommentsForPost(postId)
    }

    fun addComment(postId: String, text: String, parentId: String? = null) {
        if (text.isNotBlank()) {
            repository.addComment(postId, text.trim(), parentId)
        }
    }

    fun toggleCommentLike(postId: String, commentId: String) {
        repository.toggleCommentLike(postId, commentId)
    }

    fun deleteComment(postId: String, commentId: String) {
        repository.deleteComment(postId, commentId)
    }

    // Tipping
    fun openTipModal(post: Post) {
        _tippingPost.value = post
    }

    fun closeTipModal() {
        _tippingPost.value = null
    }

    fun confirmTip(amount: Double) {
        val post = _tippingPost.value ?: return
        repository.sendTip(post.id, amount)
        closeTipModal()
        showMessage("Tip of ${amount.toInt()} ETB sent via Chapa!")
    }

    // Friends
    fun sendFriendRequest(user: User) {
        repository.sendFriendRequest(user.uid)
        showMessage("Friend request sent to ${user.displayName}")
    }

    fun cancelFriendRequest(user: User) {
        repository.cancelFriendRequest(user.uid)
    }

    fun acceptFriendRequest(user: User) {
        repository.acceptFriendRequest(user.uid)
        showMessage("Accepted ${user.displayName}'s request")
    }

    fun declineFriendRequest(user: User) {
        repository.declineFriendRequest(user.uid)
    }

    fun unfriend(user: User) {
        repository.unfriend(user.uid)
        showMessage("Removed from friends")
    }

    // Messages
    fun getMessagesForUser(otherUid: String): List<ChatMessage> {
        return repository.getMessages(otherUid)
    }

    fun sendMessage(otherUid: String, text: String) {
        if (text.isNotBlank()) {
            repository.sendMessage(otherUid, text.trim())
        }
    }

    fun deleteMessage(otherUid: String, msgId: String) {
        repository.deleteMessage(otherUid, msgId)
    }

    // Audio / Video Calls
    fun startCall(otherUser: User, callType: String) {
        _activeCall.value = ActiveCall(
            otherUser = otherUser,
            callType = callType,
            isRinging = true
        )
        viewModelScope.launch {
            delay(2500) // Realistic ringing duration
            if (_activeCall.value != null) {
                _activeCall.value = _activeCall.value?.copy(isRinging = false)
                startCallTimer()
            }
        }
    }

    private fun startCallTimer() {
        viewModelScope.launch {
            while (_activeCall.value != null && !_activeCall.value!!.isRinging) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let { it.copy(durationSec = it.durationSec + 1) }
            }
        }
    }

    fun toggleCallMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallCamera() {
        _activeCall.value = _activeCall.value?.let { it.copy(isCameraOff = !it.isCameraOff) }
    }

    fun endCall() {
        val call = _activeCall.value
        if (call != null) {
            repository.logCall(
                otherUid = call.otherUser.uid,
                callType = call.callType,
                callStatus = if (call.durationSec > 0) "completed" else "missed",
                durationSec = call.durationSec
            )
        }
        _activeCall.value = null
    }

    // Groups
    fun toggleGroupJoin(group: GroupItem) {
        repository.toggleGroupJoin(group.id)
    }

    fun createGroup(name: String, desc: String) {
        if (name.isNotBlank()) {
            repository.createGroup(name.trim(), desc.trim())
            showMessage("Group created")
        }
    }

    fun getGroupPosts(groupId: String): List<Post> {
        return repository.groupPosts.value[groupId] ?: emptyList()
    }

    // Albums
    fun createAlbum(title: String) {
        if (title.isNotBlank()) {
            repository.createAlbum(title.trim())
            showMessage("Album created")
        }
    }

    fun addPhotoToAlbum(albumId: String, photoUrl: String) {
        repository.addPhotoToAlbum(albumId, photoUrl)
        showMessage("Photo added to album")
    }

    // Profile Edit
    fun openEditProfile() {
        _isEditProfileOpen.value = true
    }

    fun closeEditProfile() {
        _isEditProfileOpen.value = false
    }

    fun saveProfile(name: String, bio: String, photoUrl: String) {
        repository.updateProfile(name, bio, photoUrl)
        closeEditProfile()
        showMessage("Profile updated")
    }

    // Language
    fun toggleLanguage() {
        val next = if (currentLanguage.value == AppLanguage.EN) AppLanguage.AM else AppLanguage.EN
        repository.setLanguage(next)
    }

    // Notifications
    fun markAllNotifsRead() {
        repository.markAllNotificationsRead()
    }

    // Admin
    fun adminToggleSuspend(uid: String) {
        repository.adminToggleSuspend(uid)
        showMessage("Account status updated")
    }

    fun adminToggleAdmin(uid: String) {
        repository.adminToggleAdmin(uid)
        showMessage("Admin privileges updated")
    }

    fun adminDeleteGroup(groupId: String) {
        repository.adminDeleteGroup(groupId)
        showMessage("Group deleted")
    }

    // Auth
    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        repository.login(email, pass) { ok, err ->
            if (ok) showMessage("Welcome back to Meskot!")
            onResult(ok, err)
        }
    }

    fun signup(fullName: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        repository.signup(fullName, email, pass) { ok, err ->
            if (ok) showMessage("Welcome to Meskot!")
            onResult(ok, err)
        }
    }

    fun login(email: String, pass: String): Boolean {
        return repository.login(email, pass)
    }

    fun signup(fullName: String, email: String, pass: String): Boolean {
        return repository.signup(fullName, email, pass)
    }

    fun logout() {
        repository.logout()
        _currentTab.value = ScreenTab.FEED
        showMessage("Logged out")
    }

    fun switchUser(user: User) {
        repository.switchUser(user)
    }
}
