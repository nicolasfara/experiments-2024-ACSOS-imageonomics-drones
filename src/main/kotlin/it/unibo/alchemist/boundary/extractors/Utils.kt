package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.VisibleNode
import it.unibo.experiment.toBoolean
import kotlin.math.PI
import kotlin.math.pow


fun Node<*>.isTarget(targetMolecule: Molecule) =
    contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

fun Node<*>.isCamera(visionMolecule: Molecule) = contains(visionMolecule)

fun <T> Node<T>.getVisibleTargets(visionMolecule: Molecule, targetMolecule: Molecule) =
    with(getConcentration(visionMolecule)) {
        require(this is List<*>) { "Expected a List but got $this" }
        if (isNotEmpty()) {
            get(0)?.also {
                require(it is VisibleNode<*, *>) {
                    "Expected a List<VisibleNode> but got List<${it::class}> = $this"
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        (this as Iterable<VisibleNode<T, *>>).filter { it.node.isTarget(targetMolecule) }
    }

fun <T> Node<T>.getVisibleCameras(nodes: List<Node<T>>, visionMolecule: Molecule, targetMolecule: Molecule) =
    nodes.filter { n ->
        n.isCamera(visionMolecule) &&
        n.getVisibleTargets(visionMolecule, targetMolecule).map { it.node }.contains(this)
    }

fun normalizationFunctionForAngle(angle: Double): Double {
    val normalizedValue = angle * 2 / PI
    return sigmoid(normalizedValue, 0.35, 5.0)
}

fun normalizationFunctionForRange(value: Double, min: Double, max: Double): Double {
    return sigmoid(
        linearizationFunction(value, min, max),
        0.40,
        4.0,
    )
}

fun linearizationFunction(value: Double, min: Double, max: Double) = when {
    value >= max -> 1.0 // If  value is greater than MAX then fix it to 1.0
    else -> (value - min) / (max - min) // Change range into [0..1]
}

fun sigmoid(value: Double, m: Double, v: Double): Double {
    return (1 + ((value * (1 - m)) / (m * (1 - value))).pow(-v)).pow(-1)
}
