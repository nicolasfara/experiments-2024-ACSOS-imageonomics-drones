package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.VisibleNode
import it.unibo.experiment.toBoolean


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