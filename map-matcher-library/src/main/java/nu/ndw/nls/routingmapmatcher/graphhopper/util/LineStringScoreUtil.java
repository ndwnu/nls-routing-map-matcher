package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import com.graphhopper.routing.Path;
import com.graphhopper.util.DistancePlaneProjection;
import com.graphhopper.util.PointList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.ReliabilityCalculationType;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

public class LineStringScoreUtil {

    private static final boolean REDUCE_TO_SEGMENT = true;
    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;
    private static final double DISTANCE_PENALTY_FACTOR = 1.5;
    private static final double PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR = 0.1;

    private final DistancePlaneProjection distanceCalc = new DistancePlaneProjection();

    public double calculateCandidatePathScore(final Path path, final LineStringLocation lineStringLocation) {
        if (ReliabilityCalculationType.POINT_OBSERVATIONS == lineStringLocation.getReliabilityCalculationType()) {
            return calculateCandidatePathScoreOnlyPoints(path, lineStringLocation);
        }
        return calculateCandidatePathScoreLineString(path, lineStringLocation);
    }

    private double calculateCandidatePathScoreOnlyPoints(final Path path, final LineStringLocation lineStringLocation) {
        final PointList pathPointList = path.calcPoints();
        final CoordinateSequence geometryCoordinates = lineStringLocation.getGeometry().getCoordinateSequence();
        final List<Double> pointDistancesToMatch = new ArrayList<>();
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            final double latitude = geometryCoordinates.getY(index);
            final double longitude = geometryCoordinates.getX(index);
            pointDistancesToMatch.add(calculateSmallestDistanceToPointList(latitude, longitude,
                    pathPointList));
        }

        double score = MAX_RELIABILITY_SCORE - Collections.min(pointDistancesToMatch)
                - Collections.max(pointDistancesToMatch);

        final Double lengthInMeters = lineStringLocation.getLengthInMeters();
        if (lengthInMeters != null) {
            final double pathDistanceLengthDifferenceInMeters = Math.abs(path.getDistance() - lengthInMeters);
            score -= PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * pathDistanceLengthDifferenceInMeters;
        }

        return Math.max(MIN_RELIABILITY_SCORE, score);
    }

    private double calculateCandidatePathScoreLineString(final Path path, final LineStringLocation lineStringLocation) {
        final double maximumDistanceInMeters = calculateMaximumDistanceInMeters(path, lineStringLocation.getGeometry());

        final double lengthInMeters = lineStringLocation.getLengthInMeters();
        final double pathDistanceLengthDifferenceInMeters = Math.abs(path.getDistance() - lengthInMeters);

        return Math.max(MIN_RELIABILITY_SCORE, MAX_RELIABILITY_SCORE
                - (DISTANCE_PENALTY_FACTOR * maximumDistanceInMeters)
                - (PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * pathDistanceLengthDifferenceInMeters));
    }

    private double calculateMaximumDistanceInMeters(final Path path, final LineString geometry) {
        final PointList pathPointList = path.calcPoints();
        final CoordinateSequence geometryCoordinates = geometry.getCoordinateSequence();
        double maximumDistanceInMeters = 0.0;
        for (int index = 0; index < pathPointList.size(); index++) {
            final double latitude = pathPointList.getLatitude(index);
            final double longitude = pathPointList.getLongitude(index);
            final double smallestDistanceToLtcLink = calculateSmallestDistanceToCoordinateSequence(latitude, longitude,
                    geometryCoordinates);
            maximumDistanceInMeters = Math.max(maximumDistanceInMeters, smallestDistanceToLtcLink);
        }
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            final double latitude = geometryCoordinates.getY(index);
            final double longitude = geometryCoordinates.getX(index);
            final double smallestDistanceToLtcLink = calculateSmallestDistanceToPointList(latitude, longitude,
                    pathPointList);
            maximumDistanceInMeters = Math.max(maximumDistanceInMeters, smallestDistanceToLtcLink);
        }
        return maximumDistanceInMeters;
    }

    private double calculateSmallestDistanceToCoordinateSequence(final double latitude, final double longitude,
            final CoordinateSequence coordinateSequence) {
        double smallestDistanceToLtcLink = Double.MAX_VALUE;
        for (int index = 1; index < coordinateSequence.size(); index++) {
            final double normalizedDistance = distanceCalc.calcNormalizedEdgeDistanceNew(latitude, longitude,
                    coordinateSequence.getY(index - 1), coordinateSequence.getX(index - 1),
                    coordinateSequence.getY(index), coordinateSequence.getX(index), REDUCE_TO_SEGMENT);
            final double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }

    private double calculateSmallestDistanceToPointList(final double latitude, final double longitude,
            final PointList pointList) {
        double smallestDistanceToLtcLink = Double.MAX_VALUE;
        for (int index = 1; index < pointList.size(); index++) {
            final double normalizedDistance = distanceCalc.calcNormalizedEdgeDistanceNew(latitude, longitude,
                    pointList.getLatitude(index - 1), pointList.getLongitude(index - 1),
                    pointList.getLatitude(index), pointList.getLongitude(index), REDUCE_TO_SEGMENT);
            final double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }
}
