package com.kangtaeyoung.daynote.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android: EncryptedSharedPreferences 로 API 키를 암호화 저장한다.
 * 마스터 키는 Android Keystore(AES256-GCM)로 보호된다.
 */
class AndroidApiKeyProvider(context: Context) : ApiKeyProvider {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun openAiKey(): String? = prefs.getString(KEY_OPENAI, null)

    override fun setOpenAiKey(key: String) {
        prefs.edit().putString(KEY_OPENAI, key.trim()).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_OPENAI).apply()
    }

    private companion object {
        const val PREFS_NAME = "daynote_secure"
        const val KEY_OPENAI = "openai_api_key"
    }
}
