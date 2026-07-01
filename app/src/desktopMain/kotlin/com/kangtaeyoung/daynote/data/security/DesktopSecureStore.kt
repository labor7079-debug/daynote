package com.kangtaeyoung.daynote.data.security

import java.io.File
import java.util.Properties

/**
 * Desktop(JVM): `~/.daynote/secure.properties` 에 키-값 저장(소유자 전용 권한).
 * 데스크톱엔 Keystore 가 없어 평문이며, 가능하면 파일 권한을 소유자 전용으로 제한한다(문서화된 한계).
 */
class DesktopSecureStore : SecureStore {

    private val file: File by lazy {
        val dir = File(System.getProperty("user.home"), ".daynote").apply { mkdirs() }
        File(dir, "secure.properties")
    }

    private fun load(): Properties = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    private fun save(props: Properties) {
        file.outputStream().use { props.store(it, "DayNote secure store") }
        runCatching {
            file.setReadable(false, false); file.setReadable(true, true)
            file.setWritable(false, false); file.setWritable(true, true)
        }
    }

    override fun get(key: String): String? = load().getProperty(key)?.ifBlank { null }

    override fun put(key: String, value: String) {
        val props = load()
        props.setProperty(key, value)
        save(props)
    }

    override fun remove(key: String) {
        val props = load()
        if (props.remove(key) != null) save(props)
    }
}
