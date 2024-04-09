package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import kotlin.math.atan2

class CentroidQuality<T>(
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

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = CentroidQuality(environment, node, blindSpotDistance, fovDistance, aperture)

    fun influentialNodes(): List<Node<T>> = environment.getNodesWithin(
        fovShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        },
    ).minus(nodesInBlindSpot().toSet())

    fun shapeRepresentation(): Shape {
        val angle: Double = aperture
        val startAngle = -angle / 2
        val fov: Shape = Arc2D.Double(
            -fovDistance,
            -fovDistance,
            fovDistance * 2,
            fovDistance * 2,
            startAngle,
            angle,
            Arc2D.PIE
        )
        val blindSpotPie: Shape = Arc2D.Double(
            -blindSpotDistance,
            -blindSpotDistance,
            blindSpotDistance * 2,
            blindSpotDistance * 2,
            startAngle,
            angle,
            Arc2D.PIE
        )
        val areaFov = java.awt.geom.Area(fov)
        val areaBlindSpot = java.awt.geom.Area(blindSpotPie)
        areaFov.subtract(areaBlindSpot)
        return areaFov
    }

    fun transformToEnvironmentPosition(): Shape {
        val heading = environment.getHeading(node)
        val affineTransform = AffineTransform()
        affineTransform.rotate(-atan2(heading.y, heading.x))
        return affineTransform.createTransformedShape(shapeRepresentation())
    }

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
    private fun nodesInBlindSpot(): List<Node<T>> = environment.getNodesWithin(
        blindSpotShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        },
    )

}