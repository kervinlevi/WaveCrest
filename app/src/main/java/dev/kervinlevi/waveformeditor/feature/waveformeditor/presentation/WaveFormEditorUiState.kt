package dev.kervinlevi.waveformeditor.feature.waveformeditor.presentation

import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.model.WavePair

data class WaveFormEditorUiState(
    val waves: List<WavePair> = emptyList(),
    val markerAIndex: Int = -1,
    val markerBIndex: Int = -1,
    val isLoading: Boolean = false,
    val loadingStringResource: Int = 0,
    val importButtonEnabled: Boolean = false,
    val exportButtonEnabled: Boolean = false,
)
