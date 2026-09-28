package com.coverRoot.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    data class Language(
        val code: String,
        val nativeName: String,
        val displayName: String,
    )

    val supportedLanguages = listOf(
        Language("system", "", ""),
        Language("en", "English", "English"),
        Language("zh-CN", "简体中文", "Chinese (Simplified)"),
        Language("zh-TW", "繁體中文", "Chinese (Traditional)"),
        Language("ja", "日本語", "Japanese"),
        Language("ko", "한국어", "Korean"),
        Language("de", "Deutsch", "German"),
        Language("es", "Español", "Spanish"),
        Language("fr", "Français", "French"),
        Language("ru", "Русский", "Russian"),
    )

    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_LANGUAGE = "language"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun persistLanguage(context: Context, language: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun wrapContext(context: Context): Context {
        val language = prefs(context).getString(KEY_LANGUAGE, "system") ?: "system"
        if (language == "system") return context
        return try {
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } catch (_: Exception) {
            context
        }
    }

    fun localizedContext(context: Context, language: String): Context {
        if (language == "system") return context
        return try {
            val locale = Locale.forLanguageTag(language)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } catch (_: Exception) {
            context
        }
    }
}
