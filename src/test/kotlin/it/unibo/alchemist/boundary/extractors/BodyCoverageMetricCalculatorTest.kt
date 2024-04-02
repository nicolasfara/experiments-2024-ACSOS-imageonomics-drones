package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class BodyCoverageMetricCalculatorTest {
    companion object {
        private const val BODY_LENGTH = 2.0
        private const val BODY_WIDTH = 1.0
    }

    @Test
    fun `the metric for 4 perpendicular cameras should return 1`() {
        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH)
        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to 0.0
        val camerasPositions = listOf(
            Euclidean2DPosition(2.0, 0.0) to 180.0, // pointing to the left
            Euclidean2DPosition(-2.0, 0.0) to 0.0, // pointing to the right
            Euclidean2DPosition(0.0, 2.0) to 270.0, // pointing down
            Euclidean2DPosition(0.0, -2.0) to 90.0, // pointing up
        )
        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
        assertEquals(1.0, result)
    }

    @Test
    fun `the metric for 1 perpendicular camera on short side should return the ratio between the short side and the perimeter`() {
        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH)
        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to 0.0
        val camerasPositions = listOf(
            Euclidean2DPosition(2.0, 0.0) to 180.0, // pointing to the left
        )
        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
        assertEquals(BODY_WIDTH / (2 * (BODY_LENGTH + BODY_WIDTH)), result)
    }

    @Test
    fun `the metric for 1 perpendicular camera on long side should return the ratio between the long side and the perimeter`() {
        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH)
        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to 0.0
        val camerasPositions = listOf(
            Euclidean2DPosition(0.0, 2.0) to 270.0, // pointing down
        )
        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
        assertEquals(BODY_LENGTH / (2 * (BODY_LENGTH + BODY_WIDTH)), result)
    }

    @Test
    fun `the metric for 1 camera on a diagonal should return a value less than half of the perimeter`() {
        val calculator = BodyCoverageMetricCalculator(BODY_LENGTH, BODY_WIDTH)
        val bodyPosition = Euclidean2DPosition(0.0, 0.0) to 0.0
        val camerasPositions = listOf(
            Euclidean2DPosition(2.0, 2.0) to 225.0, // pointing to the bottom left
        )
        val result = calculator.computeMetricForNode(bodyPosition, camerasPositions)
        assertTrue(result < (BODY_LENGTH + BODY_WIDTH))
    }
}