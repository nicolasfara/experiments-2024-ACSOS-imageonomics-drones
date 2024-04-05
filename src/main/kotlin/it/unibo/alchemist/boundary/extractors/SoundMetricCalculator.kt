package it.unibo.alchemist.boundary.extractors

import kotlin.math.log10
import kotlin.math.pow

class SoundMetricCalculator {
    fun sumOfSoundPressures(decibelPerceived: List<Double>): Double {
        val summedSoundPressions = decibelPerceived.sumOf { 10.0.pow(it / 10) }
        return 10.0 * log10(summedSoundPressions)
    }

    fun dispersionOfSoundDecibel(
        decibelEmitted: Double,
        distanceMeasurementFromSource: Double,
        distance: Double,
    ): Double {
        val resultingDecibel = decibelEmitted + 20.0 * log10(distanceMeasurementFromSource / distance)
        return if (resultingDecibel < 0.0) 0.0 else resultingDecibel
    }

    fun perceivedSoundLevelForZebras(decibel: Double, threshold: Double): Double =
        if (decibel < threshold) 0.0 else decibel
}
