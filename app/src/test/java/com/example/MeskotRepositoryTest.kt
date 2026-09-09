package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppLanguage
import com.example.data.MeskotRepository
import com.example.data.MeskotStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeskotRepositoryTest {

    @Test
    fun testBilingualStrings() {
        assertEquals("Meskot", MeskotStrings.get("appName", AppLanguage.EN))
        assertEquals("መስኮት", MeskotStrings.get("appName", AppLanguage.AM))
        assertEquals("News Feed", MeskotStrings.get("navFeed", AppLanguage.EN))
        assertEquals("ዜና ምግብ", MeskotStrings.get("navFeed", AppLanguage.AM))
    }

    @Test
    fun testPostCreationAndReactions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MeskotRepository(context)
        val testUser = com.example.data.User(
            uid = "test_user_1",
            displayName = "Test User",
            email = "test@meskot.et"
        )
        repo.switchUser(testUser)

        val initialCount = repo.posts.value.size
        repo.createPost(
            text = "ሰላም ለሁላችሁ! Welcome to Meskot Android app",
            mediaUrls = emptyList(),
            bgColorIndex = 0,
            visibility = "public"
        )

        val newPosts = repo.posts.value
        assertEquals(initialCount + 1, newPosts.size)
        val created = newPosts.first()
        assertEquals("ሰላም ለሁላችሁ! Welcome to Meskot Android app", created.text)

        // Toggle reaction
        repo.toggleReaction(created.id, "love")
        val reactedPost = repo.posts.value.find { it.id == created.id }
        assertNotNull(reactedPost)
        assertEquals("love", reactedPost!!.reactions[repo.currentUser.value!!.uid])

        // Add tip via Chapa ETB
        repo.sendTip(created.id, 50.0)
        val tippedPost = repo.posts.value.find { it.id == created.id }
        assertEquals(50.0, tippedPost!!.tipTotal, 0.01)
    }

    @Test
    fun testComments() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MeskotRepository(context)
        val testUser = com.example.data.User(
            uid = "test_user_1",
            displayName = "Test User",
            email = "test@meskot.et"
        )
        repo.switchUser(testUser)
        repo.createPost(text = "Hello Meskot!")
        val firstPost = repo.posts.value.first()

        repo.addComment(firstPost.id, "Great post!")
        val comments = repo.getCommentsForPost(firstPost.id)
        assertTrue(comments.any { it.text == "Great post!" })
    }
}
