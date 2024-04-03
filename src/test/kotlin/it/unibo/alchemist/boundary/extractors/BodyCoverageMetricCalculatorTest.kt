package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class BodyCoverageMetricCalculatorTest {
    companion object {
        private const val BODY_LENGTH = 2.0
        private const val BODY_WIDTH = 1.0
    }

//    @Test
//    fun `the metric for 4 perpendicular cameras should return 1`() {
//        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH, 4)
//        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to Euclidean2DPosition(0.0, 0.0)
//        val camerasPositions = listOf(
//            Euclidean2DPosition(0.6, 0.35), // pointing to the left
//            Euclidean2DPosition(-0.6, 0.35), // pointing to the right
//            Euclidean2DPosition(-0.6, -0.35), // pointing down
//            Euclidean2DPosition(0.6, -0.35), // pointing up
//        )
//        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
//        assertEquals(1.0, result, 0.03)
//    }
//
//    @Test
//    fun `the metric for 1 perpendicular camera on short side should return the ratio between the short side and the perimeter`() {
//        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH, 1000)
//        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to Euclidean2DPosition(0.0, 0.0)
//        val camerasPositions = listOf(
//            Euclidean2DPosition(0.8, 0.8) // pointing to the left
//        )
//        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
//        assertEquals(BODY_WIDTH / (2 * (BODY_LENGTH + BODY_WIDTH)), result)
//    }
//
//    @Test
//    fun `the metric for 1 perpendicular camera on long side should return the ratio between the long side and the perimeter`() {
//        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH, 1000)
//        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to Euclidean2DPosition(0.0, 0.0)
//        val camerasPositions = listOf(
//            Euclidean2DPosition(0.0, 10.0), // pointing down
//        )
//        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
//        assertEquals(BODY_LENGTH / (2 * (BODY_LENGTH + BODY_WIDTH)), result)
//    }
//
    @Test
    fun `the metric for 1 camera on a side should return a value less than half of the perimeter`() {
        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH, 8)
        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to Euclidean2DPosition(0.0, 0.0)
        val camerasPositions = listOf(
            Euclidean2DPosition(10.0, 0.0), // pointing to the bottom left
            Euclidean2DPosition(-10.0, 0.0) // pointing to the bottom left
        )
        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
        assertTrue(result < (BODY_LENGTH + BODY_WIDTH))
    }
//
//    @Test
//    fun `the metric for 4 perpendicular cameras on trans-rotated body should return 1`() {
//        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH, 4)
//        val bodyPosition = Euclidean2DPosition(1.0, 1.0) to Euclidean2DPosition(1.0, 1.0)
//        val camerasPositions = listOf(
//            Euclidean2DPosition(2.0606601717798214, 2.0606601717798214),
//            Euclidean2DPosition(-0.0606601717798214, -0.0606601717798214),
//            Euclidean2DPosition(2.0606601717798214, -0.0606601717798214),
//            Euclidean2DPosition(-0.0606601717798214, 2.0606601717798214),
//        )
//        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
//        assertEquals(1.0, result)
//    }
//
//    @Test
//    fun `the metric for cameras from the same side should return the value associated to the best view`() {
//        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH, 1000)
//        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to Euclidean2DPosition(0.0, 0.0)
//        val camerasPositions = listOf(
//            Euclidean2DPosition(10.0, 0.0), // This is the best view (perpendicular to the short side)
//            Euclidean2DPosition(7.5, 0.0),
//            Euclidean2DPosition(7.0, 2.5),
//        )
//        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
//        assertEquals(BODY_WIDTH / (2 * (BODY_WIDTH + BODY_LENGTH)), result)
//    }
}
