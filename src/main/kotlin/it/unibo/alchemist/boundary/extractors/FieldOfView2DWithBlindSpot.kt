package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

class FieldOfView2DWithBlindSpot<T>(
    private val environment: Physics2DEnvironment<T>,
    override val node: Node<T>,
    val blindSpotDistance: Double,
    val fovDistance: Double,
    val aperture: Double,
): NodeProperty<T> {
    private val fovShape = environment.shapeFactory.circleSector(fovDistance, aperture, 0.0)
    private val blindSpotShape = environment.shapeFactory.circleSector(blindSpotDistance, aperture, 0.0)

    init {
        require(blindSpotDistance in 0.0..fovDistance)
    }
    fun influentialNodes(): List<Node<T>> = environment.getNodesWithin(
        fovShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        },
    ).minus(nodesInBlindSpot().toSet())

    private fun nodesInBlindSpot(): List<Node<T>> = environment.getNodesWithin(
        blindSpotShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        },
    )

    fun contains(point: Euclidean2DPosition): Boolean {
        val pointShape = environment.shapeFactory.circle(1.0).transformed {
            origin(point)
        }
        val intersectFoV = fovShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        }.intersects(pointShape)
        val intersectBlindSpot = blindSpotShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        }.intersects(pointShape)
        return intersectFoV && !intersectBlindSpot
    }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> {
        TODO("Not yet implemented")
    }
}