package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.routing.Path;
import com.graphhopper.util.DistanceCalcCustom;
import com.graphhopper.util.PointList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.ReliabilityCalculationType;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

@Slf4j
public class LineStringScoreUtil {

    private static final boolean REDUCE_TO_SEGMENT = true;
    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;
    private static final double DISTANCE_PENALTY_FACTOR = 1.5;
    private static final double PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR = 0.1;

    private final DistanceCalcCustom distanceCalc = new DistanceCalcCustom();
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final double absoluteRelativeWeighingFactor;


    public LineStringScoreUtil(FractionAndDistanceCalculator fractionAndDistanceCalculator, double absoluteRelativeWeighingFactor) {
        this.fractionAndDistanceCalculator = fractionAndDistanceCalculator;
        this.absoluteRelativeWeighingFactor = absoluteRelativeWeighingFactor;
        log.debug("LineStringScoreUtil created with absoluteRelativeWeighingFactor of {}", absoluteRelativeWeighingFactor);
    }

    public double calculateCandidatePathScore(Path path, LineStringLocation lineStringLocation) {
        if (ReliabilityCalculationType.POINT_OBSERVATIONS == lineStringLocation.getReliabilityCalculationType()) {
            return calculateCandidatePathScoreOnlyPoints(path.calcPoints(), path.getDistance(),
                    lineStringLocation.getGeometry(), lineStringLocation.getLengthInMeters());
        }
        return calculateCandidatePathScoreLineString(path.calcPoints(), path.getDistance(),
                lineStringLocation.getGeometry(), lineStringLocation.getLengthInMeters());
    }

    // This method is public to allow applications that implement other mapmatching algorithms than GraphHopper to
    // calculate reliability scores using the same algorithm.
    public double calculateCandidatePathScoreOnlyPoints(PointList pathPointList, double pathDistance,
            LineString originalGeometry, Double originalDistance) {
        CoordinateSequence geometryCoordinates = originalGeometry.getCoordinateSequence();
        List<Double> pointDistancesToMatch = new ArrayList<>();
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            double latitude = geometryCoordinates.getY(index);
            double longitude = geometryCoordinates.getX(index);
            pointDistancesToMatch.add(calculateSmallestDistanceToPointList(latitude, longitude, pathPointList));
        }

        double score = MAX_RELIABILITY_SCORE - Collections.min(pointDistancesToMatch)
                - Collections.max(pointDistancesToMatch);

        if (originalDistance != null) {
            double pathDistanceLengthDifferenceInMeters = Math.abs(pathDistance - originalDistance);
            double pathDistanceLengthDifferenceInPercent = pathDistanceLengthDifferenceInMeters / originalDistance;
            score -= PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * calculateWeighedScore(pathDistanceLengthDifferenceInMeters,
                    pathDistanceLengthDifferenceInPercent);
        }

        return Math.max(MIN_RELIABILITY_SCORE, score);
    }

    // This method is public to allow applications that implement other mapmatching algorithms than GraphHopper to
    // calculate reliability scores using the same algorithm.
    public double calculateCandidatePathScoreLineString(PointList pathPointList, double pathDistance,
            LineString originalGeometry, Double originalDistance) {
        double maximumDistanceInMeters = calculateMaximumDistanceInMeters(pathPointList,
                originalGeometry);

        double lengthInMeters = originalDistance != null ? originalDistance
                : fractionAndDistanceCalculator.calculateLengthInMeters(originalGeometry);
        double pathDistanceLengthDifferenceInMeters = Math.abs(pathDistance - lengthInMeters);
        double pathDistanceLengthDifferenceInPercent = pathDistanceLengthDifferenceInMeters / lengthInMeters;

        return Math.max(MIN_RELIABILITY_SCORE, MAX_RELIABILITY_SCORE
                - (DISTANCE_PENALTY_FACTOR * maximumDistanceInMeters)
                - (PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * calculateWeighedScore(pathDistanceLengthDifferenceInMeters,
                pathDistanceLengthDifferenceInPercent)));
    }

    private double calculateMaximumDistanceInMeters(PointList pathPointList, LineString geometry) {
        CoordinateSequence geometryCoordinates = geometry.getCoordinateSequence();
        double maximumDistanceInMeters = 0.0;
        for (int index = 0; index < pathPointList.size(); index++) {
            double latitude = pathPointList.getLat(index);
            double longitude = pathPointList.getLon(index);
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
                    pointList.getLat(index - 1), pointList.getLon(index - 1),
                    pointList.getLat(index), pointList.getLon(index), REDUCE_TO_SEGMENT);
            double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }

    private double calculateWeighedScore(double absoluteDifference, double relativeDifference) {
        return absoluteRelativeWeighingFactor * absoluteDifference + (1.0 - absoluteRelativeWeighingFactor)
                * relativeDifference;
    }
}
