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
    }

    fun computeBodyCoverageMetric(): Double {
        require(environment is Physics2DEnvironment) {
            "Expected a Physics2DEnvironment but got ${environment::class}"
        }
        val nodes = environment.nodes
        val visibleCameras = nodes.filter { n -> n.isCamera() && n.getVisibleTargets().map { it.node }.contains(node) }
        return metricCalculator.computeMetricForNode(
            environment.getPosition(node) to environment.getHeading(node),
            visibleCameras.map { environment.getPosition(it) }
        )
    }

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

    private fun Node<*>.isCamera() = contains(visionMolecule)

    private fun Node<T>.getVisibleTargets() =
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
            (this as Iterable<VisibleNode<T, *>>).filter { it.node.isTarget() }
        }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = TODO("Not yet implemented")
}