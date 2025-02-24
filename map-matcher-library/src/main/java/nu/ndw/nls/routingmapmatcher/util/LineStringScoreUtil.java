package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.routing.Path;
import com.graphhopper.util.DistanceCalcCustom;
import com.graphhopper.util.PointList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.confidence.LineStringReliabilityCalculator;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.ReliabilityCalculationType;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

@Slf4j
@RequiredArgsConstructor
public class LineStringScoreUtil {

    private static final boolean REDUCE_TO_SEGMENT = true;
    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;

    private final DistanceCalcCustom distanceCalc = new DistanceCalcCustom();
    private final PointListUtil pointListUtil;
    private final LineStringReliabilityCalculator lineStringReliabilityCalculator;

    public double calculateCandidatePathScore(Path path, LineStringLocation lineStringLocation) {
        if (ReliabilityCalculationType.POINT_OBSERVATIONS == lineStringLocation.getReliabilityCalculationType()) {
            return calculateCandidatePathScoreOnlyPoints(path.calcPoints(), lineStringLocation.getGeometry());
        }
        return calculateCandidatePathScoreLineString(path.calcPoints(), lineStringLocation.getGeometry());
    }

    private double calculateCandidatePathScoreOnlyPoints(PointList pathPointList, LineString originalGeometry) {
        CoordinateSequence geometryCoordinates = originalGeometry.getCoordinateSequence();
        List<Double> pointDistancesToMatch = new ArrayList<>();
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            double latitude = geometryCoordinates.getY(index);
            double longitude = geometryCoordinates.getX(index);
            pointDistancesToMatch.add(calculateSmallestDistanceToPointList(latitude, longitude, pathPointList));
        }

        double score = MAX_RELIABILITY_SCORE - Collections.min(pointDistancesToMatch) - Collections.max(pointDistancesToMatch);
        return Math.max(MIN_RELIABILITY_SCORE, score);
    }

    private double calculateCandidatePathScoreLineString(PointList pathPointList, LineString originalGeometry) {
        LineString pathLineString = pointListUtil.toLineString(pathPointList);
        return lineStringReliabilityCalculator.calculateLineStringReliability(originalGeometry, pathLineString);
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
}
