package com.coverRoot.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central settings repository for the app.
 *
 * Persists [AppPreferences] into shared preferences and exposes them as a
 * [StateFlow] so UI can react to changes. The same shared-preferences file
 * ("settings_prefs") is also used by [LocaleHelper], so the persisted language
 * stays consistent across app restarts.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun load(): AppPreferences = try {
        AppPreferences(
            colorMode = prefs.getInt(KEY_COLOR_MODE, AppPreferences().colorMode),
            uiMode = prefs.getString(KEY_UI_MODE, AppPreferences().uiMode) ?: AppPreferences().uiMode,
            language = prefs.getString(KEY_LANGUAGE, AppPreferences().language) ?: AppPreferences().language,
            keyColor = prefs.getInt(KEY_KEY_COLOR, AppPreferences().keyColor),
            colorStyle = prefs.getString(KEY_COLOR_STYLE, AppPreferences().colorStyle) ?: AppPreferences().colorStyle,
            colorSpec = prefs.getString(KEY_COLOR_SPEC, AppPreferences().colorSpec) ?: AppPreferences().colorSpec,
            miuixMonet = prefs.getBoolean(KEY_MIUIX_MONET, AppPreferences().miuixMonet),
            enableBlur = prefs.getBoolean(KEY_ENABLE_BLUR, AppPreferences().enableBlur),
            enableFloatingBottomBar = prefs.getBoolean(KEY_ENABLE_FLOATING_BOTTOM_BAR, AppPreferences().enableFloatingBottomBar),
            enableFloatingBottomBarBlur = prefs.getBoolean(KEY_ENABLE_FLOATING_BOTTOM_BAR_BLUR, AppPreferences().enableFloatingBottomBarBlur),
            enableScrollAnimation = prefs.getBoolean(KEY_ENABLE_SCROLL_ANIMATION, AppPreferences().enableScrollAnimation),
            enablePredictiveBack = prefs.getBoolean(KEY_ENABLE_PREDICTIVE_BACK, AppPreferences().enablePredictiveBack),
            enableSwipeDismiss = prefs.getBoolean(KEY_ENABLE_SWIPE_DISMISS, AppPreferences().enableSwipeDismiss),
            enableNavigationBadge = prefs.getBoolean(KEY_ENABLE_NAVIGATION_BADGE, AppPreferences().enableNavigationBadge),
            pagerInterceptionMode = prefs.getInt(KEY_PAGER_INTERCEPTION_MODE, AppPreferences().pagerInterceptionMode),
            pageScale = prefs.getFloat(KEY_PAGE_SCALE, AppPreferences().pageScale),
            blurIntensity = prefs.getFloat(KEY_BLUR_INTENSITY, AppPreferences().blurIntensity),
            blurAlpha = prefs.getFloat(KEY_BLUR_ALPHA, AppPreferences().blurAlpha),
            hideAppIcon = prefs.getBoolean(KEY_HIDE_APP_ICON, AppPreferences().hideAppIcon),
        )
    } catch (_: Exception) {
        AppPreferences()
    }

    private val _preferences = MutableStateFlow(load())

    /** Latest app settings, kept in sync with persisted prefs. */
    val preferencesFlow: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private fun update(transform: (AppPreferences) -> AppPreferences) {
        val updated = transform(_preferences.value)
        with(prefs.edit()) {
            putInt(KEY_COLOR_MODE, updated.colorMode)
            putString(KEY_UI_MODE, updated.uiMode)
            putString(KEY_LANGUAGE, updated.language)
            putInt(KEY_KEY_COLOR, updated.keyColor)
            putString(KEY_COLOR_STYLE, updated.colorStyle)
            putString(KEY_COLOR_SPEC, updated.colorSpec)
            putBoolean(KEY_MIUIX_MONET, updated.miuixMonet)
            putBoolean(KEY_ENABLE_BLUR, updated.enableBlur)
            putBoolean(KEY_ENABLE_FLOATING_BOTTOM_BAR, updated.enableFloatingBottomBar)
            putBoolean(KEY_ENABLE_FLOATING_BOTTOM_BAR_BLUR, updated.enableFloatingBottomBarBlur)
            putBoolean(KEY_ENABLE_SCROLL_ANIMATION, updated.enableScrollAnimation)
            putBoolean(KEY_ENABLE_PREDICTIVE_BACK, updated.enablePredictiveBack)
            putBoolean(KEY_ENABLE_SWIPE_DISMISS, updated.enableSwipeDismiss)
            putBoolean(KEY_ENABLE_NAVIGATION_BADGE, updated.enableNavigationBadge)
            putInt(KEY_PAGER_INTERCEPTION_MODE, updated.pagerInterceptionMode)
            putFloat(KEY_PAGE_SCALE, updated.pageScale)
            putFloat(KEY_BLUR_INTENSITY, updated.blurIntensity)
            putFloat(KEY_BLUR_ALPHA, updated.blurAlpha)
            putBoolean(KEY_HIDE_APP_ICON, updated.hideAppIcon)
            apply()
        }
        _preferences.value = updated
    }

    suspend fun setUiMode(value: String) = update { it.copy(uiMode = value) }

    suspend fun setLanguage(value: String) = update { it.copy(language = value) }

    suspend fun setKeyColor(value: Int) = update { it.copy(keyColor = value) }

    suspend fun setColorMode(value: Int) = update { it.copy(colorMode = value) }

    suspend fun setColorStyle(value: String) = update { it.copy(colorStyle = value) }

    suspend fun setColorSpec(value: String) = update { it.copy(colorSpec = value) }

    suspend fun setMiuixMonet(value: Boolean) = update { it.copy(miuixMonet = value) }

    suspend fun setEnableBlur(value: Boolean) = update { it.copy(enableBlur = value) }

    suspend fun setEnableFloatingBottomBar(value: Boolean) = update { it.copy(enableFloatingBottomBar = value) }

    suspend fun setEnableFloatingBottomBarBlur(value: Boolean) = update { it.copy(enableFloatingBottomBarBlur = value) }

    suspend fun setEnablePredictiveBack(value: Boolean) = update { it.copy(enablePredictiveBack = value) }

    suspend fun setEnableSwipeDismiss(value: Boolean) = update { it.copy(enableSwipeDismiss = value) }

    suspend fun setEnableNavigationBadge(value: Boolean) = update { it.copy(enableNavigationBadge = value) }

    suspend fun setPagerInterceptionMode(value: Int) = update { it.copy(pagerInterceptionMode = value) }

    suspend fun setBlurIntensity(value: Float) = update { it.copy(blurIntensity = value) }

    suspend fun setBlurAlpha(value: Float) = update { it.copy(blurAlpha = value) }

    suspend fun setPageScale(value: Float) = update { it.copy(pageScale = value) }

    suspend fun setHideAppIcon(value: Boolean) = update { it.copy(hideAppIcon = value) }

    companion object {
        // Same file/key as LocaleHelper so the persisted language is shared.
        const val PREFS_NAME = "settings_prefs"
        const val KEY_LANGUAGE = "language"

        private const val KEY_COLOR_MODE = "color_mode"
        private const val KEY_UI_MODE = "ui_mode"
        private const val KEY_KEY_COLOR = "key_color"
        private const val KEY_COLOR_STYLE = "color_style"
        private const val KEY_COLOR_SPEC = "color_spec"
        private const val KEY_MIUIX_MONET = "miuix_monet"
        private const val KEY_ENABLE_BLUR = "enable_blur"
        private const val KEY_ENABLE_FLOATING_BOTTOM_BAR = "enable_floating_bottom_bar"
        private const val KEY_ENABLE_FLOATING_BOTTOM_BAR_BLUR = "enable_floating_bottom_bar_blur"
        private const val KEY_ENABLE_SCROLL_ANIMATION = "enable_scroll_animation"
        private const val KEY_ENABLE_PREDICTIVE_BACK = "enable_predictive_back"
        private const val KEY_ENABLE_SWIPE_DISMISS = "enable_swipe_dismiss"
        private const val KEY_ENABLE_NAVIGATION_BADGE = "enable_navigation_badge"
        private const val KEY_PAGER_INTERCEPTION_MODE = "pager_interception_mode"
        private const val KEY_PAGE_SCALE = "page_scale"
        private const val KEY_BLUR_INTENSITY = "blur_intensity"
        private const val KEY_BLUR_ALPHA = "blur_alpha"
        private const val KEY_HIDE_APP_ICON = "hide_app_icon"
    }
}
