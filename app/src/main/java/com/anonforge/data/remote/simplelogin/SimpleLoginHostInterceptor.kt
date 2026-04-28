package com.anonforge.data.remote.simplelogin

import com.anonforge.core.security.ApiKeyManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Rewrites the request host to point at the user-configured SimpleLogin
 * instance (self-hosted setups). When no custom URL is configured, the
 * request is passed through untouched and Retrofit's BASE_URL takes effect.
 *
 * SECURITY: setInstanceUrl() in ApiKeyManager only accepts https:// URLs,
 * so this interceptor will never downgrade to cleartext.
 */
class SimpleLoginHostInterceptor @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!apiKeyManager.isUsingCustomInstance()) {
            return chain.proceed(request)
        }
        val configured = apiKeyManager.getInstanceUrl().toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val rewritten = request.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .build()
        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
