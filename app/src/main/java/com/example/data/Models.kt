package com.example.data

data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val bio: String = "",
    val photoUrl: String = "",
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class SharedPostPreview(
    val postId: String,
    val authorName: String,
    val authorPhoto: String,
    val text: String,
    val mediaUrls: List<String> = emptyList(),
    val createdAt: Long
)

data class Post(
    val id: String,
    val uid: String,
    val authorName: String,
    val authorPhoto: String = "",
    val text: String = "",
    val mediaUrls: List<String> = emptyList(),
    val bgColorIndex: Int = 0, // 0: Normal card, 1..5: Gradient backgrounds
    val visibility: String = "public", // "public", "friends", "onlyme"
    val reactions: Map<String, String> = emptyMap(), // uid -> "like", "love", "haha", "wow", "sad", "angry"
    val commentCount: Int = 0,
    val tipTotal: Double = 0.0,
    val sharedPost: SharedPostPreview? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val isSaved: Boolean = false
)

data class Comment(
    val id: String,
    val postId: String,
    val uid: String,
    val authorName: String,
    val authorPhoto: String = "",
    val text: String,
    val parentId: String? = null,
    val likes: Map<String, Boolean> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null
)

data class GroupItem(
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val memberCount: Int = 1,
    val isJoined: Boolean = false,
    val coverColorHex: String = "#B8863A"
)

data class AlbumItem(
    val id: String,
    val uid: String,
    val title: String,
    val count: Int = 0,
    val coverUrl: String = "",
    val photos: List<String> = emptyList()
)

data class ConversationItem(
    val convoId: String,
    val otherUser: User,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val convoId: String,
    val fromUid: String,
    val text: String,
    val isCallLog: Boolean = false,
    val callType: String = "audio", // "audio", "video"
    val callStatus: String = "completed", // "missed", "completed"
    val callDurationSec: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null
)

data class NotificationItem(
    val id: String,
    val fromUid: String,
    val fromName: String,
    val fromPhoto: String = "",
    val text: String = "",
    val type: String, // "like", "comment", "friend_request", "friend_accept", "message", "share", "tip", "reaction"
    val targetId: String? = null,
    val reactionType: String? = null,
    val amount: Double? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReactionType(val code: String, val emoji: String, val labelKey: String) {
    LIKE("like", "👍", "reactLike"),
    LOVE("love", "❤️", "reactLove"),
    HAHA("haha", "😆", "reactHaha"),
    WOW("wow", "😮", "reactWow"),
    SAD("sad", "😢", "reactSad"),
    ANGRY("angry", "😡", "reactAngry")
}
