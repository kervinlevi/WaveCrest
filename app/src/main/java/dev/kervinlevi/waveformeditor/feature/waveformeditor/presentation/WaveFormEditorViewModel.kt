package dev.kervinlevi.waveformeditor.feature.waveformeditor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kervinlevi.waveformeditor.common.domain.model.Result
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.repository.WaveFormsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.InvalidParameterException

class WaveFormEditorViewModel(
    private val waveFormsRepository: WaveFormsRepository
) : ViewModel() {

    private val _pointsFlow = MutableStateFlow(WaveFormEditorUiState(importButtonEnabled = true))
    val pointsFlow: StateFlow<WaveFormEditorUiState> = _pointsFlow

    fun importFile(uriString: String?) = viewModelScope.launch {
        if (uriString.isNullOrEmpty()) {
            return@launch
        }

        _pointsFlow.value = WaveFormEditorUiState(isLoading = true)
        val waves = waveFormsRepository.getWaveFormList(uriString)
        _pointsFlow.value = WaveFormEditorUiState(
            waves,
            markerAIndex = 0,
            markerBIndex = waves.lastIndex,
            importButtonEnabled = true,
            exportButtonEnabled = true,
        )
    }

    fun updateMarkers(markers: Pair<Int, Int>) {
        if (areMarkersValid(markers.first, markers.second, pointsFlow.value.waves.size)) {
            _pointsFlow.value = _pointsFlow.value.copy(
                markerAIndex = markers.first, markerBIndex = markers.second
            )
        }
    }

    private fun areMarkersValid(markerAIndex: Int, markerBIndex: Int, size: Int): Boolean {
        return markerAIndex < size && markerBIndex < size && markerAIndex in 0 until markerBIndex
    }

    suspend fun saveTrimmedFileToDownloads(): Result<String> {
        val state = pointsFlow.value
        return if (areMarkersValid(state.markerAIndex, state.markerBIndex, state.waves.size)) {
            _pointsFlow.value = state.copy(isLoading = true)
            val trimmed = state.waves.subList(state.markerAIndex, state.markerBIndex.inc())
            val uriResult = waveFormsRepository.saveWaveFormFileToDownloads(trimmed)
            _pointsFlow.value = state.copy(isLoading = false)
            uriResult
        } else {
            Result.Failure(InvalidParameterException())
        }
    }
}
