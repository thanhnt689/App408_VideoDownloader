package com.files.video.downloader.videoplayerdownloader.downloader.ui.language

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.MainActivity
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityLanguageBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.setLocale
import com.files.video.downloader.videoplayerdownloader.downloader.ui.intro.IntroActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtil.getLanguageApp
import com.nlbn.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var languageAdapter: LanguageAdapter

    val languages = mutableListOf<LanguageModel>()

    private var isLoadNativeLanguageSelect = false

    var fromSplash = false
    override fun setBinding(layoutInflater: LayoutInflater): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(layoutInflater)
    }

    override fun initView() {
        initData()
        setupListLanguage()
        initEvent()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initData() {

        fromSplash = intent.getBooleanExtra(FROM_SPLASH, false)

        val selectedLanguage = if (fromSplash) {
            getLanguage()
        } else {
            SystemUtil.getPreLanguage(this)
        }

        if (fromSplash) {
            val localizedContext = getLocalizedContext(selectedLanguage)
            binding.tvLanguageOpen.text = localizedContext.getString(R.string.string_languages)
            binding.tvLanguageSetting.text = localizedContext.getString(R.string.string_languages)
        }

        languages.add(LanguageModel("English", "en"))
        languages.add(LanguageModel("中文（简体)", "zh"))
        languages.add(LanguageModel("中文（繁體)", "zh-TW"))
        languages.add(LanguageModel("हिंदी भाषा", "hi"))
        languages.add(LanguageModel("Español", "es"))
        languages.add(LanguageModel("Português (Brasil)", "pt-BR"))
        languages.add(LanguageModel("Português (Portugal)", "pt"))
        languages.add(LanguageModel("Français", "fr"))
        languages.add(LanguageModel("العربية", "ar"))
        languages.add(LanguageModel("বাংলা", "bn"))
        languages.add(LanguageModel("Русский", "ru"))
        languages.add(LanguageModel("Deutsch", "de"))
        languages.add(LanguageModel("日本語", "ja"))
        languages.add(LanguageModel("Türkçe", "tr"))
        languages.add(LanguageModel("한국어", "ko"))
        languages.add(LanguageModel("Bahasa Indonesia", "in"))


        val languageFound = languages.firstOrNull { it.code == selectedLanguage }

        if (languageFound != null) {
            languages.remove(languageFound)
            languages.add(3, languageFound)
        } else {
            val englishLanguage = languages.find { it.code == "en" }
            englishLanguage?.let {
                languages.remove(it)
                languages.add(3, it)
            }
        }

    }

    private fun initListLanguage(selectedLanguage: String) {
        if (!fromSplash) {
            if (languages.any { it.code == selectedLanguage }) {
                languages.first { it.code == selectedLanguage }.active = true
            }
        }

        binding.lvLanguage.adapter = languageAdapter
        languageAdapter.submitList(languages)

        if (fromSplash) {
            languageAdapter.setIsSplash(true)
        } else {
            languageAdapter.setIsSplash(false)
        }

        languageAdapter.setOnClickListener {
            if (fromSplash) {
                val localizedContext =
                    getLocalizedContext(languageAdapter.getSelectedLanguage()!!.code)
                binding.tvLanguageOpen.text = localizedContext.getString(R.string.string_languages)
                binding.tvLanguageSetting.text =
                    localizedContext.getString(R.string.string_languages)

                if (!isLoadNativeLanguageSelect
                ) {
                    isLoadNativeLanguageSelect = true
//                    loadAdsNativeLanguageSelect()
                }
            }
        }

    }

    private fun getLocalizedContext(languageCode: String): Context {
        val locale = if (languageCode.contains("-")) {
            val split = languageCode.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            Locale(
                split[0],
                split[1]
            )
        } else {
            Locale(languageCode)
        }

        val config = resources.configuration
        config.setLocale(locale)

        return createConfigurationContext(config)
    }

    private fun getLanguage(): String {
        Locale.getDefault().displayLanguage
        val lang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            Resources.getSystem().configuration.locale.language
        }

        return if (!getLanguageApp()
                .contains(lang)
        ) {
            "en"
        } else {
            lang
        }
    }

    private fun setupListLanguage() {
        fromSplash = intent.getBooleanExtra(FROM_SPLASH, false)
        if (fromSplash) {
            binding.layoutLanguagesOpen.visibility = View.VISIBLE
            binding.layoutLanguagesSetting.visibility = View.GONE
            val locale = Locale.getDefault()
            initListLanguage(locale.language)

//            loadAdsNativeLanguage()

        } else {
            binding.layoutLanguagesOpen.visibility = View.GONE
            binding.layoutLanguagesSetting.visibility = View.VISIBLE
            val selectedLanguage =
                preferenceHelper.getString(PreferenceHelper.PREF_CURRENT_LANGUAGE)
            initListLanguage(selectedLanguage ?: "")

//            loadAdsNativeLanguageSetting()
        }
    }

    private fun initEvent() {
        binding.btnDone.setOnClickListener {
            val selectedLanguage = languageAdapter.getSelectedLanguage()
            if (selectedLanguage == null) {
                Toast.makeText(
                    this,
                    getString(R.string.string_please_select_language),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                setLocale(selectedLanguage.code)
                preferenceHelper.setString(
                    PreferenceHelper.PREF_CURRENT_LANGUAGE,
                    selectedLanguage.code
                )
                SystemUtil.setPreLanguage(this, selectedLanguage.code)
                if (fromSplash) {
                    startActivity(
                        IntroActivity.newIntent(this).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    )
                } else {
                    if (Admob.getInstance().isLoadFullAds) {
                        val intent = IntroActivity.newIntent(this)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finishAffinity()
                    } else {
                        val intent = MainActivity.newIntent(this)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finishAffinity()
                    }
                }
            }
        }

        binding.btnDoneSetting.setOnClickListener {
            val selectedLanguage = languageAdapter.getSelectedLanguage()
            if (selectedLanguage == null) {
                Toast.makeText(
                    this,
                    getString(R.string.string_please_select_language),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                setLocale(selectedLanguage.code)
                preferenceHelper.setString(
                    PreferenceHelper.PREF_CURRENT_LANGUAGE,
                    selectedLanguage.code
                )
                SystemUtil.setPreLanguage(this, selectedLanguage.code)

                if (Admob.getInstance().isLoadFullAds) {
                    val intent = IntroActivity.newIntent(this)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                } else {
                    val intent = MainActivity.newIntent(this)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                }

            }
        }
    }

    companion object {
        const val FROM_SPLASH = "from_splash"
        fun newIntent(context: Context, fromSplash: Boolean = false): Intent {
            val intent = Intent(context, LanguageActivity::class.java)
            intent.putExtra(FROM_SPLASH, fromSplash)
            if (fromSplash) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            return intent
        }

    }
}