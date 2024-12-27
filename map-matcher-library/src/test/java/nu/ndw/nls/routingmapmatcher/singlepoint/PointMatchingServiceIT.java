package nu.ndw.nls.routingmapmatcher.singlepoint;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

import nu.ndw.nls.geometry.bearing.BearingCalculator;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.geometry.services.ClosestPointService;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchedPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class PointMatchingServiceIT {

    private static final int ID = 1;
    private static final double SNAPPED_POINT_X = 5.426784436725146;
    private static final double SNAPPED_POINT_Y = 52.1766837030269;
    private static final double DISTANCE = 3.437437452681715;
    private static final double FRACTION = 0.20448031137004322;
    private static final double BEARING = 317.8835356767284;
    private static final double BEARING_REVERSED = 137.88347510955532;
    private static final double SNAPPED_POINT_X_ZIG_ZAG = 5.42679104;
    private static final double SNAPPED_POINT_Y_ZIG_ZAG = 52.17667092;
    private static final double DISTANCE_ZIG_ZAG = 3.129465813922507;
    private static final double FRACTION_ZIG_ZAG = 0.29396506116769816;
    private static final double BEARING_ZIG_ZAG = 309.1094082854381;
    private static final Coordinate INPUT_POINT_COORDINATE = new Coordinate(5.426747, 52.176663);
    private static final Coordinate CUTOFF_COORDINATE_1 = new Coordinate(5.4268356764862355, 52.17664885998936);
    private static final Coordinate CUTOFF_COORDINATE_2 = new Coordinate(5.426759, 52.176701);
    private static final Coordinate CUTOFF_COORDINATE_3 = new Coordinate(5.426702665042876, 52.17674144561025);
    private static final Coordinate ORIGINAL_COORDINATE_1 = new Coordinate(5.426884, 52.176616);
    private static final Coordinate ORIGINAL_COORDINATE_2 = new Coordinate(5.426759, 52.176701);
    private static final Coordinate ORIGINAL_COORDINATE_3 = new Coordinate(5.426408, 52.176953);

    private static final Coordinate ZIG_ZAG_COORDINATE_1 = new Coordinate(5.42685002, 52.17661785);
    private static final Coordinate ZIG_ZAG_COORDINATE_2 = new Coordinate(5.42683042, 52.17662641);
    private static final Coordinate ZIG_ZAG_COORDINATE_3 = new Coordinate(5.42682052, 52.17663048);
    private static final Coordinate ZIG_ZAG_COORDINATE_4 = new Coordinate(5.42681463, 52.17665913);
    private static final Coordinate ZIG_ZAG_COORDINATE_5 = new Coordinate(5.42679104, 52.17667092);
    private static final Coordinate ZIG_ZAG_COORDINATE_6 = new Coordinate(SNAPPED_POINT_X_ZIG_ZAG,
            SNAPPED_POINT_Y_ZIG_ZAG);
    private static final Coordinate ZIG_ZAG_COORDINATE_7 = new Coordinate(5.42676155, 52.17665323);
    private static final Coordinate ZIG_ZAG_COORDINATE_8 = new Coordinate(5.42675986, 52.17669198);
    private static final Coordinate ZIG_ZAG_COORDINATE_9 = new Coordinate(5.42672195, 52.17669114);
    private static final Coordinate ZIG_ZAG_COORDINATE_10 = new Coordinate(5.42672869, 52.17670967);
    private static final Coordinate ZIG_ZAG_COORDINATE_11 = new Coordinate(5.42670342, 52.17673579);
    private static final Coordinate ZIG_ZAG_COORDINATE_12 = new Coordinate(5.42669635, 52.17673473);
    private static final Coordinate ZIG_ZAG_COORDINATE_13 = new Coordinate(5.42665413, 52.17673958);
    @Autowired
    private GeometryFactoryWgs84 geometryFactoryWgs84;
    @Autowired
    private BearingCalculator bearingCalculator;
    @Autowired
    private FractionAndDistanceCalculator fractionAndDistanceCalculator;
    @Autowired
    private ClosestPointService closestPointService;

    private Point inputPoint;

    private LineString cutoffGeometry;

    private PointMatchingService pointMatchingService;

    @BeforeEach
    void setup() {
        pointMatchingService = new PointMatchingService(geometryFactoryWgs84, bearingCalculator,
                fractionAndDistanceCalculator,closestPointService);
    }

    @Test
    void calculateMatches_with_no_bearing_should_produce_one_match() {
        LineString originalGeometry = createOriginalGeometryForStraightLine();
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactoryWgs84.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .matchedLinkId(ID)
                .originalGeometry(originalGeometry)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(EdgeIteratorTravelDirection.FORWARD)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().getMatchedLinkId()).isEqualTo(ID);
        var match = matches.getFirst();
        assertThat(match.getMatchedLinkId()).isEqualTo(ID);
        assertThat(match.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(match.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(match.isReversed()).isFalse();
        assertThat(match.getDistance()).isEqualTo(DISTANCE);
        assertThat(match.getFraction()).isEqualTo(FRACTION);
        assertThat(match.getBearing()).isEqualTo(BEARING);
    }

    @Test
    void calculateMatches_with_no_bearing_and_both_directions_should_produce_two_matches() {
        LineString originalGeometry = createOriginalGeometryForStraightLine();
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactoryWgs84.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .matchedLinkId(ID)
                .originalGeometry(originalGeometry)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(EdgeIteratorTravelDirection.BOTH_DIRECTIONS)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(2);
        assertThat(matches.getFirst().getMatchedLinkId()).isEqualTo(ID);
        var matchOne = matches.getFirst();
        assertThat(matchOne.getMatchedLinkId()).isEqualTo(ID);
        assertThat(matchOne.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(matchOne.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(matchOne.isReversed()).isFalse();
        assertThat(matchOne.getDistance()).isEqualTo(DISTANCE);
        assertThat(matchOne.getFraction()).isEqualTo(FRACTION);
        assertThat(matchOne.getBearing()).isEqualTo(BEARING);
        var matchTwo = matches.get(1);
        assertThat(matchTwo.getMatchedLinkId()).isEqualTo(ID);
        assertThat(matchTwo.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(matchTwo.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(matchTwo.isReversed()).isTrue();
        assertThat(matchTwo.getDistance()).isEqualTo(DISTANCE);
        assertThat(matchTwo.getFraction()).isEqualTo(1 - FRACTION);
        assertThat(matchTwo.getBearing()).isEqualTo(BEARING_REVERSED);
    }

    @Test
    void calculateMatches_with_matching_bearing_should_produce_one_match() {
        LineString originalGeometry = createOriginalGeometryForStraightLine();
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactoryWgs84.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .matchedLinkId(ID)
                .bearingFilter(new BearingFilter(315, 5))
                .originalGeometry(originalGeometry)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(EdgeIteratorTravelDirection.BOTH_DIRECTIONS)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().getMatchedLinkId()).isEqualTo(ID);
        var matchOne = matches.getFirst();
        assertThat(matchOne.getMatchedLinkId()).isEqualTo(ID);
        assertThat(matchOne.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(matchOne.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(matchOne.isReversed()).isFalse();
        assertThat(matchOne.getDistance()).isEqualTo(DISTANCE);
        assertThat(matchOne.getFraction()).isEqualTo(FRACTION);
        assertThat(matchOne.getBearing()).isEqualTo(BEARING);
    }

    @Test
    void calculateMatches_with_no_matching_bearing_should_produce_no_match() {
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactoryWgs84.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .matchedLinkId(ID)
                .bearingFilter(new BearingFilter(110, 10))
                .originalGeometry(cutoffGeometry)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(EdgeIteratorTravelDirection.BOTH_DIRECTIONS)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).isEmpty();
    }

    @Test
    void calculateMatches_with_zig_zag_line_should_produce_three_matches() {
        createCutOffGeometryForZigzagLine();
        LineString originalGeometry = createOriginalGeometryForZigZagLine();
        inputPoint = geometryFactoryWgs84.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .matchedLinkId(ID)
                .bearingFilter(new BearingFilter(315, 15))
                .originalGeometry(originalGeometry)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(EdgeIteratorTravelDirection.FORWARD)
                .build();
        var matches = pointMatchingService.calculateMatches(request)
                .stream()
                .sorted(comparing(MatchedPoint::getDistance))
                .toList();
        assertThat(matches).hasSize(3);
        var closestMatch = matches.getFirst();
        assertThat(closestMatch.getMatchedLinkId()).isEqualTo(ID);
        assertThat(closestMatch.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X_ZIG_ZAG);
        assertThat(closestMatch.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y_ZIG_ZAG);
        assertThat(closestMatch.isReversed()).isFalse();
        assertThat(closestMatch.getDistance()).isEqualTo(DISTANCE_ZIG_ZAG);
        assertThat(closestMatch.getFraction()).isEqualTo(FRACTION_ZIG_ZAG);
        assertThat(closestMatch.getBearing()).isEqualTo(BEARING_ZIG_ZAG);
    }

    private void createCutOffGeometryForZigzagLine() {
        var cutoffCoordinates = new Coordinate[]{
                ZIG_ZAG_COORDINATE_2,
                ZIG_ZAG_COORDINATE_3,
                ZIG_ZAG_COORDINATE_4,
                ZIG_ZAG_COORDINATE_5,
                ZIG_ZAG_COORDINATE_6,
                ZIG_ZAG_COORDINATE_7,
                ZIG_ZAG_COORDINATE_8,
                ZIG_ZAG_COORDINATE_9,
                ZIG_ZAG_COORDINATE_10,
                ZIG_ZAG_COORDINATE_11,
                ZIG_ZAG_COORDINATE_12
        };
        cutoffGeometry = geometryFactoryWgs84.createLineString(cutoffCoordinates);
    }

    private LineString createOriginalGeometryForZigZagLine() {
        var originalCoordinates = new Coordinate[]{
                ZIG_ZAG_COORDINATE_1,
                ZIG_ZAG_COORDINATE_2,
                ZIG_ZAG_COORDINATE_3,
                ZIG_ZAG_COORDINATE_4,
                ZIG_ZAG_COORDINATE_5,
                ZIG_ZAG_COORDINATE_6,
                ZIG_ZAG_COORDINATE_7,
                ZIG_ZAG_COORDINATE_8,
                ZIG_ZAG_COORDINATE_9,
                ZIG_ZAG_COORDINATE_10,
                ZIG_ZAG_COORDINATE_11,
                ZIG_ZAG_COORDINATE_13
        };
        return geometryFactoryWgs84.createLineString(originalCoordinates);
    }

    private void createCutOffGeometryForStraightLine() {
        var cutoffCoordinates = new Coordinate[]{
                CUTOFF_COORDINATE_1,
                CUTOFF_COORDINATE_2,
                CUTOFF_COORDINATE_3};
        cutoffGeometry = geometryFactoryWgs84.createLineString(cutoffCoordinates);
    }

    private LineString createOriginalGeometryForStraightLine() {
        var originalCoordinates = new Coordinate[]{
                ORIGINAL_COORDINATE_1,
                ORIGINAL_COORDINATE_2,
                ORIGINAL_COORDINATE_3};
        return geometryFactoryWgs84.createLineString(originalCoordinates);
    }
}
