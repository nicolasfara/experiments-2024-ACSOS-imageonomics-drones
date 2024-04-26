package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.locationtech.jts.algorithm.Centroid
import org.locationtech.jts.awt.ShapeReader
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory

class FovDistance<T>(
    private val environment: Physics2DEnvironment<T>,
    override val node: Node<T>,
    visionMoleculeName: String,
    targetMoleculeName: String,
): NodeProperty<T> {
    private val visionMolecule by lazy { SimpleMolecule(visionMoleculeName) }
    private val targetMolecule by lazy { SimpleMolecule(targetMoleculeName) }
    private val geometry by lazy { GeometryFactory() }
    private val metricCalculator = CentroidQualityMetricCalculator()

    companion object {
        val fovDistanceMolecule = SimpleMolecule("FovDistance")
        val fovDistanceOnlyCovered = SimpleMolecule("FovDistanceOnlyCovered")
        private val flatness = 1.0
    }

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = FovDistance(environment, node, visionMolecule.name, targetMolecule.name)

    fun computeFoVDistance(whenNotCovered: Double): Double {
        val nodes = environment.nodes
        val visibleCameras = node.getVisibleCameras(nodes, visionMolecule)
        return metricCalculator.computeQualityMetric(
            environment.getPosition(node).asCoordinate(),
            visibleCameras.map { it.properties.filterIsInstance<CameraWithBlindSpot<Any>>().firstOrNull()
                ?.asCameraQualityInformation()
                ?: error("Property ${CameraWithBlindSpot::class} not found.") },
            whenNotCovered
        )
    }

    private fun Euclidean2DPosition.asCoordinate(): Coordinate = Coordinate(x, y)
    private fun CameraWithBlindSpot<*>.geometryRepresentation(): Geometry =
        ShapeReader.read(this.transformShapeToEnvironmentPosition(), flatness, geometry)

    private fun CameraWithBlindSpot<*>.centroid(): Coordinate =
        Centroid.getCentroid(geometryRepresentation())
    private fun CameraWithBlindSpot<*>.worstCaseCoordinateVector(): Coordinate {
        val cameraShape = this.geometryRepresentation()
        val centroid = this.centroid()
        return cameraShape.coordinates.map { it to it.distance(centroid) }
            .maxByOrNull { it.second }
            ?.let { (coordinate, _) -> coordinate }
            ?: error("It should have at least one coordinate")
    }
    private fun CameraWithBlindSpot<Any>.asCameraQualityInformation(): CameraQualityInformation = this.centroid() to this.worstCaseCoordinateVector()
}