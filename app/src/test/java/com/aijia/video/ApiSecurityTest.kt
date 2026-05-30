package com.aijia.video

import com.aijia.video.data.remote.ApiSecurity
import org.junit.Test
import org.junit.Assert.*

class ApiSecurityTest {

    @Test
    fun testEncryptDecrypt() {
        ApiSecurity.initWithPassphrase("AijiaAES2026SecurePassphrase!@#")

        val plaintext = "https://example.com/video.mp4"
        println("原始文本: $plaintext")

        val encrypted = ApiSecurity.encrypt(plaintext)
        println("加密结果: $encrypted")

        val decrypted = ApiSecurity.decrypt(encrypted)
        println("解密结果: $decrypted")

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testEncryptDecryptChinese() {
        ApiSecurity.initWithPassphrase("AijiaAES2026SecurePassphrase!@#")

        val plaintext = "第1集\$http://example.com/1.mp4#第2集\$http://example.com/2.mp4"

        val encrypted = ApiSecurity.encrypt(plaintext)
        val decrypted = ApiSecurity.decrypt(encrypted)

        assertEquals(plaintext, decrypted)
    }
}
