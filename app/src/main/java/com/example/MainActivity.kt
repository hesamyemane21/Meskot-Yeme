package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MeskotRepository
import com.example.ui.MeskotViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.CallOverlay
import com.example.ui.components.ComposerDialog
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.IconNavBar
import com.example.ui.components.PostOptionsMenu
import com.example.ui.components.TipModal
import com.example.ui.components.TopNavBar
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GroupDetailScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.theme.MeskotTheme
import com.example.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = MeskotRepository(applicationContext)

        setContent {
            val viewModel = remember { MeskotViewModel(repository) }
            MeskotTheme {
                MeskotApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun MeskotApp(viewModel: MeskotViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val users by viewModel.users.collectAsState()
    val friendsSet by viewModel.friends.collectAsState()
    val incomingReqs by viewModel.incomingRequests.collectAsState()
    val outgoingReqs by viewModel.outgoingRequests.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val savedPostIds by viewModel.savedPostIds.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()

    val unreadNotifsCount by viewModel.unreadNotifsCount.collectAsState()
    val unreadMsgCount by viewModel.unreadMsgCount.collectAsState()

    val viewingUser by viewModel.viewingUser.collectAsState()
    val chattingWithUser by viewModel.chattingWithUser.collectAsState()
    val viewingGroup by viewModel.viewingGroup.collectAsState()
    val viewingAlbum by viewModel.viewingAlbum.collectAsState()

    val isComposerOpen by viewModel.isComposerOpen.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val tippingPost by viewModel.tippingPost.collectAsState()
    val postMenuTarget by viewModel.postMenuTarget.collectAsState()
    val isEditProfileOpen by viewModel.isEditProfileOpen.collectAsState()

    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Android back button handling for sub-screens
    BackHandler(enabled = currentTab != ScreenTab.FEED) {
        when (currentTab) {
            ScreenTab.CHAT -> viewModel.navigateTo(ScreenTab.MESSAGES)
            ScreenTab.GROUP_DETAIL -> viewModel.navigateTo(ScreenTab.GROUPS)
            ScreenTab.ALBUM_DETAIL -> viewModel.navigateTo(ScreenTab.PHOTOS)
            ScreenTab.PROFILE, ScreenTab.SAVED, ScreenTab.ADMIN, ScreenTab.DASHBOARD, ScreenTab.MENU -> viewModel.navigateTo(ScreenTab.FEED)
            else -> viewModel.navigateTo(ScreenTab.FEED)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            currentLanguage = currentLanguage
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopNavBar(
                        currentUser = currentUser,
                        currentLanguage = currentLanguage,
                        onToggleLanguage = { viewModel.toggleLanguage() },
                        onOpenComposer = { viewModel.openComposer() },
                        onOpenSearch = { viewModel.navigateTo(ScreenTab.FRIENDS) },
                        onOpenMenu = { viewModel.navigateTo(ScreenTab.MENU) },
                        onProfileClick = { currentUser?.let { viewModel.openProfile(it) } },
                        onLogout = { viewModel.logout() }
                    )

                    // Web App's exact Icon Nav Bar with Badges
                    IconNavBar(
                        currentTab = currentTab,
                        unreadReqCount = incomingReqs.size,
                        unreadMsgCount = unreadMsgCount,
                        unreadNotifCount = unreadNotifsCount,
                        isAdmin = currentUser?.isAdmin == true,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Paper)
            ) {
                // Navigation Screen Router
                when (currentTab) {
                    ScreenTab.FEED -> {
                        val friendsList = users.filter { friendsSet.contains(it.uid) }
                        FeedScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            feedPosts = feedPosts,
                            friends = friendsList,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.FRIENDS -> {
                        FriendsScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            allUsers = users,
                            friendUids = friendsSet,
                            incomingRequests = incomingReqs,
                            outgoingRequests = outgoingReqs,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.MESSAGES -> {
                        MessagesScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            allUsers = users,
                            friendUids = friendsSet,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.CHAT -> {
                        chattingWithUser?.let { other ->
                            ChatScreen(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                recipient = other,
                                currentLanguage = currentLanguage
                            )
                        } ?: viewModel.navigateTo(ScreenTab.MESSAGES)
                    }

                    ScreenTab.GROUPS -> {
                        GroupsScreen(
                            viewModel = viewModel,
                            groups = groups,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.GROUP_DETAIL -> {
                        viewingGroup?.let { group ->
                            GroupDetailScreen(
                                viewModel = viewModel,
                                group = group,
                                currentUser = currentUser,
                                currentLanguage = currentLanguage
                            )
                        } ?: viewModel.navigateTo(ScreenTab.GROUPS)
                    }

                    ScreenTab.PHOTOS -> {
                        PhotosScreen(
                            viewModel = viewModel,
                            albums = albums,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.ALBUM_DETAIL -> {
                        viewingAlbum?.let { album ->
                            AlbumDetailScreen(
                                viewModel = viewModel,
                                album = album,
                                currentLanguage = currentLanguage
                            )
                        } ?: viewModel.navigateTo(ScreenTab.PHOTOS)
                    }

                    ScreenTab.NOTIFICATIONS -> {
                        NotificationsScreen(
                            viewModel = viewModel,
                            notifications = notifications,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.DASHBOARD -> {
                        val myPosts = feedPosts.filter { it.uid == currentUser?.uid }
                        DashboardScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            myPosts = myPosts,
                            friendsCount = friendsSet.size,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.ADMIN -> {
                        AdminScreen(
                            viewModel = viewModel,
                            users = users,
                            posts = feedPosts,
                            groups = groups,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.SAVED -> {
                        val savedList = feedPosts.filter { savedPostIds.contains(it.id) }
                        SavedScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            savedPosts = savedList,
                            currentLanguage = currentLanguage
                        )
                    }

                    ScreenTab.PROFILE -> {
                        val targetUser = viewingUser ?: currentUser
                        if (targetUser != null) {
                            val userPosts = feedPosts.filter { it.uid == targetUser.uid }
                            ProfileScreen(
                                viewModel = viewModel,
                                user = targetUser,
                                currentUser = currentUser,
                                userPosts = userPosts,
                                friendUids = friendsSet,
                                currentLanguage = currentLanguage
                            )
                        } else {
                            viewModel.navigateTo(ScreenTab.FEED)
                        }
                    }

                    ScreenTab.MENU -> {
                        MenuScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            allUsers = users,
                            currentLanguage = currentLanguage
                        )
                    }
                }

                // Global Modals & Overlays
                if (isComposerOpen && currentUser != null) {
                    ComposerDialog(
                        currentUser = currentUser!!,
                        currentLanguage = currentLanguage,
                        onDismiss = { viewModel.closeComposer() },
                        onSubmit = { text, media, bgIdx, vis ->
                            viewModel.submitPost(text, media, bgIdx, vis)
                        }
                    )
                }

                postMenuTarget?.let { post ->
                    PostOptionsMenu(
                        post = post,
                        isAuthor = post.uid == currentUser?.uid,
                        isAdmin = currentUser?.isAdmin == true,
                        isSaved = savedPostIds.contains(post.id),
                        currentLanguage = currentLanguage,
                        onDismiss = { viewModel.closePostMenu() },
                        onSaveToggle = { viewModel.toggleSavePost(post.id) },
                        onShare = { viewModel.sharePost(post.id) },
                        onEdit = { viewModel.startEditingPost(post) },
                        onDelete = { viewModel.deletePost(post.id) },
                        onHide = { viewModel.hidePost(post.id) },
                        onReport = { viewModel.reportPost(post.id) },
                        onInterested = { viewModel.showMessage("We'll tune your feed for more posts like this.") },
                        onNotInterested = { viewModel.showMessage("We'll show fewer posts like this.") },
                        onToggleNotifs = { viewModel.showMessage("Notifications updated for this post.") },
                        onCopyLink = { viewModel.showMessage("Post link copied to clipboard!") }
                    )
                }

                tippingPost?.let { post ->
                    TipModal(
                        post = post,
                        currentLanguage = currentLanguage,
                        onDismiss = { viewModel.closeTipModal() },
                        onConfirmTip = { amount -> viewModel.confirmTip(amount) }
                    )
                }

                if (isEditProfileOpen && currentUser != null) {
                    EditProfileDialog(
                        currentUser = currentUser!!,
                        currentLanguage = currentLanguage,
                        onDismiss = { viewModel.closeEditProfile() },
                        onSave = { name, bio, photoUrl ->
                            viewModel.saveProfile(name, bio, photoUrl)
                        }
                    )
                }

                // Active Audio / Video Call Overlay
                activeCall?.let { call ->
                    CallOverlay(
                        activeCall = call,
                        currentLanguage = currentLanguage,
                        onToggleMute = { viewModel.toggleCallMute() },
                        onToggleCamera = { viewModel.toggleCallCamera() },
                        onEndCall = { viewModel.endCall() }
                    )
                }
            }
        }
    }
}
