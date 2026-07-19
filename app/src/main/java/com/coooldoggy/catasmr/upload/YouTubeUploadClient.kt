package com.coooldoggy.catasmr.upload

import android.content.Context
import android.net.Uri
import com.coooldoggy.catasmr.settings.PrivacyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException

/**
 * Raw REST calls against the YouTube Data API v3 resumable-upload protocol — deliberately
 * not the generated google-api-services-youtube client, which pulls in a much heavier
 * dependency tree for what's really two HTTP calls. v1 does a single-shot PUT (no
 * byte-range resume-on-interrupt); acceptable given clips are short.
 */
class YouTubeUploadClient(private val context: Context) {

    private val client = OkHttpClient()

    class UploadException(message: String) : IOException(message)

    suspend fun upload(
        accessToken: String,
        videoUri: Uri,
        title: String,
        description: String,
        privacyStatus: PrivacyStatus
    ): String = withContext(Dispatchers.IO) {
        val size = fileSizeOf(videoUri) ?: throw UploadException("Could not determine file size for $videoUri")
        val sessionUrl = initiateSession(accessToken, size, title, description, privacyStatus)
        uploadBytes(accessToken, sessionUrl, videoUri, size)
    }

    private fun fileSizeOf(uri: Uri): Long? {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            val length = afd.length
            return if (length >= 0) length else null
        }
        return null
    }

    private fun initiateSession(
        accessToken: String,
        fileSize: Long,
        title: String,
        description: String,
        privacyStatus: PrivacyStatus
    ): String {
        val metadata = JSONObject().apply {
            put("snippet", JSONObject().apply {
                put("title", title)
                put("description", description)
                put("categoryId", "15") // Pets & Animals
            })
            put("status", JSONObject().apply {
                put("privacyStatus", privacyStatus.apiValue)
            })
        }

        val body = metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("X-Upload-Content-Type", "video/mp4")
            .addHeader("X-Upload-Content-Length", fileSize.toString())
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UploadException("Failed to start upload session: HTTP ${response.code} ${response.body?.string()}")
            }
            return response.header("Location") ?: throw UploadException("No upload session URL returned")
        }
    }

    private fun uploadBytes(accessToken: String, sessionUrl: String, uri: Uri, size: Long): String {
        val streamingBody = object : RequestBody() {
            override fun contentType() = "video/mp4".toMediaType()
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw UploadException("Could not open $uri")
                input.use { stream ->
                    val buffer = ByteArray(8192)
                    var read = stream.read(buffer)
                    while (read >= 0) {
                        sink.write(buffer, 0, read)
                        read = stream.read(buffer)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(sessionUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .put(streamingBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UploadException("Upload failed: HTTP ${response.code} ${response.body?.string()}")
            }
            val json = JSONObject(response.body?.string().orEmpty())
            return json.optString("id")
        }
    }
}
