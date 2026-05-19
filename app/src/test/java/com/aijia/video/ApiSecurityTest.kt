package com.aijia.video

import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.util.GifUrlReader
import org.junit.Test
import org.junit.Assert.*

class ApiSecurityTest {

    @Test
    fun testKeyGeneration() {
        val config = GifUrlReader.Config(
            url = "https://api.example.com",
            keyPrefix = "kumiao_enc_v3_",
            keySalt = "K8#mZ!qL9vX2",
            keyTimeout = 300L,
            signPrefix = "kumiao_sign_v3_",
            signKey = "N5@pR#wY7kJ1"
        )
        ApiSecurity.init(config)
        
        val key = ApiSecurity.generateDynamicKey()
        println("密钥长度: ${key.size}")
        assertEquals(32, key.size)
        
        val keyHex = key.joinToString("") { "%02x".format(it) }
        println("密钥SHA256: $keyHex")
    }
    
    @Test
    fun testEncryptDecrypt() {
        val config = GifUrlReader.Config(
            url = "https://api.example.com",
            keyPrefix = "kumiao_enc_v3_",
            keySalt = "K8#mZ!qL9vX2",
            keyTimeout = 300L,
            signPrefix = "kumiao_sign_v3_",
            signKey = "N5@pR#wY7kJ1"
        )
        ApiSecurity.init(config)
        
        val plaintext = "https://example.com/video.mp4"
        println("原始文本: $plaintext")
        
        val encrypted = ApiSecurity.encrypt(plaintext)
        println("加密结果: $encrypted")
        
        val decrypted = ApiSecurity.decrypt(encrypted)
        println("解密结果: $decrypted")
        
        assertEquals(plaintext, decrypted)
    }
}
