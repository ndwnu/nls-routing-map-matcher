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

    public double calculateCandidatePathScore(Path path, LineStringLocation lineStringLocation) {
        if (ReliabilityCalculationType.POINT_OBSERVATIONS == lineStringLocation.getReliabilityCalculationType()) {
            return calculateCandidatePathScoreOnlyPoints(path, lineStringLocation);
        }
        return calculateCandidatePathScoreLineString(path, lineStringLocation);
    }

    private double calculateCandidatePathScoreOnlyPoints(Path path, LineStringLocation lineStringLocation) {
        PointList pathPointList = path.calcPoints();
        CoordinateSequence geometryCoordinates = lineStringLocation.getGeometry().getCoordinateSequence();
        List<Double> pointDistancesToMatch = new ArrayList<>();
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            double latitude = geometryCoordinates.getY(index);
            double longitude = geometryCoordinates.getX(index);
            pointDistancesToMatch.add(calculateSmallestDistanceToPointList(latitude, longitude, pathPointList));
        }

        double score = MAX_RELIABILITY_SCORE - Collections.min(pointDistancesToMatch)
                - Collections.max(pointDistancesToMatch);

        Double lengthInMeters = lineStringLocation.getLengthInMeters();
        if (lengthInMeters != null) {
            double pathDistanceLengthDifferenceInMeters = Math.abs(path.getDistance() - lengthInMeters);
            score -= PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * pathDistanceLengthDifferenceInMeters;
        }

        return Math.max(MIN_RELIABILITY_SCORE, score);
    }

    private double calculateCandidatePathScoreLineString(Path path, LineStringLocation lineStringLocation) {
        double maximumDistanceInMeters = calculateMaximumDistanceInMeters(path, lineStringLocation.getGeometry());

        double lengthInMeters = lineStringLocation.getLengthInMeters();
        double pathDistanceLengthDifferenceInMeters = Math.abs(path.getDistance() - lengthInMeters);

        return Math.max(MIN_RELIABILITY_SCORE, MAX_RELIABILITY_SCORE
                - (DISTANCE_PENALTY_FACTOR * maximumDistanceInMeters)
                - (PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * pathDistanceLengthDifferenceInMeters));
    }

    private double calculateMaximumDistanceInMeters(Path path, LineString geometry) {
        PointList pathPointList = path.calcPoints();
        CoordinateSequence geometryCoordinates = geometry.getCoordinateSequence();
        double maximumDistanceInMeters = 0.0;
        for (int index = 0; index < pathPointList.size(); index++) {
            double latitude = pathPointList.getLatitude(index);
            double longitude = pathPointList.getLongitude(index);
            double smallestDistanceToLtcLink = calculateSmallestDistanceToCoordinateSequence(latitude, longitude,
                    geometryCoordinates);
            maximumDistanceInMeters = Math.max(maximumDistanceInMeters, smallestDistanceToLtcLink);
        }
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            double latitude = geometryCoordinates.getY(index);
            double longitude = geometryCoordinates.getX(index);
            double smallestDistanceToLtcLink = calculateSmallestDistanceToPointList(latitude, longitude, pathPointList);
            maximumDistanceInMeters = Math.max(maximumDistanceInMeters, smallestDistanceToLtcLink);
        }
        return maximumDistanceInMeters;
    }

    private double calculateSmallestDistanceToCoordinateSequence(double latitude, double longitude,
            CoordinateSequence coordinateSequence) {
        double smallestDistanceToLtcLink = Double.MAX_VALUE;
        for (int index = 1; index < coordinateSequence.size(); index++) {
            double normalizedDistance = distanceCalc.calcNormalizedEdgeDistanceNew(latitude, longitude,
                    coordinateSequence.getY(index - 1), coordinateSequence.getX(index - 1),
                    coordinateSequence.getY(index), coordinateSequence.getX(index), REDUCE_TO_SEGMENT);
            double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }

    private double calculateSmallestDistanceToPointList(double latitude, double longitude, PointList pointList) {
        double smallestDistanceToLtcLink = Double.MAX_VALUE;
        for (int index = 1; index < pointList.size(); index++) {
            double normalizedDistance = distanceCalc.calcNormalizedEdgeDistanceNew(latitude, longitude,
                    pointList.getLatitude(index - 1), pointList.getLongitude(index - 1),
                    pointList.getLatitude(index), pointList.getLongitude(index), REDUCE_TO_SEGMENT);
            double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }
}
