package com.coooldoggy.catasmr.ui.gallery

import android.os.Environment
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class GalleryViewModel : ViewModel() {
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    init {
        loadVideos()
    }

    private fun loadVideos() {
        val moviesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "CatAsmr"
        )

        if (moviesDir.exists() && moviesDir.isDirectory) {
            val videoFiles = moviesDir.listFiles { file ->
                file.isFile && file.extension == "mp4"
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            _videos.value = videoFiles.map { file ->
                VideoItem(
                    fileName = file.name,
                    path = file.absolutePath,
                    timestamp = file.lastModified()
                )
            }
        }
    }

    fun deleteVideo(path: String) {
        File(path).delete()
        loadVideos()
    }
}
