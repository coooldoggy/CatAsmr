package com.coooldoggy.catasmr.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient

class KakaoManager(private val context: Context) {

    fun signIn(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                handleLoginResult(token, error, onSuccess, onError)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                handleLoginResult(token, error, onSuccess, onError)
            }
        }
    }

    private fun handleLoginResult(
        token: OAuthToken?,
        error: Throwable?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        when {
            error != null -> {
                Log.e("KakaoManager", "Login failed: ${error.message}")
                onError(error.message ?: "Login failed")
            }
            token != null -> {
                Log.i("KakaoManager", "Login success: ${token.accessToken}")
                onSuccess(token.accessToken)
            }
        }
    }

    fun openKakaoTalkToMe(
        title: String,
        message: String
    ) {
        val text = "$title\n$message"

        try {
            // Try to open KakaoTalk chat with "Me" (자신에게)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.kakao.talk"
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("KakaoManager", "Failed to open KakaoTalk: ${e.message}")
            // Fallback: try to open KakaoTalk app
            try {
                val uri = Uri.parse("kakaoopen://talk")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (ex: Exception) {
                Log.e("KakaoManager", "KakaoTalk app not installed")
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        UserApiClient.instance.logout { _ ->
            Log.i("KakaoManager", "Logout success")
            onComplete()
        }
    }

    fun getUserInfo(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        UserApiClient.instance.me { user, error ->
            when {
                error != null -> {
                    Log.e("KakaoManager", "Get user info failed: ${error.message}")
                    onError(error.message ?: "Failed to get user info")
                }
                user != null -> {
                    val nickname = user.kakaoAccount?.profile?.nickname ?: "User"
                    Log.i("KakaoManager", "User info: $nickname")
                    onSuccess(nickname)
                }
            }
        }
    }
}
