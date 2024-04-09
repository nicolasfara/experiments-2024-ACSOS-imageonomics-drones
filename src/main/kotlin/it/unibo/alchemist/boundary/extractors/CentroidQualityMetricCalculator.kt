package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.locationtech.jts.algorithm.Centroid
import org.locationtech.jts.awt.ShapeReader
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.math.Vector2D

class CentroidQualityMetricCalculator {
    private val geometry by lazy { GeometryFactory() }
    private val flatness = 1.0

    fun computeQualityMetric(animalPosition: Euclidean2DPosition, cameras: List<CameraWithBlindSpot<*>>): Double {
        return 0.0
    }


    private fun CameraWithBlindSpot<*>.geometryRepresentation(): Geometry =
        ShapeReader.read(transformShapeToEnvironmentPosition(), flatness, geometry)

    private fun CameraWithBlindSpot<*>.centroid(): Coordinate =
        Centroid.getCentroid(geometryRepresentation())
    private fun CameraWithBlindSpot<*>.worstCaseCoordinateVector(): Vector2D {
        val cameraShape = this.geometryRepresentation()
        val centroid = this.centroid()
        return cameraShape.coordinates.map { it to it.distance(centroid) }
            .maxByOrNull { it.second }
                ?.let { (coordinate, _) -> Vector2D(coordinate.x - centroid.x, coordinate.y - centroid.y) }
                ?: error("It should have at least one coordinate")
    }

    private fun CameraWithBlindSpot<*>.metricForCamera(animalPosition: Euclidean2DPosition): Double {
        val worstCaseVector = worstCaseCoordinateVector()
        val animalVector = Vector2D(animalPosition.x - centroid().x, animalPosition.y - centroid().y)
        return worstCaseVector.angleTo(animalVector)
    }
}