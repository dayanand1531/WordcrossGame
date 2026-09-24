package com.dayanand.wordscapes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationAndGameplayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testHomeScreenDisplaysPlayButton() {
        composeTestRule.onNodeWithText("PLAY NOW", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("LEVELS", substring = true).assertIsDisplayed()
    }

    @Test
    fun testNavigateToLevelSelectAndBack() {
        // Click LEVELS on Home
        composeTestRule.onNodeWithText("LEVELS", substring = true).performClick()

        // Verify Level Select screen title is displayed
        composeTestRule.onNodeWithText("SELECT LEVEL").assertIsDisplayed()

        // Click Back button
        composeTestRule.onNodeWithText("◄").performClick()

        // Verify back on Home
        composeTestRule.onNodeWithText("PLAY NOW", substring = true).assertIsDisplayed()
    }

    @Test
    fun testNavigateToGameplayAndPause() {
        // Click PLAY NOW
        composeTestRule.onNodeWithText("PLAY NOW", substring = true).performClick()

        // Verify Level 1 gameplay top bar is displayed
        composeTestRule.onNodeWithText("LEVEL 1").assertIsDisplayed()

        // Click Pause button
        composeTestRule.onNodeWithText("❚❚").performClick()

        // Verify Pause overlay is displayed
        composeTestRule.onNodeWithText("PAUSED").assertIsDisplayed()

        // Click RESUME
        composeTestRule.onNodeWithText("RESUME").performClick()

        // Verify back in Gameplay
        composeTestRule.onNodeWithText("LEVEL 1").assertIsDisplayed()
    }
}
