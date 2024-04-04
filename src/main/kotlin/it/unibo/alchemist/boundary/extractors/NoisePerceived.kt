package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.experiment.clustering.HerdExperimentUtils
import it.unibo.experiment.toBoolean
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

class NoisePerceived(
    private val decibel: Double,
    private val droneHeight: Double,
    private val hearingDecibel: Double,
): AbstractDoubleExporter() {

    private val targetMolecule = SimpleMolecule("zebra")
    private val visionMolecule = SimpleMolecule("vision")
    private val droneMolecule = SimpleMolecule("drone")

    private val noisePerceivedColumnName = "NoisePerceived"
    override val columnNames: List<String> = listOf(noisePerceivedColumnName)

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
                    dispersionOfSoundDecibel(distanceHypoten)
                }.map {
                    perceivedSoundLevelForZebras(it, hearingDecibel)
                }
                sumOfSoundPressures(pressures)
            }
            .average()
        return mapOf(noisePerceivedColumnName to averageNoiseForZebras)
    }

    private fun sumOfSoundPressures(decibelPerceived: List<Double>): Double {
        val summedSoundPressions = decibelPerceived.sumOf { 10.0.pow(it / 10) }
        return 10.0 * log10(summedSoundPressions)
    }

    private fun dispersionOfSoundDecibel(distance: Double): Double {
        return decibel + 20.0.times(log10(1.0.div(distance)))
    }

    private fun perceivedSoundLevelForZebras(decibel: Double, threshold: Double): Double =
        if (decibel < threshold) 0.0 else decibel

    private fun <T> Environment<T, *>.distanceCameraToZebra(camera: Node<T>, zebra: Node<T>, height: Double): Double {
        val onGroundDistance = getDistanceBetweenNodes(camera, zebra)
        return hypot(onGroundDistance, height)
    }

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

    private fun Node<*>.getVisibleTargets() =
        with(getConcentration(visionMolecule)) {
            require(this is List<*>) { "Expected a List but got $this of type ${this?.javaClass}" }
            if (!isEmpty()) {
                get(0)?.also {
                    require(it is VisibleNode<*, *>) {
                        "Expected a List<VisibleNode> but got List<${it::class}> = $this"
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            (this as Iterable<VisibleNode<*, *>>).filter { it.node.isTarget() }
        }

}
