package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.util.GeometricShapeFactory
import kotlin.math.PI
import kotlin.math.pow

class BodyCoverageMetricCalculator(
    private val bodyLength: Double,
    private val bodyWidth: Double,
    private val segments: Int,
) {
    private val geometryFactory by lazy { GeometryFactory() }

    fun computeMetricForNode(
        bodyPositionAndAngle: Pair<Euclidean2DPosition, Euclidean2DPosition>,
        camerasPositionsAndAngles: List<Euclidean2DPosition>,
        whenNotCovered: Double,
    ): Double {
        val body = createApproximatedEllipseForBody(
            bodyPositionAndAngle.first,
            bodyPositionAndAngle.second,
            bodyWidth,
            bodyLength,
            segments,
        )
        val cameras = camerasPositionsAndAngles.map { Coordinate(it.x, it.y) }
        if (cameras.isEmpty()) {
            return whenNotCovered
        }

        val segmentsAndAngles = cameras.flatMap { visibleSidesFromCamera(body, it) }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, angles) -> angles.max() }

        require(segmentsAndAngles.size <= segments) { "At most $segments sides can be detected" }
        return segmentsAndAngles.map {
            (side, angle) -> side.length * normalizationFunctionForAngle(angle)
        }.sum() / body.length
    }



    private fun visibleSidesFromCamera(body: Geometry, cameraPosition: Coordinate): List<Pair<LineString, Double>> {
        val convexHull = body.convexHull()
        if (convexHull.contains(geometryFactory.createPoint(cameraPosition))) {
            return emptyList()
        }
        val visibleSegments = (0 until convexHull.coordinates.size - 1).mapNotNull {
            val p1 = convexHull.coordinates[it]
            val p2 = convexHull.coordinates[it + 1]
            val segment1 = geometryFactory.createLineString(arrayOf(p1, cameraPosition))
            val segment2 = geometryFactory.createLineString(arrayOf(p2, cameraPosition))
            val intersection1 = segment1.intersection(convexHull)
            val intersection2 = segment2.intersection(convexHull)
            when {
                intersection1.coordinates.size == 1 && intersection1.coordinates[0] == p1 &&
                        intersection2.coordinates.size == 1 && intersection2.coordinates[0] == p2 ->
                    geometryFactory.createLineString(arrayOf(p1, p2))
                else -> null
            }
        }
        return visibleSegments.map { ls ->
            val p1 = ls.getCoordinateN(0)
            val p2 = ls.getCoordinateN(1)
            val midPoint = (p1 to p2).midPoint()
            val sortedPoint = listOf(p1, p2) // Used to determine the farthest point from the camera
                .map { it to it.distance(cameraPosition) }
                .sortedBy { -it.second }
                .map { it.first }
            require(sortedPoint.size == 2) { "There must be exactly two points" }
            val angleBetweenSegmentAndCamera = Angle.angleBetween(sortedPoint[1], midPoint, cameraPosition)
            geometryFactory.createLineString(arrayOf(p1, p2)) to angleBetweenSegmentAndCamera
        }
    }

    private fun Pair<Coordinate, Coordinate>.midPoint(): Coordinate {
        val (p1, p2) = this
        return Coordinate((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
    }

    private fun createApproximatedEllipseForBody(
        position: Euclidean2DPosition,
        angle: Euclidean2DPosition,
        width: Double,
        height: Double,
        segments: Int,
    ): Geometry {
        val ellipseFactory = GeometricShapeFactory().apply {
            setNumPoints(segments)
            setCentre(Coordinate(position.x, position.y))
            setWidth(width)
            setHeight(height)
        }
        val ellipse = ellipseFactory.createEllipse()
        val rotation = AffineTransformation().rotate(angle.asAngle + (PI / 2), position.x, position.y)
        return rotation.transform(ellipse)
    }
}
