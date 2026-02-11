package com.anonforge.security.encryption

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportCryptoManager @Inject constructor() {
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
    
    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        
        val key = deriveKey(password, salt)
        
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        val ciphertext = cipher.doFinal(data)
        
        password.fill('0')
        key.fill(0)
        
        return salt + iv + ciphertext
    }
    
    fun decrypt(encrypted: ByteArray, password: CharArray): ByteArray {
        require(encrypted.size > SALT_LENGTH + GCM_IV_LENGTH) { "Invalid encrypted data" }
        
        val salt = encrypted.copyOfRange(0, SALT_LENGTH)
        val iv = encrypted.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
        val ciphertext = encrypted.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, encrypted.size)
        
        val key = deriveKey(password, salt)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        val plaintext = cipher.doFinal(ciphertext)
        
        password.fill('0')
        key.fill(0)
        
        return plaintext
    }
    
    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
