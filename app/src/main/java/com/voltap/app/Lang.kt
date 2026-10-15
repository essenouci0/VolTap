package com.voltap.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** In-app language: 0 = follow the system, 1 = English, 2 = Arabic. */
object Lang {
    private class ResContext(base: Context, private val res: Resources) : ContextWrapper(base) {
        override fun getResources(): Resources = res
    }

    fun localized(base: Context, language: Int): Context {
        val tag = when (language) {
            1 -> "en"
            2 -> "ar"
            else -> return base
        }
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return ResContext(base, base.createConfigurationContext(config).resources)
    }
}
