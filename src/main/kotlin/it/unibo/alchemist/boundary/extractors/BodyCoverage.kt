package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.experiment.toBoolean

class BodyCoverage<T>(
    private val environment: Environment<T, *>,
    override val node: Node<T>,
    visionMoleculeName: String,
    targetMoleculeName: String,
    bodyLength: Double,
    bodyWidth: Double,
    bodySegments: Int,
) : NodeProperty<T> {
    private val metricCalculator = BodyCoverageMetricCalculator(bodyLength, bodyWidth, bodySegments)
    private val visionMolecule by lazy { SimpleMolecule(visionMoleculeName) }
    private val targetMolecule by lazy { SimpleMolecule(targetMoleculeName) }

    companion object {
        val bodyCoverageMolecule = SimpleMolecule("BodyCoverage")
        val bodyCoverageOnlyCovered = SimpleMolecule("BodyCoverageOnlyCovered")
    }

    fun computeBodyCoverageMetric(whenNotCovered: Double): Double {
        require(environment is Physics2DEnvironment) {
            "Expected a Physics2DEnvironment but got ${environment::class}"
        }
        val nodes = environment.nodes
        val visibleCameras = node.getVisibleCameras(nodes, visionMolecule)
        return metricCalculator.computeMetricForNode(
            environment.getPosition(node) to environment.getHeading(node),
            visibleCameras.map { environment.getPosition(it) },
            whenNotCovered,
        )
    }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = TODO("Not yet implemented")
}