package com.example.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun rememberLocalizedResources(languageCode: String): Resources {
    val context = LocalContext.current
    return remember(context, languageCode) {
        val locale = when (languageCode) {
            "en" -> Locale.ENGLISH
            "zh-Hans" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            else -> Locale.TRADITIONAL_CHINESE
        }
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        context.createConfigurationContext(config).resources
    }
}

fun Resources.str(@StringRes id: Int, vararg args: Any): String {
    return if (args.isEmpty()) getString(id) else getString(id, *args)
}
