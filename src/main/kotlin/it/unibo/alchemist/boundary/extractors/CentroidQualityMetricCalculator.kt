package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.locationtech.jts.algorithm.Centroid
import org.locationtech.jts.awt.ShapeReader
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.math.Vector2D

typealias CameraQualityInformation = Pair<Coordinate, Coordinate>
fun CameraQualityInformation.centroid(): Coordinate = this.first
fun CameraQualityInformation.furthestPoint(): Coordinate = this.second
fun CameraQualityInformation.worstCaseCoordinateVector(): Vector2D =
    Vector2D(this.furthestPoint().x - this.centroid().x, this.furthestPoint().y - this.centroid().y)
class CentroidQualityMetricCalculator {
    fun computeQualityMetric(animalPosition: Coordinate, cameras: List<CameraQualityInformation>): Double =
        cameras.maxOfOrNull { it.metricForCamera(animalPosition) } ?: 0.0

    private fun CameraQualityInformation.metricForCamera(animalPosition: Coordinate): Double {
        val worstCaseVector = this.worstCaseCoordinateVector()
        val animalVector = Vector2D(animalPosition.x - this.centroid().x, animalPosition.y - this.centroid().y)
        return (worstCaseVector.length() - animalVector.length()) / worstCaseVector.length()
    }
}