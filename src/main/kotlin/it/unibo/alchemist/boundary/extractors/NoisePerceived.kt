package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.experiment.toBoolean
import kotlin.math.*

class NoisePerceived(
    private val decibelEmitted: Double,
    private val distanceMeasurementFromSource: Double,
    private val droneHeight: Double,
    private val hearingThreshold: Double,
): AbstractDoubleExporter() {

    private val targetMolecule = SimpleMolecule("zebra")
    private val droneMolecule = SimpleMolecule("drone")

    private val noisePerceivedColumnName = "NoisePerceived"
    override val columnNames: List<String> = listOf(noisePerceivedColumnName)

    private val soundMetricCalculator = SoundMetricCalculator()

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Double> {
        val drones = environment.nodes
            .filter { it.contains(droneMolecule) }
        val averageNoiseForZebras = environment.nodes
            .filter { it.isTarget() }
            .map { zebra ->
                val pressures = drones.map { drone ->
                    val distanceHypoten = environment.distanceCameraToZebra(zebra, drone, droneHeight)
                    soundMetricCalculator.dispersionOfSoundDecibel(
                        decibelEmitted,
                        distanceMeasurementFromSource,
                        distanceHypoten,
                    )
                }.map {
                    soundMetricCalculator.perceivedSoundLevelForZebras(it, hearingThreshold)
                }
                soundMetricCalculator.sumOfSoundPressures(pressures)
            }
            .average()
        return mapOf(noisePerceivedColumnName to averageNoiseForZebras)
    }

    private fun <T> Environment<T, *>.distanceCameraToZebra(camera: Node<T>, zebra: Node<T>, height: Double): Double {
        val onGroundDistance = getDistanceBetweenNodes(camera, zebra)
        return hypot(onGroundDistance, height)
    }

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()
}
