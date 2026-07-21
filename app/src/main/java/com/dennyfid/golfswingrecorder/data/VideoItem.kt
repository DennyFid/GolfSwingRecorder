package com.dennyfid.golfswingrecorder.data

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val name: String,
    val dateAdded: Long
)
