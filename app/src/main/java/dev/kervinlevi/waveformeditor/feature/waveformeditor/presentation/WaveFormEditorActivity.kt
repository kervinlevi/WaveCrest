package dev.kervinlevi.waveformeditor.feature.waveformeditor.presentation

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.kervinlevi.waveformeditor.R
import dev.kervinlevi.waveformeditor.common.domain.model.Result
import dev.kervinlevi.waveformeditor.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class WaveFormEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: WaveFormEditorViewModel by viewModel()
    private val openFileContract = registerForActivityResult(OpenDocument()) { uri ->
        viewModel.importFile(uri?.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setImmersiveMode()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.initialize()
        collectFlows()
    }

    override fun onStop() {
        super.onStop()
        viewModel.updateMarkers(binding.trimmerView.getMarkers())
    }

    private fun ActivityMainBinding.initialize() {
        importButton.setOnClickListener {
            val launchFileSelector = { openFileContract.launch(ACCEPTED_MIME_TYPES) }
            if (binding.trimmerView.isEmpty()) {
                launchFileSelector.invoke()
            } else {
                createDiscardAlertDialog(launchFileSelector)
            }
        }

        downloadButton.setOnClickListener {
            viewModel.updateMarkers(binding.trimmerView.getMarkers())
            if (hasWritePermission()) {
                downloadFile()
            } else {
                requestWritePermissionAndDownload()
            }
        }
    }

    private fun collectFlows() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pointsFlow.collect {
                binding.trimmerView.setWaves(it.waves, it.markerAIndex, it.markerBIndex)
                binding.importButton.isEnabled = it.importButtonEnabled
                binding.downloadButton.isEnabled = it.exportButtonEnabled
                binding.progressFramelayout.isVisible = it.isLoading
            }
        }
    }

    private fun downloadFile() = lifecycleScope.launch {
        when (val result = viewModel.saveTrimmedFileToDownloads()) {
            is Result.Success -> {
                val fileName = result.value.toUri().toFile().name
                Toast.makeText(
                    this@WaveFormEditorActivity,
                    getString(R.string.file_save_success, fileName),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is Result.Failure -> {
                Toast.makeText(
                    this@WaveFormEditorActivity,
                    getString(R.string.file_save_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_PERMISSION_AND_DOWNLOAD && grantResults.firstOrNull() == PERMISSION_GRANTED) {
            downloadFile()
        } else {
            showFilesPermissionRationaleDialog()
        }
    }

    private fun hasWritePermission(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.P || ActivityCompat.checkSelfPermission(
            this, WRITE_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED
    }

    private fun requestWritePermissionAndDownload() {
        ActivityCompat.requestPermissions(
            this, arrayOf(WRITE_EXTERNAL_STORAGE), WRITE_PERMISSION_AND_DOWNLOAD
        )
    }

    private fun showFilesPermissionRationaleDialog() {
        AlertDialog.Builder(this).apply {
            setMessage(R.string.write_permission_message)
            setPositiveButton(R.string.write_permission_button) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun createDiscardAlertDialog(yesAction: () -> Unit) {
        AlertDialog.Builder(this).apply {
            setMessage(R.string.discard_message)
            setPositiveButton(R.string.discard_message_yes) { dialog, _ ->
                yesAction.invoke()
                dialog.dismiss()
            }
            setNegativeButton(R.string.discard_message_no) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun setImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        private const val WRITE_PERMISSION_AND_DOWNLOAD = 3000
        private val ACCEPTED_MIME_TYPES = arrayOf("text/plain")
    }
}
