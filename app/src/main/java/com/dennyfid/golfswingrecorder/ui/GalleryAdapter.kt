package com.dennyfid.golfswingrecorder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dennyfid.golfswingrecorder.data.VideoItem
import com.dennyfid.golfswingrecorder.databinding.ItemVideoBinding

class GalleryAdapter(
    private val onVideoClick: (VideoItem) -> Unit,
    private val onVideoDelete: (VideoItem) -> Unit,
    private val onVideoShare: (VideoItem) -> Unit,
    private val onVideoAnalyze: (VideoItem) -> Unit
) :
    ListAdapter<VideoItem, GalleryAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(private val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItem) {
            binding.thumbnail.load(item.uri) {
                crossfade(true)
                // Ensure video frame decoder is used if possible
                placeholder(android.R.drawable.progress_indeterminate_horizontal)
                error(android.R.drawable.ic_dialog_alert)
            }
            binding.root.setOnClickListener { onVideoClick(item) }
            binding.btnDelete.setOnClickListener { 
                android.util.Log.d("GalleryAdapter", "Delete clicked for ${item.uri}")
                onVideoDelete(item) 
            }
            binding.btnShare.setOnClickListener {
                onVideoShare(item)
            }
            binding.btnAnalyze.setOnClickListener {
                onVideoAnalyze(item)
            }
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem) = oldItem.uri == newItem.uri
        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem) = oldItem == newItem
    }
}
