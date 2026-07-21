package com.dennyfid.golfswingrecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.dennyfid.golfswingrecorder.R
import com.dennyfid.golfswingrecorder.camera.CameraManager
import com.dennyfid.golfswingrecorder.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var cameraManager: CameraManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permissions required for recording", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainBinding.bind(view)

        checkPermissions()

        binding.btnStartSession.setOnClickListener {
            viewModel.startSession()
        }

        binding.btnReviewVideos.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_galleryFragment)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                updateUi(state)
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startCamera() {
        cameraManager.startCamera(
            viewLifecycleOwner,
            binding.previewView.surfaceProvider,
            viewModel::onMotionDetected
        )
    }

    private fun updateUi(state: MainViewModel.UiState) {
        when (state) {
            MainViewModel.UiState.Idle -> {
                binding.statusText.text = "Status: READY"
                binding.btnStartSession.text = "Start Session"
            }
            MainViewModel.UiState.Waiting -> {
                binding.statusText.text = "Waiting for Swing..."
                binding.btnStartSession.text = "Stop Session"
            }
            MainViewModel.UiState.Recording -> {
                binding.statusText.text = "● Recording"
            }
            MainViewModel.UiState.Saving -> {
                binding.statusText.text = "Saving..."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
