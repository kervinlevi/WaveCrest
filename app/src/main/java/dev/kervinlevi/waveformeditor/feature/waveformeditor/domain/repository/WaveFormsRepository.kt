package dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.repository

import dev.kervinlevi.waveformeditor.common.domain.model.Result
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.model.WavePair

interface WaveFormsRepository {
    suspend fun getWaveFormList(uriString: String): List<WavePair>
    suspend fun saveWaveFormFileToDownloads(waves: List<WavePair>): Result<String>
}
