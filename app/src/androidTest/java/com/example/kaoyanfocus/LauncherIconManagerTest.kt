package com.example.kaoyanfocus

import android.content.ComponentName
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconManagerTest {
    @Test fun choosingThemeKeepsActivityResumedAndDefersLauncherAliasUntilBackground() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LauncherIconManager.applyTheme(context, ThemeCatalog.DEFAULT)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            SystemClock.sleep(500) // let ViewModel restore the saved theme first
            LauncherIconManager.rememberTheme(context, ThemeCatalog.STARRY)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                context.packageManager.getComponentEnabledSetting(
                    ComponentName(context.packageName, "com.example.kaoyanfocus.DefaultIconAlias")
                )
            )

            scenario.moveToState(Lifecycle.State.CREATED)
            SystemClock.sleep(300)
            assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                context.packageManager.getComponentEnabledSetting(
                    ComponentName(context.packageName, "com.example.kaoyanfocus.StarryIconAlias")
                )
            )
        }
        LauncherIconManager.applyTheme(context, ThemeCatalog.DEFAULT)
    }

    @Test fun studyTimerDoesNotCreateANotification() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch(MainActivity::class.java).use {
            TimerStore.start(context, 1, "学习", "", ThemeCatalog.DEFAULT)
            SystemClock.sleep(1_200)

            val notifications = context.getSystemService(NotificationManager::class.java).activeNotifications
            assertFalse(notifications.any { it.id == 7 })
            val permissions = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions.orEmpty()
            assertFalse("android.permission.POST_NOTIFICATIONS" in permissions)

            TimerStore.clear(context)
            LauncherIconManager.applyTheme(context, ThemeCatalog.DEFAULT)
        }
    }

}
