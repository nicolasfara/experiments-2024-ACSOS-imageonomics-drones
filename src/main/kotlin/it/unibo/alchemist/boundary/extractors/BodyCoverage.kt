package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.actions.CameraSeeWithBlindSpot
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.experiment.toBoolean
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class BodyCoverage(
    private val visionMolecule: Molecule,
    private val targetMolecule: Molecule,
) : AbstractDoubleExporter() {

    override val columnNames: List<String> = listOf("BodyCoverage")

    private val bodyLength = SimpleMolecule("bodyLength")
    private val bodyWidth = SimpleMolecule("bodyWidth")

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        // cast environment to Physics2DEnvironment, and then use getHeading to get the directions of the nodes
        // val nodes = environment.nodes
        // val cameraNodes = nodes.filter { it.isCamera() }
        // val targetNodes = nodes.filter { it.isTarget() }
        // val visibleTargets = cameraNodes.flatMap { it.getVisibleTargets() }.toSet()
        TODO("Not yet implemented")
    }

    private fun <T> VisibleNode<*, *>.computeMetric(environment: Environment<T, *>, cameraNodes: List<Node<*>>): Double {
        camerasReachingBody(cameraNodes).map { // camera ->
            // Suppress("UNCHECKED_CAST")
            // val cameraPoint = environment.getPosition(camera as Node<T>).coordinates
        }
        return environment.getDimensions().toDouble()
        //TODO()
    }

    private fun VisibleNode<*, *>.camerasReachingBody(cameraNodes: List<Node<*>>): List<Node<*>> {
        return cameraNodes.filter { it.getVisibleTargets().contains(this) }
    }

    /**
     * Ramanujan's approximation of the perimeter of an ellipse.
     */
    private fun VisibleNode<*, *>.perimeter(): Double {
        val x = this.position.coordinates[0] / 2
        val y = this.position.coordinates[1] / 2

        val t = ((x - y) / (x + y)).pow(2)
        return PI * (x + y) * (1 + (3 * t) / (10 + sqrt(4 - 3 * t)))
    }

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

    private fun Node<*>.isCamera() = contains(visionMolecule)

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