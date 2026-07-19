package com.coooldoggy.catasmr.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Wraps Google Play Services' Authorization API (com.google.android.gms.auth.api.identity)
 * to obtain a short-lived OAuth access token scoped to youtube.upload. We don't persist
 * refresh tokens ourselves — Play Services caches the grant, so [authorize] is called fresh
 * before every upload and returns silently once the user has consented once.
 */
class YouTubeAuthManager(private val context: Context) {

    private val client by lazy { Identity.getAuthorizationClient(context) }

    sealed interface AuthOutcome {
        data class Authorized(val accessToken: String, val accountEmail: String?) : AuthOutcome
        data class NeedsConsent(val pendingIntent: PendingIntent) : AuthOutcome
        data class Failed(val message: String) : AuthOutcome
    }

    private fun buildRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(YOUTUBE_UPLOAD_SCOPE)))
            .build()

    suspend fun authorize(): AuthOutcome {
        return try {
            val result = client.authorize(buildRequest()).await()
            when {
                result.hasResolution() -> {
                    val pendingIntent = result.pendingIntent
                    if (pendingIntent != null) {
                        AuthOutcome.NeedsConsent(pendingIntent)
                    } else {
                        AuthOutcome.Failed("Consent required but no resolution was returned")
                    }
                }
                result.accessToken != null -> AuthOutcome.Authorized(
                    accessToken = result.accessToken!!,
                    accountEmail = runCatching { result.toGoogleSignInAccount()?.email }.getOrNull()
                )
                else -> AuthOutcome.Failed("No access token returned")
            }
        } catch (e: Exception) {
            AuthOutcome.Failed(e.message ?: "Authorization failed")
        }
    }

    /** Parse the result Intent from launching a [AuthOutcome.NeedsConsent] pending intent. */
    fun resultFromIntent(data: Intent): AuthOutcome {
        return try {
            val result = client.getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token != null) {
                AuthOutcome.Authorized(
                    accessToken = token,
                    accountEmail = runCatching { result.toGoogleSignInAccount()?.email }.getOrNull()
                )
            } else {
                AuthOutcome.Failed("No access token in authorization result")
            }
        } catch (e: Exception) {
            AuthOutcome.Failed(e.message ?: "Failed to parse authorization result")
        }
    }

    companion object {
        const val YOUTUBE_UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload"
    }
}
