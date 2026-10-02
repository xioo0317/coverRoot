package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl

/**
 * In-app language switching: "system", "zh-CN", "zh-TW", "en".
 * Wrap the base context in [android.app.Activity.attachBaseContext]; applied
 * after activity recreation.
 */
object LocaleHelper {
    const val SYSTEM = "system"

    val SUPPORTED = listOf(SYSTEM, "zh-CN", "zh-TW", "en")

    /** Display names are intentionally NOT localized — shown in their own language. */
    val DISPLAY_NAMES = listOf("Follow system", "简体中文", "繁體中文", "English")

    fun wrap(context: Context): Context {
        val tag = SettingsRepositoryImpl().appLanguage
        if (tag == SYSTEM || tag !in SUPPORTED) return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
