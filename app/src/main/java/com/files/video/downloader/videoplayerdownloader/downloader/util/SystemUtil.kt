package com.files.video.downloader.videoplayerdownloader.downloader.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import java.util.Locale

object SystemUtil {
    private var myLocale: Locale? = null

    // Load lại ngôn ngữ đã lưu và thay đổi chúng
    fun setLocale(context: Context) {
        val language = getPreLanguage(context)
        Log.d("ntt", "setLocale: $language")
        if (language == "") {
            val config = Configuration()
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            config.locale = locale
            context.resources
                .updateConfiguration(config, context.resources.displayMetrics)
        } else {
            changeLang(language, context)
        }
    }

    // method phục vụ cho việc thay đổi ngôn ngữ.
    fun changeLang(lang: String?, context: Context) {
//		if (lang.equals("", ignoreCase = true)) return
//		myLocale = lang?.let { Locale(it) }
//		saveLocale(context, lang)
//		myLocale?.let { Locale.setDefault(it) }
//		val config = Configuration()
//		config.locale = myLocale
//		context.resources.updateConfiguration(config, context.resources.displayMetrics)

        if (lang.equals("", ignoreCase = true)) return
        if (lang!!.contains("-")) {
            val split = lang.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            myLocale = Locale(
                split[0],
                split[1]
            )
        } else {
            myLocale = Locale(lang)
        }
        saveLocale(context, lang)
        Locale.setDefault(myLocale)
        val config = Configuration()
        config.locale = myLocale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun saveLocale(context: Context, lang: String?) {
        setPreLanguage(context, lang)
    }

    fun getPreLanguage(mContext: Context?): String {
        val preferences = mContext?.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
//		return preferences?.getString("KEY_LANGUAGE", "en")?: "en"

        Locale.getDefault().displayLanguage
        val lang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            Resources.getSystem().configuration.locale.language
        }

        return if (!getLanguageApp()
                .contains(lang)
        ) {
            preferences!!.getString("KEY_LANGUAGE", "en")!!
        } else {
            preferences!!.getString("KEY_LANGUAGE", lang)!!
        }
    }

    fun setPreLanguage(context: Context, language: String?) {
        if (language == null || language == "") {
            return
        } else {
            val preferences = context.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
            preferences.edit().putString("KEY_LANGUAGE", language).apply()
        }
    }

//    fun getLanguageApp(context: Context): List<String> {
//        val sharedPreferences = context.getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
//        // boolean openLanguage = sharedPreferences.getBoolean("openLanguage", false);
//        val languages: MutableList<String> = ArrayList()
//        languages.add("en")
//        languages.add("fr")
//        languages.add("pt")
//        languages.add("es")
//        languages.add("hi")
//        return languages
//    }

    fun getLanguageApp(): List<String> {
        val languages: MutableList<String> = java.util.ArrayList()
        languages.add("en")
        languages.add("zh")
		languages.add("zh-TW")
        languages.add("hi")
        languages.add("es")
		languages.add("pt-BR")
        languages.add("pt")
        languages.add("fr")
        languages.add("ar")
        languages.add("bn")
        languages.add("ru")
        languages.add("de")
        languages.add("ja")
        languages.add("tr")
        languages.add("ko")
        languages.add("in")
        return languages
    }
}