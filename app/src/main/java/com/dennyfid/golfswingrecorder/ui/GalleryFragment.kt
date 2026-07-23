package com.dennyfid.golfswingrecorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onPermissionGranted()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGalleryBinding.bind(view)

        val adapter = GalleryAdapter(
            onVideoClick = { video ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(video.uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            },
            onVideoDelete = { video ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Video")
                    .setMessage("Are you sure you want to delete this golf swing?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteVideo(video)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onVideoShare = { video ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, video.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Golf Swing"))
            },
            onVideoAnalyze = { video ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, video.uri)
                    putExtra(Intent.EXTRA_TEXT, "analyze Swiing")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Analyze with AI"))
            }
        )

        binding.recyclerView.adapter = adapter

        lifecycleScope.launchWhenStarted {
            viewModel.videos.collectLatest { videos ->
                adapter.submitList(videos)
                binding.emptyText.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.pendingDeleteUri.collectLatest { uri ->
                uri?.let {
                    android.util.Log.d("GalleryFragment", "Pending delete triggered for $it")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        try {
                            val rows = requireContext().contentResolver.delete(it, null, null)
                            if (rows > 0) {
                                android.widget.Toast.makeText(requireContext(), "Video deleted", android.widget.Toast.LENGTH_SHORT).show()
                                viewModel.onPermissionGranted() // Clears and reloads
                            }
                        } catch (e: android.app.RecoverableSecurityException) {
                            android.util.Log.d("GalleryFragment", "RecoverableSecurityException caught")
                            e.userAction.actionIntent.intentSender.let { sender ->
                                deleteLauncher.launch(
                                    androidx.activity.result.IntentSenderRequest.Builder(sender).build()
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("GalleryFragment", "Delete failed", e)
                        }
                    }
                }
            }
        }

        viewModel.loadVideos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
