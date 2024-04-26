package it.unibo.alchemist.model.actions

import it.unibo.alchemist.boundary.extractors.BodyCoverage
import it.unibo.alchemist.boundary.extractors.CameraWithBlindSpot
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.nodes.VisibleNodeImpl
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

/**
 * Checks nodes in the [environment] and writes in [outputMolecule]
 * the list of [it.unibo.alchemist.model.VisibleNode], containing [filterByMolecule].
 * @param node owner of the action
 * @param blindSpotDistance radius of the blind spot inside FoV.
 * @param distance radius of the FoV.
 * @param angle of the FoV.
 * @param outputMolecule for visible nodes.
 * @param filterByMolecule allows to consider only target nodes.
 */
class CameraCaptureAnimals @JvmOverloads constructor(
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val outputMolecule: Molecule = SimpleMolecule("vision"),
    private val filterByMolecule: Molecule? = null,
) : AbstractAction<Any>(node) {

    private val fieldOfView = node.properties.filterIsInstance<CameraWithBlindSpot<Any>>().firstOrNull()
        ?: error("Property ${CameraCaptureAnimals::class} not found.")

    /**
     * used by GUI to draw clusters.
     */
    var seenTargets: List<Node<Any>> = emptyList()

    init {
        node.setConcentration(outputMolecule, emptyList<Any>())
    }

    fun isVisible(point: Euclidean2DPosition): Boolean {
        return fieldOfView.contains(point)
    }

    override fun cloneAction(node: Node<Any>, reaction: Reaction<Any>) =
        CameraCaptureAnimals(node, environment, outputMolecule, filterByMolecule)

    override fun execute() {
        var seen = fieldOfView.influentialNodes()
        filterByMolecule?.run {
            seen = seen.filter { it.contains(filterByMolecule) }
        }
        seenTargets = seen
        node.setConcentration(outputMolecule, seen.map { VisibleNodeImpl(it, environment.getPosition(it)) })
    }

    override fun getContext() = Context.LOCAL
}
