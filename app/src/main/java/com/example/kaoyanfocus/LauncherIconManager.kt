package com.example.kaoyanfocus

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherIconManager {
    private const val DEFAULT_ALIAS = "com.example.kaoyanfocus.DefaultIconAlias"
    private const val CINNAMOROLL_ALIAS = "com.example.kaoyanfocus.CinnamorollIconAlias"
    private const val STARRY_ALIAS = "com.example.kaoyanfocus.StarryIconAlias"
    private const val PREFS = "launcher_theme"
    private const val ACTIVE_THEME = "active_theme"

    fun applyTheme(context: Context, themeKey: String) {
        val packageManager = context.packageManager
        val desired = when (themeKey) {
            ThemeCatalog.CINNAMOROLL -> CINNAMOROLL_ALIAS
            ThemeCatalog.STARRY -> STARRY_ALIAS
            else -> DEFAULT_ALIAS
        }
        // Keep a synchronous copy so the launcher icon can be applied after
        // the visible activity has naturally moved to the background.
        rememberTheme(context, themeKey)

        // Always enable the new launcher entry before disabling the old one.
        // Some launchers return the running task to Home if there is a brief
        // moment where no launcher component is enabled.
        setEnabled(packageManager, context, desired, true)
        listOf(DEFAULT_ALIAS, CINNAMOROLL_ALIAS, STARRY_ALIAS)
            .filterNot { it == desired }
            .forEach { setEnabled(packageManager, context, it, false) }
    }

    /** Saves the visual theme without touching launcher components. */
    fun rememberTheme(context: Context, themeKey: String) {
        // commit() is intentional because the alias is applied after onStop.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(ACTIVE_THEME, themeKey).commit()
    }

    /** Applies the icon only after the activity has naturally gone to background. */
    fun applyRememberedTheme(context: Context) = applyTheme(context, currentTheme(context))

    fun currentTheme(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ACTIVE_THEME, ThemeCatalog.DEFAULT) ?: ThemeCatalog.DEFAULT

    private fun setEnabled(packageManager: PackageManager, context: Context, className: String, enabled: Boolean) {
        val desired = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val component = ComponentName(context.packageName, className)
        if (packageManager.getComponentEnabledSetting(component) != desired) {
            packageManager.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
        }
    }
}
