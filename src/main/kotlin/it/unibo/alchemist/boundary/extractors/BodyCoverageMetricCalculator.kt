package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.math.Vector2D
import kotlin.math.PI

class BodyCoverageMetricCalculator(private val bodyLength: Double, private val bodyWidth: Double) {
    fun computeMetricForNode(
        bodyPositionAndAngle: Pair<Euclidean2DPosition, Euclidean2DPosition>,
        camerasPositionsAndAngles: List<Pair<Euclidean2DPosition, Euclidean2DPosition>>
    ): Double {
        val body = createBody(bodyPositionAndAngle.first, bodyPositionAndAngle.second)
        val cameras = camerasPositionsAndAngles.map {
            Coordinate(it.first.x, it.first.y) to Vector2D.create(it.second.x, it.second.y)
        }

        val res = cameras.flatMap { visibleSidesFromCamera(body, it.first) }
        val sideAndAngle = res.groupBy({ it.first }, { it.second }).mapValues { it.value.max() }
        require(sideAndAngle.size <= 4) { "At most 4 sides can be detected" }
        return sideAndAngle.map { (side, angle) -> side.length * normalizationFunction(angle) }.sum() / body.length
    }

    private fun normalizationFunction(angle: Double): Double {
        return angle * 2 / PI
    }

    private fun visibleSidesFromCamera(rectangle: Geometry, cameraPosition: Coordinate): List<Pair<LineString, Double>> {
        val rectangleCoordinates = rectangle.coordinates
        val segments = (0 until rectangleCoordinates.size - 1).mapNotNull {
            val p1 = rectangleCoordinates[it]
            val p2 = rectangleCoordinates[it + 1]
            GeometryFactory().createLineString(arrayOf(p1, p2))
        }.filter { isSegmentPerpendicularToPoint(it, cameraPosition) }
        return when {
            segments.size == 2 -> {
                val res = segments.map {
                    it to it.coordinates[0].distance(cameraPosition) + it.coordinates[1].distance(cameraPosition)
                }.minByOrNull { it.second }!!.first to (PI / 2)
                listOf(res)
            }
            else -> {
                (0 until rectangle.coordinates.size - 1).mapNotNull {
                    val p1 = rectangleCoordinates[it]
                    val p2 = rectangleCoordinates[it + 1]
                    val side = GeometryFactory().createLineString(arrayOf(p1, p2))
                    val angle = Angle.angleBetween(p1, p2, cameraPosition)
                    (side to angle) to
                        side.coordinates[0].distance(cameraPosition) + side.coordinates[1].distance(cameraPosition)
                }.sortedBy { it.second }.take(2).map { it.first }
            }
        }
    }

    private fun LineString.midPoint(): Coordinate {
        val p1 = getCoordinateN(0)
        val p2 = getCoordinateN(1)
        return Coordinate((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
    }

    private fun isSegmentPerpendicularToPoint(segment: LineString, point: Coordinate): Boolean {
        val p1 = segment.getCoordinateN(0)
        val midPoint = segment.midPoint()
        return Angle.angleBetween(p1, midPoint, point) == PI / 2
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
}
