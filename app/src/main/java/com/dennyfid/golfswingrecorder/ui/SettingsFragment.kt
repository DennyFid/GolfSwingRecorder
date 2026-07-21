package com.dennyfid.golfswingrecorder.ui

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.dennyfid.golfswingrecorder.R
import com.dennyfid.golfswingrecorder.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupThresholdSeekBar()
        setupPreRollSeekBar()

        lifecycleScope.launchWhenStarted {
            viewModel.motionThreshold.collectLatest {
                binding.seekBarThreshold.progress = it.toInt()
                binding.textThresholdValue.text = it.toInt().toString()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.preRollSec.collectLatest {
                binding.seekBarPreRoll.progress = it
                binding.textPreRollValue.text = "${it}s"
            }
        }
    }

    private fun setupThresholdSeekBar() {
        binding.seekBarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.textThresholdValue.text = progress.toString()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.setMotionThreshold(seekBar?.progress?.toDouble() ?: 5000.0)
            }
        })
    }

    private fun setupPreRollSeekBar() {
        binding.seekBarPreRoll.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.textPreRollValue.text = "${progress}s"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.setPreRollSec(seekBar?.progress ?: 1)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
