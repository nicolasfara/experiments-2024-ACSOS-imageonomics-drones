package it.unibo.alchemist.boundary.extractors

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.math.Vector2D

typealias CameraQualityInformation = Pair<Coordinate, Coordinate>
fun CameraQualityInformation.centroid(): Coordinate = this.first
fun CameraQualityInformation.furthestPoint(): Coordinate = this.second
fun CameraQualityInformation.worstCaseCoordinateVector(): Vector2D =
    Vector2D(this.furthestPoint().x - this.centroid().x, this.furthestPoint().y - this.centroid().y)
class CentroidQualityMetricCalculator {
    fun computeQualityMetric(
        animalPosition: Coordinate,
        cameras: List<CameraQualityInformation>,
        whenNotCovered: Double,
    ): Double =  cameras.maxOfOrNull { it.metricForCamera(animalPosition) } ?: whenNotCovered


    private fun CameraQualityInformation.metricForCamera(animalPosition: Coordinate): Double {
        val worstCaseVector = this.worstCaseCoordinateVector()
        val animalVector = Vector2D(animalPosition.x - this.centroid().x, animalPosition.y - this.centroid().y)
        return (worstCaseVector.length() - animalVector.length()) / worstCaseVector.length()
    }
}