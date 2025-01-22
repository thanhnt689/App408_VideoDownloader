package com.files.video.downloader.videoplayerdownloader.downloader.helper

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import com.files.video.downloader.videoplayerdownloader.downloader.model.Proxy
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


interface SharedPreferenceHelper {
    fun setString(key: String, value: String)
    fun getString(key: String): String?

    fun setInt(key: String, value: Int)
    fun getInt(key: String): Int?

    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String): Boolean?

}

@Singleton
class PreferenceHelper @Inject constructor(
    @ApplicationContext context: Context
) : SharedPreferenceHelper {
    companion object {
        const val APP_PREFS = "app_prefs"
        const val LAST_RATE_PREFS = "last_rate_prefs"
        const val PREF_CURRENT_LANGUAGE = "pref_current_language"
        const val PREF_SHOWED_START_LANGUAGE = "pref_showed_start_language"
        const val PREF_FIRST_APP_OPENED = "pref_first_app_opened"
        const val PREF_LAST_BASE = "pref_last_base"  // Last base selected
        const val PREF_BASE_RATE = "pref_base_rate"  // Base of last rate

        const val SWITCH_BUTTON_KEY = "switch"
        const val PREF_KEY = "pref"
        const val TICK_FORMAT_NUMBER = "tick_format_number"
        const val SELECT_FORMAT_NUMBER = "select_format_number"
        const val SELECT_TYPE_NUMBER = "select_type_number"

        const val PREF_LAST_DATE = "pref_last_date"  // Base of last rate

        private const val IS_DESKTOP = "IS_DESKTOP"
        private const val IS_FIND_BY_URL = "IS_FIND_BY_URL"
        private const val IS_CHECK_EVERY_REQUEST = "IS_CHECK_EVERY_REQUEST"
        private const val IS_AD_BLOCKER = "IS_AD_BLOCKER"
        private const val PROXY_IP_PORT = "PROXY_IP_PORT"
        private const val IS_PROXY_TURN_ON = "IS_PROXY_TURN_ON"
        private const val IS_FIRST_START = "IS_FIRST_START"
        private const val IS_SHOW_VIDEO_ALERT = "IS_SHOW_VIDEO_ALERT"
        private const val IS_SHOW_VIDEO_ACTION_BUTTON = "IS_SHOW_VIDEO_ACTION_BUTTON"
        private const val IS_PRESENT = "IS_PRESENT"
        private const val HOSTS_UPDATE = "HOSTS_UPDATE"
        private const val HOSTS_POPULATED = "HOSTS_POPULATED"
        private const val IS_EXTERNAL_USE = "IS_EXTERNAL_USE"
        private const val IS_APP_DIR_USE = "IS_APP_DIR_USE"
        private const val IS_DARK_MODE = "IS_DARK_MODE"
        const val REGULAR_THREAD_COUNT = "REGULAR_THREAD_COUNT"
        const val SPEED = "SPEED"
        const val LOOP = "LOOP_MEDIA"
        const val FILL = "FILL_MEDIA"
        private const val M3U8_THREAD_COUNT = "M3U8_THREAD_COUNT"
        private const val VIDEO_DETECTION_TRESHOLD = "VIDEO_DETECTION_TRESHOLD"
        private const val IS_LOCK_PORTRAIT = "IS_LOCK_PORTRAIT"
        private const val USER_PROXY = "USER_PROXY"
        private const val IS_CHECK_IF_IN_LIST = "IS_CHECK_IF_IN_LIST"
        private const val IS_CHECK_EVERY_ON_M3U8 = "IS_CHECK_EVERY_ON_M3U8"
        private const val IS_SETUP_PIN_CODE = "IS_SETUP_PIN_CODE"
        private const val NUM_SECURITY_QUESTION = "NUM_SECURITY_QUESTION"
        private const val SECURITY_ANSWER = "SECURITY_ANSWER"
        private const val PIN_CODE = "PIN_CODE"

    }

    //    private val sharedPreferences by lazy {
//        EncryptedSharedPreferences.create(
//            context,
//            APP_PREFS,
//            masterKeyAlias,
//            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//        )
//    }
    private val sharedPreferences by lazy {
        context.getSharedPreferences(APP_PREFS, MODE_PRIVATE)
    }

    override fun setString(key: String, value: String) {
        sharedPreferences
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    override fun setInt(key: String, value: Int) {
        sharedPreferences
            .edit()
            .putInt(key, value)
            .apply()
    }

    override fun getInt(key: String): Int {
        return sharedPreferences.getInt(key, 0)
    }

    override fun setBoolean(key: String, value: Boolean) {
        sharedPreferences
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    override fun getBoolean(key: String): Boolean? {
        return sharedPreferences.getBoolean(key, false)
    }

    fun hidePermission() {
        sharedPreferences.edit().putBoolean("show_permission", false).apply()
    }

    fun showPermission(): Boolean {
        return sharedPreferences.getBoolean("show_permission", true)
    }

    fun forceRated() {
        sharedPreferences.edit().putBoolean("rate", true).apply()
    }

    fun isRate(): Boolean {
        return sharedPreferences.getBoolean("rate", false)
    }

    fun setCurrentProxy(proxy: Proxy) {
        sharedPreferences.edit().let {
            it.putString(PROXY_IP_PORT, Gson().toJson(proxy.toMap()))
            it.apply()
        }
    }

    fun getCurrentProxy(): Proxy {
        val value = sharedPreferences.getString(PROXY_IP_PORT, "{}") ?: "{}"
        val tmp = Gson().fromJson(value, Map::class.java)
        return Proxy.fromMap(tmp)
    }

    fun getIsProxyOn(): Boolean {
        return sharedPreferences.getBoolean(IS_PROXY_TURN_ON, false)
    }

    fun setIsProxyOn(isTurnedOn: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_PROXY_TURN_ON, isTurnedOn)
            it.apply()
        }
    }

    fun getUserProxy(): Proxy? {
        val proxyString = sharedPreferences.getString(USER_PROXY, "")
        if (proxyString?.isNotEmpty() == true) {
            return Gson().fromJson(proxyString, Proxy::class.java)
        }

        return Proxy.noProxy()
    }

    fun saveUserProxy(proxy: Proxy) {
        val proxyString = Gson().toJson(proxy)
        sharedPreferences.edit().let {
            it.putString(USER_PROXY, proxyString)
            it.apply()
        }
    }


    fun setIsAdHostsUpdateTime(time: Long) {
        sharedPreferences.edit().let {
            it.putLong(HOSTS_UPDATE, time)
            it.apply()
        }
    }

    fun getAdHostsUpdateTime(): Long {
        return sharedPreferences.getLong(HOSTS_UPDATE, 0)
    }

    fun getIsPopulated(): Boolean {
        return sharedPreferences.getBoolean(HOSTS_POPULATED, false)
    }

    fun setIsPopulated(isPopulated: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(HOSTS_POPULATED, isPopulated)
            it.apply()
        }
    }

    fun getIsExternalUse(): Boolean {
        val defIsExternal = FileUtil.isExternalStorageWritable()

        return sharedPreferences.getBoolean(IS_EXTERNAL_USE, defIsExternal)
    }

    fun setIsExternalUse(isExternalUse: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_EXTERNAL_USE, isExternalUse)
            it.apply()
        }
    }

    fun getIsAppDirUse(): Boolean {
        return sharedPreferences.getBoolean(IS_APP_DIR_USE, true)
    }

    fun setIsAppDirUse(isAppDirUse: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_APP_DIR_USE, isAppDirUse)
            it.apply()
        }
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean(
            IS_DARK_MODE,
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        )
    }

    fun setIsDarkMode(isDarkMode: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_DARK_MODE, isDarkMode)
            it.apply()
        }
    }

    fun getRegularDownloaderThreadCount(): Int {
        return sharedPreferences.getInt(REGULAR_THREAD_COUNT, 1)
    }

    fun setRegularDownloaderThreadCount(count: Int) {
        sharedPreferences.edit().let {
            it.putInt(REGULAR_THREAD_COUNT, count)
            it.apply()
        }
    }

    fun getM3u8DownloaderThreadCount(): Int {
        return sharedPreferences.getInt(M3U8_THREAD_COUNT, 3) // means 4
    }

    fun setM3u8DownloaderThreadCount(count: Int) {
        sharedPreferences.edit().let {
            it.putInt(M3U8_THREAD_COUNT, count)
            it.apply()
        }
    }

    fun getVideoDetectionTreshold(): Int {
        return sharedPreferences.getInt(VIDEO_DETECTION_TRESHOLD, 5 * 1024 * 1024)
    }

    fun setVideoDetectionTreshold(count: Int) {
        sharedPreferences.edit().let {
            it.putInt(VIDEO_DETECTION_TRESHOLD, count)
            it.apply()
        }
    }

    fun getIsLockPortrait(): Boolean {
        return sharedPreferences.getBoolean(IS_LOCK_PORTRAIT, false)
    }

    fun setIsLockPortrait(isLock: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_LOCK_PORTRAIT, isLock)
            it.apply()
        }
    }

    fun getIsCheckByList(): Boolean {
        return sharedPreferences.getBoolean(IS_CHECK_IF_IN_LIST, false)
    }

    fun getIsCheckEveryOnM3u8(): Boolean {
        return sharedPreferences.getBoolean(IS_CHECK_EVERY_ON_M3U8, true)
    }

    fun saveIsCheckByList(isCheck: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_CHECK_IF_IN_LIST, isCheck)
            it.apply()
        }
    }

    fun saveIsCheckEveryOnM3u8(isCheck: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_CHECK_EVERY_ON_M3U8, isCheck)
            it.apply()
        }
    }

    fun saveIsAdBlocker(isAdBlocker: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_AD_BLOCKER, isAdBlocker)
            it.apply()
        }
    }

    fun getIsAdBlocker(): Boolean {
        return sharedPreferences.getBoolean(IS_AD_BLOCKER, true)
    }

    fun saveIsDesktop(isDesktop: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_DESKTOP, isDesktop)
            it.apply()
        }
    }

    fun getIsDesktop(): Boolean {
        return sharedPreferences.getBoolean(IS_DESKTOP, false)
    }

    fun saveIsFindByUrl(isFind: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_FIND_BY_URL, isFind)
            it.apply()
        }
    }

    fun isFindVideoByUrl(): Boolean {
        return sharedPreferences.getBoolean(IS_FIND_BY_URL, true)
    }

    fun isShowVideoAlert(): Boolean {
        return sharedPreferences.getBoolean(IS_SHOW_VIDEO_ALERT, true)
    }

    fun setIsShowVideoAlert(isShow: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_SHOW_VIDEO_ALERT, isShow)
            it.apply()
        }
    }

    fun isShowActionButton(): Boolean {
        return sharedPreferences.getBoolean(IS_SHOW_VIDEO_ACTION_BUTTON, true)
    }

    fun setIsShowActionButton(isShow: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_SHOW_VIDEO_ACTION_BUTTON, isShow)
            it.apply()
        }
    }

    fun saveIsCheck(isCheck: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_CHECK_EVERY_REQUEST, isCheck)
            it.apply()
        }
    }

    fun isCheckEveryRequestOnVideo(): Boolean {
        return sharedPreferences.getBoolean(IS_CHECK_EVERY_REQUEST, true)
    }

    fun getIsFirstStart(): Boolean {
        return sharedPreferences.getBoolean(IS_FIRST_START, true)
    }

    fun setIsFirstStart(isFirstStart: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_FIRST_START, isFirstStart)
            it.apply()
        }
    }

    fun getSpeedMedia(): Float {
        return sharedPreferences.getFloat(SPEED, 1.0f)
    }

    fun setSpeedMedia(speed: Float) {
        sharedPreferences.edit().let {
            it.putFloat(SPEED, speed)
            it.apply()
        }
    }

    fun getIsLoopMedia(): Boolean {
        return sharedPreferences.getBoolean(LOOP, false)
    }

    fun setIsLoopMedia(isLoopMedia: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(LOOP, isLoopMedia)
            it.apply()
        }
    }

    fun getIsFillMedia(): Boolean {
        return sharedPreferences.getBoolean(FILL, true)
    }

    fun setIsFillMedia(isFillMedia: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(FILL, isFillMedia)
            it.apply()
        }
    }

    fun getIsSetupPinCode(): Boolean {
        return sharedPreferences.getBoolean(IS_SETUP_PIN_CODE, false)
    }

    fun setIsSetupPinCode(isSetupPinCode: Boolean) {
        sharedPreferences.edit().let {
            it.putBoolean(IS_SETUP_PIN_CODE, isSetupPinCode)
            it.apply()
        }
    }

    fun getNumSecurityQuestion(): Int {
        return sharedPreferences.getInt(NUM_SECURITY_QUESTION, 1)
    }

    fun setNumSecurityQuestion(numSecurityQuestion: Int) {
        sharedPreferences.edit().let {
            it.putInt(NUM_SECURITY_QUESTION, numSecurityQuestion)
            it.apply()
        }
    }

    fun getSecurityAnswer(): String? {
        return sharedPreferences.getString(SECURITY_ANSWER, "")
    }

    fun setSecurityAnswer(securityAnswer: String) {
        sharedPreferences.edit().let {
            it.putString(SECURITY_ANSWER, securityAnswer)
            it.apply()
        }
    }

    fun getPinCode(): String? {
        return sharedPreferences.getString(PIN_CODE, "")
    }

    fun setPinCode(pinCode: String) {
        sharedPreferences.edit().let {
            it.putString(PIN_CODE, pinCode)
            it.apply()
        }
    }
}