package com.ispgr5.locationsimulator.screenshots

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.ispgr5.locationsimulator.di.AppModule
import com.ispgr5.locationsimulator.ui.theme.LocationSimulatorTheme
import com.ispgr5.locationsimulator.ui.theme.ThemeState
import com.ispgr5.locationsimulator.ui.theme.ThemeType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.locale.LocaleUtil

@HiltAndroidTest
@UninstallModules(AppModule::class)
class ScreenshotTests {

    @get:Rule(order = 0)
    val hiltAndroidRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Rule(order = 2)
    @JvmField
    val localeTestRule = LocaleTestRule()

    @Before
    fun init() {
        hiltAndroidRule.inject()
        Screengrab.setDefaultScreenshotStrategy(
            UiAutomatorScreenshotStrategy()
        )
    }

    private fun screenshot(
        screenshotName: String,
        content: @Composable ScreenshotScope.() -> Unit
    ) {
        composeTestRule.setContent {
            val themeState by remember {
                mutableStateOf(ThemeState(ThemeType.LIGHT))
            }
            val screenshotScope by remember {
                mutableStateOf(ScreenshotScope(screenshotName, themeState, LocaleUtil.getTestLocale()))
            }
            LocationSimulatorTheme(themeState = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    screenshotScope.content()
                }
            }
        }
        Thread.sleep(2000L)
        Screengrab.screenshot(screenshotName)
        Log.i("Screenshot", "took screenshot $screenshotName")
    }

    @Test
    fun homeScreen() {
        screenshot("home_screen") {
            HomeScreenScreenshot()
        }
    }

    @Test
    fun infoScreen() {
        screenshot("info_screen") {
            InfoScreenScreenshot()
        }
    }

    @Test
    fun selectScreenNormal() {
        screenshot("select_screen_normal") {
            SelectScreenNormalScreenshot()
        }
    }

    @Test
    fun selectScreenDelete() {
        screenshot("select_screen_delete") {
            SelectScreenDeleteModeScreenshot()
        }
    }

    @Test
    fun addScreen() {
        screenshot("add_screen") {
            AddScreenScreenshot()
        }
    }

    @Test
    fun settingsScreenVibrationLight() {
        screenshot("settings_screen_vibration") {
            SettingsScreenVibrationScreenshot()
        }
    }

    @Test
    fun settingsScreenSoundLight() {
        screenshot("settings_screen_sound") {
            SettingsScreenSoundScreenshot()
        }
    }

    @Test
    fun delayScreenLight() {
        screenshot("delay_screen") {
            // TODO:
            Placeholder()
        }
    }

    @Test
    fun runScreenLight() {
        screenshot("run_screen") {
            // TODO:
            Placeholder()
        }
    }

    @Test
    fun editTimelineScreen() {
        screenshot("edit_timeline_screen") {
            // TODO:
            Placeholder()
        }
    }

    @Test
    fun soundScreenLight() {
        screenshot("sound_screen") {
            // TODO:
            Placeholder()
        }
    }
}
