package com.kangtaeyoung.daynote.data.security

import java.io.File

/**
 * Desktop(JVM): `~/.daynote/openai.key` 파일에 키를 저장한다(DB 와 같은 폴더 관례).
 * 데스크톱엔 Keystore 가 없어 평문 파일이며, 가능하면 소유자 전용 권한으로 만든다(문서화된 한계).
 */
class DesktopApiKeyProvider : ApiKeyProvider {

    private val keyFile: File by lazy {
        val appDir = File(System.getProperty("user.home"), ".daynote").apply { mkdirs() }
        File(appDir, "openai.key")
    }

    override fun openAiKey(): String? =
        keyFile.takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }

    override fun setOpenAiKey(key: String) {
        keyFile.writeText(key.trim())
        runCatching {
            keyFile.setReadable(false, false)
            keyFile.setReadable(true, true) // 소유자만 읽기
            keyFile.setWritable(false, false)
            keyFile.setWritable(true, true)
        }
    }

    override fun clear() {
        runCatching { if (keyFile.exists()) keyFile.delete() }
    }
}
