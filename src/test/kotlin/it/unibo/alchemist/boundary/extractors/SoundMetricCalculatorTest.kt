package it.unibo.alchemist.boundary.extractors

import kotlin.test.Test
import org.junit.jupiter.api.Assertions.*
import java.math.RoundingMode
import kotlin.math.roundToInt

class SoundMetricCalculatorTest {
    val soundMetricCalculator = SoundMetricCalculator()

    @Test
    fun `SoundMetricCalculator calculates the correct value for SoundPressureLevel dispersion`() {
        val sourceDb = 90.0
        val distances = listOf(2.0, 4.0)
        val expectedDb = listOf(84, 78)
        distances.forEachIndexed { index, element ->
            assertEquals(
                soundMetricCalculator.dispersionOfSoundDecibel(sourceDb, 1.0, element).roundToInt(),
                expectedDb[index],
            )
        }
    }

    @Test
    fun `Test SoundPressureLevel threshold`() {
        val threshold = 20.0
        assertEquals(soundMetricCalculator.perceivedSoundLevelForZebras(90.0, threshold), 90.0)
        assertEquals(soundMetricCalculator.perceivedSoundLevelForZebras(17.0, threshold), 0.0)
    }

    @Test
    fun `Test Sum of SoundPressureLevels`() {
        val values = listOf(80.3, 83.0, 84.0)
        assertEquals(
            soundMetricCalculator.sumOfSoundPressures(values)
                .toBigDecimal().setScale(1, RoundingMode.UP).toDouble(),
            87.5
        )
    }

    @Test
    fun `Evaluation of metric results`() {
        val pressures = listOf(10.0, 50.0, 100.0, 500.0)
            .map {
                soundMetricCalculator.dispersionOfSoundDecibel(
                    73.0,
                    1.0,
                    it,
                )
            }.map {
                soundMetricCalculator.perceivedSoundLevelForZebras(it, 20.0)
            }
        println(pressures)
        println(soundMetricCalculator.sumOfSoundPressures(pressures))
    }
}
