package dev.kervinlevi.waveformeditor.feature.waveformeditor.presentation

import dev.kervinlevi.waveformeditor.MainDispatcherRule
import dev.kervinlevi.waveformeditor.common.domain.model.Result
import dev.kervinlevi.waveformeditor.common.domain.model.Result.Failure
import dev.kervinlevi.waveformeditor.common.domain.model.Result.Success
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.repository.WaveFormsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WaveFormEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    lateinit var mockRepository: WaveFormsRepository

    private lateinit var viewModel: WaveFormEditorViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        viewModel = WaveFormEditorViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given empty string, verify it should not invoke repository method`() = runTest {
        coEvery { mockRepository.getWaveFormList(any()) } returns SAMPLE_WAVES_LIST
        viewModel.importFile(uriString = "")
        coVerify(exactly = 0) { mockRepository.getWaveFormList(any()) }
    }

    @Test
    fun `Given valid URI, verify the updated state `() = runTest {
        coEvery { mockRepository.getWaveFormList(SAMPLE_URI_STRING) } returns SAMPLE_WAVES_LIST

        viewModel.importFile(uriString = SAMPLE_URI_STRING)

        coVerify { mockRepository.getWaveFormList(SAMPLE_URI_STRING) }
        viewModel.pointsFlow.value.run {
            assertEquals(waves, SAMPLE_WAVES_LIST)
            assertTrue(importButtonEnabled)
            assertTrue(exportButtonEnabled)
            assertFalse(isLoading)
            assertEquals(markerAIndex, 0)
            assertEquals(markerBIndex, 2)
        }
    }

    @Test
    fun `Given invalid markers, verify the state is not updated`() = runTest {
        coEvery { mockRepository.getWaveFormList(SAMPLE_URI_STRING) } returns SAMPLE_WAVES_LIST

        viewModel.importFile(uriString = SAMPLE_URI_STRING)
        viewModel.updateMarkers(markers = -1 to 3)

        coVerify { mockRepository.getWaveFormList(SAMPLE_URI_STRING) }
        viewModel.pointsFlow.value.run {
            assertEquals(waves, SAMPLE_WAVES_LIST)
            assertTrue(markerAIndex != -1)
            assertTrue(markerBIndex != 3)
        }
    }

    @Test
    fun `Given valid updated markers, verify the state is not updated`() = runTest {
        coEvery { mockRepository.getWaveFormList(SAMPLE_URI_STRING) } returns SAMPLE_WAVES_LIST

        viewModel.importFile(uriString = SAMPLE_URI_STRING)
        viewModel.updateMarkers(markers = 0 to 1)

        coVerify { mockRepository.getWaveFormList(SAMPLE_URI_STRING) }
        viewModel.pointsFlow.value.run {
            assertEquals(SAMPLE_WAVES_LIST, waves)
            assertEquals(0, markerAIndex)
            assertEquals(1, markerBIndex)
        }
    }

    @Test
    fun `Given empty waves, verify saveTrimmedFileToDownloads return Failure`() = runTest {
        coEvery { mockRepository.getWaveFormList(SAMPLE_URI_STRING) } returns SAMPLE_WAVES_LIST
        coEvery { mockRepository.saveWaveFormFileToDownloads(any()) } returns Success(
            SAMPLE_URI_STRING
        )

        val result = viewModel.saveTrimmedFileToDownloads()

        coVerify(exactly = 0) { mockRepository.getWaveFormList(SAMPLE_URI_STRING) }
        assertTrue(viewModel.pointsFlow.value.waves.isEmpty())
        assertTrue(result is Failure)
    }

    @Test
    fun `Given valid state, verify saveTrimmedFileToDownloads return Success`() = runTest {
        coEvery { mockRepository.getWaveFormList(SAMPLE_URI_STRING) } returns SAMPLE_WAVES_LIST
        coEvery { mockRepository.saveWaveFormFileToDownloads(any()) } returns Success(
            SAMPLE_OUTPUT_URI_STRING
        )

        viewModel.importFile(uriString = SAMPLE_URI_STRING)
        val result = viewModel.saveTrimmedFileToDownloads()

        coVerify { mockRepository.getWaveFormList(SAMPLE_URI_STRING) }
        assertFalse(viewModel.pointsFlow.value.waves.isEmpty())
        assertTrue(result is Success)
        assertEquals(SAMPLE_OUTPUT_URI_STRING, (result as Success).value)
    }

    companion object {
        private const val SAMPLE_URI_STRING = "content://Downloads/input.txt"
        private const val SAMPLE_OUTPUT_URI_STRING = "content://Downloads/output.txt"
        private val SAMPLE_WAVES_LIST = listOf(
            0.5 to -0.25,
            0.3 to -0.15,
            1.0 to -1.0
        )
    }
}
