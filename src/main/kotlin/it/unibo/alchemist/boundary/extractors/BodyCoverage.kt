package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.experiment.toBoolean

class BodyCoverage(
    private val visionMolecule: Molecule,
    private val targetMolecule: Molecule,
    private val bodyLength: Double,
    private val bodyWidth: Double,
) : AbstractDoubleExporter() {

    private val bodyCoverageColumnName = "BodyCoverage"
    override val columnNames: List<String> = listOf(bodyCoverageColumnName)

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        require(environment is Physics2DEnvironment) {
            "Expected a Physics2DEnvironment but got ${environment::class}"
        }
        val nodes = environment.nodes
        val cameraNodes: List<Node<T>> = nodes.filter { it.isCamera() }
        val targetNodes = nodes.filter { it.isTarget() }
        val visibleTargets = cameraNodes.flatMap { it.getVisibleTargets() }.toSet()
        val sum = visibleTargets.sumOf { it.computeMetricForNode(environment, cameraNodes, bodyLength, bodyWidth) }
        return mapOf(bodyCoverageColumnName to sum / targetNodes.size)
    }

    private fun <T> VisibleNode<T, *>.computeMetricForNode(
        environment: Environment<T, *>,
        cameraNodes: List<Node<T>>,
        bodyLength: Double,
        bodyWidth: Double,
    ): Double {
        require(environment is Physics2DEnvironment) {
            "Expected a Physics2DEnvironment but got ${environment::class}"
        }
        return camerasReachingTheBody(cameraNodes).sumOf { camera ->
            val bodyRotoTranslated = getRotoTranslationToOrigin(environment)
            val (dx, dy, angle) = camera.rotoTranslateCamera(environment, bodyRotoTranslated)
            val rotoTranslatedCameraPosition = environment.makePosition(dx, dy)
            when (angle) {
                0.0, 180.0 -> bodyWidth
                90.0, 270.0 -> bodyLength
                else -> getVisiblePerimeter(environment, rotoTranslatedCameraPosition, bodyLength, bodyWidth)
            }
        }
    }

    private fun <T> getVisiblePerimeter(
        env: Physics2DEnvironment<T>,
        cameraPosition: Euclidean2DPosition,
        bodyLength: Double,
        bodyWidth: Double,
    ): Double {
        val topRightCoordinate = env.makePosition(bodyLength / 2, bodyWidth / 2)
        val bottomRightCoordinate = env.makePosition(bodyLength / 2, -bodyWidth / 2)
        val bottomLeftCoordinate = env.makePosition(-bodyLength / 2, -bodyWidth / 2)
        val topLeftCoordinate = env.makePosition(-bodyLength / 2, bodyWidth / 2)
        val segments = listOf(
            topRightCoordinate to bottomRightCoordinate,
            bottomRightCoordinate to bottomLeftCoordinate,
            bottomLeftCoordinate to topLeftCoordinate,
            topLeftCoordinate to topRightCoordinate,
        )
        return segments.asSequence()
            // Determine the vertex distance to the camera
            .map { it to it.first.distanceTo(cameraPosition) + it.second.distanceTo(cameraPosition) }
            // Sort by distance (closest first)
            .sortedBy { it.second }
            // Take the first two segments (only two are visible when the camera is not perpendicular to the body)
            .take(2)
            // Compute the visible perimeter
            .map { (points, _) -> points to points.first.distanceTo(points.second) }
            // Idea: weight the perimeter by the angle of the camera and distance.
            //       The more the camera is perpendicular to the body, the more the quality of the coverage is high.
            //       The more the camera is far from the body, the less the quality of the coverage is high.
            .map { (points, perimeter) ->
                val angle = when (points.isVertical()) {
                    // Rotate by 90 degrees the camera
                    true -> (cameraPosition.minus(points.midPoint()).asAngle + 90.0) % 90.0
                    false -> cameraPosition.minus(points.midPoint()).asAngle % 90.0
                }
                perimeter * (angle / 90.0)
                // TODO: add a weight based on the distance
            }
            .sum() / perimeter()
    }

    private fun Pair<Euclidean2DPosition, Euclidean2DPosition>.isVertical(): Boolean {
        val (p1, p2) = this
        return p1.coordinates[0] == p2.coordinates[0]
    }

    private fun midPoint(p1: Euclidean2DPosition, p2: Euclidean2DPosition): Euclidean2DPosition {
        return Euclidean2DPosition((p1.coordinates[0] + p2.coordinates[0]) / 2, (p1.coordinates[1] + p2.coordinates[1]) / 2)
    }
    private fun Pair<Euclidean2DPosition, Euclidean2DPosition>.midPoint(): Euclidean2DPosition {
        return midPoint(first, second)
    }

    /**
     * Returns the list of cameras that can see the body.
     * TODO: verify if the body must be fully covered or partially covered.
     */
    private fun <T> VisibleNode<T, *>.camerasReachingTheBody(cameraNodes: List<Node<T>>): List<Node<T>> {
        return cameraNodes.filter { it.getVisibleTargets().contains(this) }
    }

    private data class RotoTranslation(val x: Double, val y: Double, val angle: Double)

    private fun <T> VisibleNode<T, *>.getRotoTranslationToOrigin(environment: Environment<T, *>): RotoTranslation {
        require(environment is Physics2DEnvironment<T>) {
            "Expected a Physics2DEnvironment but got ${environment::class}"
        }
        val bodyPosition = environment.getPosition(node).coordinates
        val bodyAngle = environment.getHeading(node).asAngle
        return RotoTranslation(bodyPosition[0], bodyPosition[1], bodyAngle)
    }

    private fun <T> Node<T>.rotoTranslateCamera(env: Environment<T, *>, bodyRotoTranslation: RotoTranslation): RotoTranslation {
        require(env is Physics2DEnvironment<T>) {
            "Expected a Physics2DEnvironment but got ${env::class}"
        }
        val (dx, dy, angle) = bodyRotoTranslation
        val cameraPosition = env.getPosition(this)
        val cameraAngle = env.getHeading(this).asAngle
        val normalizedPosition = cameraPosition.minus(dx to dy)
        val normalizedAngle = cameraAngle - angle
        return RotoTranslation(normalizedPosition[0], normalizedPosition[1], normalizedAngle)
    }

    /**
     * Bounding box perimeter of the body.
     */
    private fun perimeter(): Double = 2 * (bodyLength + bodyWidth)

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

    private fun Node<*>.isCamera() = contains(visionMolecule)

    private fun <T> Node<T>.getVisibleTargets() =
        with(getConcentration(visionMolecule)) {
            require(this is List<*>) { "Expected a List but got $this" }
            if (isNotEmpty()) {
                get(0)?.also {
                    require(it is VisibleNode<*, *>) {
                        "Expected a List<VisibleNode> but got List<${it::class}> = $this"
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            (this as Iterable<VisibleNode<T, *>>).filter { it.node.isTarget() }
        }
}