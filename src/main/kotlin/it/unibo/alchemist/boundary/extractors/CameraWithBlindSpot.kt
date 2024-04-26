package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import kotlin.math.atan2

class CameraWithBlindSpot<T>(
    val environment: Physics2DEnvironment<T>,
    override val node: Node<T>,
    val blindSpotDistance: Double,
    val fovDistance: Double,
    val aperture: Double,
): NodeProperty<T> {
    private val fovShape = environment.shapeFactory.circleSector(fovDistance, Math.toRadians(aperture), 0.0)
    private val blindSpotShape = environment.shapeFactory.circleSector(blindSpotDistance, Math.toRadians(aperture), 0.0)

    init {
        require(blindSpotDistance in 0.0..fovDistance)
    }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = CameraWithBlindSpot(environment, node, blindSpotDistance, fovDistance, aperture)

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

    fun transformShapeToEnvironmentPosition(): Shape {
        val heading = environment.getHeading(node)
        val affineTransform = AffineTransform()
        affineTransform.translate(environment.getPosition(node).x, -environment.getPosition(node).y)
        affineTransform.rotate(-atan2(heading.y, heading.x))
        return affineTransform.createTransformedShape(shapeRepresentation())
    }

    fun contains(point: Euclidean2DPosition): Boolean {
        val intersectFoV = fovShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        }.contains(point)
        val intersectBlindSpot = blindSpotShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        }.contains(point)
        return intersectFoV && !intersectBlindSpot
    }
    private fun nodesInBlindSpot(): List<Node<T>> = environment.getNodesWithin(
        blindSpotShape.transformed {
            origin(environment.getPosition(node))
            rotate(environment.getHeading(node))
        },
    )
}