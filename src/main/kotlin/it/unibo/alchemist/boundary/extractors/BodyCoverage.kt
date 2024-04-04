package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.experiment.toBoolean

class BodyCoverage(
    private val visionMolecule: Molecule,
    private val targetMolecule: Molecule,
    private val bodyLength: Double,
    private val bodyWidth: Double,
    private val bodySegments: Int,
) : AbstractDoubleExporter() {

    private val bodyCoverageColumnName = "BodyCoverage"
    override val columnNames: List<String> = listOf(bodyCoverageColumnName)

    private val metricCalculator = BodyCoverageMetricCalculator(bodyLength, bodyWidth, bodySegments)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        require(environment is Physics2DEnvironment) {
            "Expected a Physics2DEnvironment but got ${environment::class}"
        }
        val nodes = environment.nodes
        val cameraNodes: List<Node<T>> = nodes.filter { it.isCamera() }
        val targetNodes = nodes.filter { it.isTarget() }
        val visibleTargets = cameraNodes.flatMap { it.getVisibleTargets() }.toSet()
        val sum = visibleTargets.sumOf { visibleNode ->
            val nodePosition = environment.getPosition(visibleNode.node)
            val nodeAngle = environment.getHeading(visibleNode.node)
            metricCalculator.computeMetricForNode(
                nodePosition to nodeAngle,
                cameraNodes.filter { it.getVisibleTargets().contains(visibleNode) }.map { environment.getPosition(it) }
            )
        }
        return mapOf(bodyCoverageColumnName to sum / targetNodes.size)
    }

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

    private fun Node<*>.isCamera() = contains(visionMolecule)

    private fun <T> Node<T>.getVisibleTargets() =
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
}