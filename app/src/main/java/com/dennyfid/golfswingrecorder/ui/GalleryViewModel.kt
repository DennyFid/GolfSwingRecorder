package com.dennyfid.golfswingrecorder.ui

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dennyfid.golfswingrecorder.data.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _pendingDeleteUri = MutableStateFlow<android.net.Uri?>(null)
    val pendingDeleteUri: StateFlow<android.net.Uri?> = _pendingDeleteUri

    fun loadVideos() {
        viewModelScope.launch {
            val videoList = queryVideos()
            _videos.value = videoList
        }
    }

    private suspend fun queryVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%GolfSwingRecorder%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val date = cursor.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)
                videoList.add(VideoItem(contentUri, name, date))
            }
        }
        videoList
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.delete(video.uri, null, null)
                }
                loadVideos()
            } catch (securityException: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                    securityException is android.app.RecoverableSecurityException
                ) {
                    _pendingDeleteUri.value = video.uri
                } else {
                    android.util.Log.e("GalleryViewModel", "Delete failed", securityException)
                }
            }
        }
    }

    fun onPermissionGranted() {
        _pendingDeleteUri.value = null
        loadVideos()
    }
}
