package dev.kervinlevi.waveformeditor.di

import dev.kervinlevi.waveformeditor.feature.waveformeditor.data.WaveFormsLocalStorageRepository
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.repository.WaveFormsRepository
import dev.kervinlevi.waveformeditor.feature.waveformeditor.presentation.WaveFormEditorViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object AppModule {
    val module = module {
        single<WaveFormsRepository> {
            WaveFormsLocalStorageRepository(androidContext(), Dispatchers.IO)
        }
        viewModel { WaveFormEditorViewModel(get()) }
    }
}
