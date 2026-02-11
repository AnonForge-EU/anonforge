package com.anonforge.data.remote.simplelogin

import com.anonforge.core.security.ApiKeyManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor for SimpleLogin API authentication.
 * Injects API key header and wipes key from memory after use.
 */
class SimpleLoginInterceptor @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val apiKey = apiKeyManager.retrieveApiKey()
        
        return if (apiKey != null) {
            try {
                val apiKeyString = String(apiKey)
                
                val authenticatedRequest = originalRequest.newBuilder()
                    .addHeader("Authentication", apiKeyString)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                chain.proceed(authenticatedRequest)
            } finally {
                // Critical: Wipe API key from memory immediately
                apiKey.fill('\u0000')
            }
        } else {
            // No API key configured - proceed without auth (will fail with 401)
            chain.proceed(originalRequest)
        }
    }
}
