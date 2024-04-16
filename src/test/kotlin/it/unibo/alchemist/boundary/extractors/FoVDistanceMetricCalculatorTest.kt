package it.unibo.alchemist.boundary.extractors

import org.junit.jupiter.api.Assertions.*
import org.locationtech.jts.geom.Coordinate
import kotlin.test.Test

class FoVDistanceMetricCalculatorTest {
    companion object {
        val center = Coordinate(0.0, 0.0)
        val anotherCenter = Coordinate(-1.0, -1.0)
        val farthestPoint = Coordinate(1.0, 1.0)
        val metric = CentroidQualityMetricCalculator()
    }
    @Test
    fun `the centroid quality metric should be 1 when the node is placed at the center`() {
        val cameras = listOf(Pair(center, farthestPoint))
        val animalPosition = center
        val qualityMetric = metric.computeQualityMetric(animalPosition, cameras)
        assertEquals(1.0, qualityMetric, 0.0)
    }
    @Test
    fun `the centroid quality metric should be 0 when the node is placed at the farthest point`() {
        val cameras = listOf(Pair(center, farthestPoint))
        val animalPosition = farthestPoint
        val qualityMetric = metric.computeQualityMetric(animalPosition, cameras)
        assertEquals(0.0, qualityMetric, 0.0)
    }
    @Test
    fun `the centroid quality metric should take the maximum values from the cameras`() {
        val cameras = listOf(Pair(center, farthestPoint), Pair(anotherCenter, farthestPoint))
        val animalPosition = center
        val qualityMetric = metric.computeQualityMetric(animalPosition, cameras)
        assertEquals(1.0, qualityMetric, 0.0)
    }
}
