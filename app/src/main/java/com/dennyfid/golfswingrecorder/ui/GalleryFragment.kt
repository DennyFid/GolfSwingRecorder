package com.dennyfid.golfswingrecorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.dennyfid.golfswingrecorder.R
import com.dennyfid.golfswingrecorder.databinding.FragmentGalleryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class GalleryFragment : Fragment(R.layout.fragment_gallery) {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGalleryBinding.bind(view)

        val adapter = GalleryAdapter { video ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(video.uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }

        binding.recyclerView.adapter = adapter

        lifecycleScope.launchWhenStarted {
            viewModel.videos.collectLatest { videos ->
                adapter.submitList(videos)
                binding.emptyText.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadVideos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
