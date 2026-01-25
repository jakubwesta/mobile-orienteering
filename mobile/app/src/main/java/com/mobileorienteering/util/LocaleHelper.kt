package com.mobileorienteering.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.core.content.edit

object LocaleHelper {
    private const val PREFS_NAME = "settings_cache"
    private const val KEY_LOCALE_CODE = "locale_code"
    private const val DEFAULT_LOCALE = "en"

    fun attachBaseContext(context: Context): Context {
        val localeCode = getCachedLocaleCode(context)
        val locale = Locale.forLanguageTag(localeCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun cacheLocaleCode(context: Context, localeCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LOCALE_CODE, localeCode)
        }
    }

    private fun getCachedLocaleCode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE_CODE, DEFAULT_LOCALE) ?: DEFAULT_LOCALE
    }
}
