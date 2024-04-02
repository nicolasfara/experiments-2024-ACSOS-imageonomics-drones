package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.util.AffineTransformation

class BodyCoverageMetricCalculator(private val bodyLength: Double, private val bodyWidth: Double) {
    private val bodyPerimeter by lazy { 2 * (bodyLength + bodyWidth) }

    fun computeMetricForNode(
        bodyPositionAndAngle: Pair<Euclidean2DPosition, Euclidean2DPosition>,
        camerasPositionsAndAngles: List<Pair<Euclidean2DPosition, Euclidean2DPosition>>
    ): Double {
//        val bodyCoordinate = Coordinate(bodyPositionAndAngle.first.x, bodyPositionAndAngle.first.y)
//        val body = createRectangle(bodyWidth, bodyLength, bodyCoordinate, bodyPositionAndAngle.second)
//        val cameras = camerasPositionsAndAngles.map {
//
//        }

//        val normalizedCameraPositions = camerasPositionsAndAngles.toPositionsAndAngles().map {
//            it.rotoTranslateRespectTo(bodyPositionAndAngle.toPositionAndAngle())
//        }
//        return normalizedCameraPositions.sumOf {
//            when (it.angle) {
//                0.0, 180.0 -> bodyWidth
//                90.0, 270.0 -> bodyLength
//                else -> getWeightedVisiblePerimeter(it)
//            }
//        } / bodyPerimeter
        TODO()
    }

    private fun createBody(position: Euclidean2DPosition, angle: Euclidean2DPosition): Geometry {
        val factory = GeometryFactory()
        val coordinates = arrayOf(
            Coordinate(-bodyWidth / 2, bodyLength / 2),
            Coordinate(bodyWidth / 2, bodyLength / 2),
            Coordinate(bodyWidth / 2, -bodyLength / 2),
            Coordinate(-bodyWidth / 2, -bodyLength / 2),
            Coordinate(-bodyWidth / 2, bodyLength / 2)
        )
        val rectangle = factory.createPolygon(coordinates)
        val rotation = AffineTransformation().rotate(angle.asAngle, 0.0, 0.0)
        val translation = AffineTransformation().translate(position.x, position.y)
        val rotated = rotation.transform(rectangle)
        return translation.transform(rotated)
    }

    private fun getWeightedVisiblePerimeter(cameraPosition: PositionAndAngle): Double {
        val topRightCorner = Euclidean2DPosition(bodyWidth / 2, bodyLength / 2)
        val bottomRightCorner = Euclidean2DPosition(bodyWidth / 2, -bodyLength / 2)
        val bottomLeftCorner = Euclidean2DPosition(-bodyWidth / 2, -bodyLength / 2)
        val topLeftCorner = Euclidean2DPosition(-bodyWidth / 2, bodyLength / 2)
        val segments = listOf(
            topRightCorner to bottomRightCorner,
            bottomRightCorner to bottomLeftCorner,
            bottomLeftCorner to topLeftCorner,
            topLeftCorner to topRightCorner
        )
        return segments.asSequence()
            .map { it to it.first.distanceTo(cameraPosition.position) + it.second.distanceTo(cameraPosition.position) }
            .sortedBy { it.second }
            .take(2)
            .map { (points, _) -> points.first.distanceTo(points.second) }
            .sum() / bodyPerimeter
    }

    private fun Pair<Euclidean2DPosition, Euclidean2DPosition>.isVertical(): Boolean {
        val (p1, p2) = this
        return p1.coordinates[0] == p2.coordinates[0]
    }

    private fun Pair<Euclidean2DPosition, Euclidean2DPosition>.midPoint(): Euclidean2DPosition {
        val (first, second) = this
        return Euclidean2DPosition((first.x + second.x) / 2, (first.y + second.y) / 2)
    }

    private fun PositionAndAngle.translateRespectTo(point: Euclidean2DPosition): PositionAndAngle {
        val translatedPosition = position.minus(point)
        return PositionAndAngle(translatedPosition, angle)
    }

    private fun PositionAndAngle.rotoTranslateRespectTo(point: PositionAndAngle): PositionAndAngle {
        val normalizedPosition = position.minus(point.position)
        val normalizedAngle = angle - point.angle
        return PositionAndAngle(normalizedPosition, normalizedAngle)
    }
}

private data class PositionAndAngle(val position: Euclidean2DPosition, val angle: Double)

private fun Pair<Euclidean2DPosition, Double>.toPositionAndAngle() = PositionAndAngle(first, second)
private fun List<Pair<Euclidean2DPosition, Double>>.toPositionsAndAngles() = map { it.toPositionAndAngle() }
