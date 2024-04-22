package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.experiment.toBoolean
import kotlin.math.*

class NoisePerceived<T>(
    override val node: Node<T>,
    private val environment: Environment<T, *>,
    // Parameters for sound computation
    private val decibelEmitted: Double,
    private val distanceMeasurementFromSource: Double,
    private val droneHeight: Double,
    private val hearingThreshold: Double,
): NodeProperty<T> {

    companion object {
        val noisePerceivedMolecule = SimpleMolecule("NoisePerceived")
        val noisePerceivedMoleculeNormalized = SimpleMolecule("NoisePerceivedNormalized")
    }

    private val droneMolecule = SimpleMolecule("drone")
    private val soundMetricCalculator = SoundMetricCalculator()

    fun computeSoundMetric(): Pair<Double, Double> {
        val drones = environment.nodes.filter { it.contains(droneMolecule) }

        val pressures = drones.map { drone ->
            val distanceHypoten = environment.distanceCameraToZebra(node, drone, droneHeight)
            soundMetricCalculator.dispersionOfSoundDecibel(
                decibelEmitted,
                distanceMeasurementFromSource,
                distanceHypoten,
            )
        }
        val perceivedDb = soundMetricCalculator.perceivedSoundLevelForZebras(
            soundMetricCalculator.sumOfSoundPressures(pressures),
            hearingThreshold,
        )
        return perceivedDb to normalizationFunctionForRange(perceivedDb, hearingThreshold, 80.0)
    }

    private fun <T> Environment<T, *>.distanceCameraToZebra(camera: Node<T>, zebra: Node<T>, height: Double): Double {
        val onGroundDistance = getDistanceBetweenNodes(camera, zebra)
        return hypot(onGroundDistance, height)
    }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = this
}
