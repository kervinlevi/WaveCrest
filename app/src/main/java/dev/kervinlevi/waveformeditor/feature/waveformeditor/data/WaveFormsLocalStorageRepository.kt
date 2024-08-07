package dev.kervinlevi.waveformeditor.feature.waveformeditor.data

import android.content.Context
import android.icu.util.Calendar
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import dev.kervinlevi.waveformeditor.common.domain.model.Result
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.model.WavePair
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.repository.WaveFormsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class WaveFormsLocalStorageRepository(
    private val context: Context, private val ioDispatcher: CoroutineDispatcher
) : WaveFormsRepository {

    override suspend fun getWaveFormList(uriString: String): List<Pair<Double, Double>> =
        withContext(ioDispatcher) {
            val uri = Uri.parse(uriString)
            val points = mutableListOf<Pair<Double, Double>>()
            if (uri != null) {
                val reader = context.contentResolver.openInputStream(uri)?.bufferedReader()
                var line: String?

                while (reader?.readLine().also { line = it } != null) {
                    val split = line?.split("\\s+".toRegex())
                    val first = split?.getOrNull(0)?.toDoubleOrNull()
                    val second = split?.getOrNull(1)?.toDoubleOrNull()
                    if (first != null && second != null) {
                        points.add(first to second)
                    }
                }
                reader?.close()
            }
            points
        }

    override suspend fun saveWaveFormFileToDownloads(waves: List<WavePair>): Result<String> =
        withContext(Dispatchers.IO) {
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists()) {
                downloads.mkdir()
            }
            val fileName = "trimmed_wave${Calendar.getInstance().timeInMillis}.txt"
            val file = File(downloads, fileName)
            val outputStream = FileOutputStream(file)
            try {
                waves.forEach {
                    outputStream.write("${it.first} ${it.second}\n".toByteArray())
                }
                outputStream.close()
                return@withContext Result.Success(file.toUri().toString())
            } catch (e: Exception) {
                outputStream.close()
                return@withContext Result.Failure(e)
            }
        }
}
