package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.TravelDirection;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointMatchingServiceTest {

    private static final Coordinate CUTOFF_COORDINATE_1 = new Coordinate(5.4268356764862355, 52.17664885998936);
    public static final Coordinate CUTOFF_COORDINATE_2 = new Coordinate(5.426759, 52.176701);
    public static final Coordinate CUTOFF_COORDINATE_3 = new Coordinate(5.426702665042876, 52.17674144561025);
    public static final Coordinate ORIGINAL_COORDINATE_1 = new Coordinate(5.426884, 52.176616);
    public static final Coordinate ORIGINAL_COORDINATE_2 = new Coordinate(5.426759, 52.176701);
    public static final Coordinate ORIGINAL_COORDINATE_3 = new Coordinate(5.426408, 52.176953);
    public static final Coordinate INPUT_POINT_COORDINATE = new Coordinate(5.426747, 52.176663);
    public static final int ID = 1;
    public static final double SNAPPED_POINT_X = 5.426768463894968;
    public static final double SNAPPED_POINT_Y = 52.176694564551426;
    public static final double DISTANCE = 3.8067685587693947;
    public static final double FRACTION = 0.2372848571472417;
    public static final double FRACTION_REVERSED = 0.7627151428527583;
    @Mock
    private LinkFlagEncoder flagEncoder;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);

    private final GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

    @Mock
    private QueryResult queryResult;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private PointList pointList;
    @Mock
    private IntsRef intsRef;

    private Point inputPoint;


    private LineString cutoffGeometry;


    private PointMatchingService pointMatchingService;


    @BeforeEach
    void setup() {
        //input   POINT (5.426747 52.176663)
        // snapped POINT (5.426768463894968 52.176694564551426)
        // 5.42633527,52.17700763
        //getCutoffGeometry().reverse()
        // LINESTRING (5.426884 52.176616, 5.426759 52.176701, 5.426408 52.176953)
        //LINESTRING (5.426884 52.176616, 5.426759 52.176701, 5.426408 52.176953)

        /// one 5.42670562,52.17673626
        // two 5.42671260,52.17666639
        // three 5.42676011,52.17669155
        // four 5.42683114,52.17664148
        pointMatchingService = new PointMatchingService(geometryFactory, flagEncoder, geodeticCalculator);
    }


    @Test
    void calculateMatches_with_no_bearing_should_produce_one_match() {
        LineString originalGeometry = createOriginalGeometryForStraightLine();
        setupFixtureForQueryResult(originalGeometry);
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactory.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .queryResult(queryResult)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(TravelDirection.FORWARD)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getMatchedLinkId()).isEqualTo(ID);
        var match = matches.get(0);
        assertThat(match.getMatchedLinkId()).isEqualTo(ID);
        assertThat(match.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(match.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(match.isReversed()).isEqualTo(false);
        assertThat(match.getDistanceToSnappedPoint()).isEqualTo(DISTANCE);
        assertThat(match.getFractionOfSnappedPoint()).isEqualTo(FRACTION);
    }

    @Test
    void calculateMatches_with_no_bearing_and_both_directions_should_produce_two_matches() {
        LineString originalGeometry = createOriginalGeometryForStraightLine();
        setupFixtureForQueryResult(originalGeometry);
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactory.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .queryResult(queryResult)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(TravelDirection.BOTH_DIRECTIONS)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(2);
        assertThat(matches.get(0).getMatchedLinkId()).isEqualTo(ID);
        var matchOne = matches.get(0);
        assertThat(matchOne.getMatchedLinkId()).isEqualTo(ID);
        assertThat(matchOne.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(matchOne.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(matchOne.isReversed()).isEqualTo(false);
        assertThat(matchOne.getDistanceToSnappedPoint()).isEqualTo(DISTANCE);
        assertThat(matchOne.getFractionOfSnappedPoint()).isEqualTo(FRACTION);
        var matchTwo = matches.get(1);
        assertThat(matchTwo.getMatchedLinkId()).isEqualTo(ID);
        assertThat(matchTwo.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(matchTwo.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(matchTwo.isReversed()).isEqualTo(true);
        assertThat(matchTwo.getDistanceToSnappedPoint()).isEqualTo(DISTANCE);
        assertThat(matchTwo.getFractionOfSnappedPoint()).isEqualTo(FRACTION_REVERSED);
    }

    @Test
    void calculateMatches_with_matching_bearing_should_produce_one_match() {
        LineString originalGeometry = createOriginalGeometryForStraightLine();
        setupFixtureForQueryResult(originalGeometry);
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactory.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .inputMinBearing(310.0)
                .inputMaxBearing(320.0)
                .queryResult(queryResult)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(TravelDirection.BOTH_DIRECTIONS)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getMatchedLinkId()).isEqualTo(ID);
        var matchOne = matches.get(0);
        assertThat(matchOne.getMatchedLinkId()).isEqualTo(ID);
        assertThat(matchOne.getSnappedPoint().getX()).isEqualTo(SNAPPED_POINT_X);
        assertThat(matchOne.getSnappedPoint().getY()).isEqualTo(SNAPPED_POINT_Y);
        assertThat(matchOne.isReversed()).isEqualTo(false);
        assertThat(matchOne.getDistanceToSnappedPoint()).isEqualTo(DISTANCE);
        assertThat(matchOne.getFractionOfSnappedPoint()).isEqualTo(FRACTION);
    }

    @Test
    void calculateMatches_with_no_matching_bearing_should_produce_no_match() {
        createCutOffGeometryForStraightLine();
        inputPoint = geometryFactory.createPoint(INPUT_POINT_COORDINATE);
        var request = MatchedQueryResult
                .builder()
                .inputMinBearing(100.0)
                .inputMaxBearing(120.0)
                .queryResult(queryResult)
                .inputPoint(inputPoint)
                .cutoffGeometry(cutoffGeometry)
                .travelDirection(TravelDirection.BOTH_DIRECTIONS)
                .build();
        var matches = pointMatchingService.calculateMatches(request);
        assertThat(matches).hasSize(0);
    }

    private void createCutOffGeometryForStraightLine() {
        var cutoffCoordinates = new Coordinate[]{
                CUTOFF_COORDINATE_1,
                CUTOFF_COORDINATE_2,
                CUTOFF_COORDINATE_3};
        cutoffGeometry = geometryFactory.createLineString(cutoffCoordinates);
    }

    private LineString createOriginalGeometryForStraightLine() {
        var originalCoordinates = new Coordinate[]{
                ORIGINAL_COORDINATE_1,
                ORIGINAL_COORDINATE_2,
                ORIGINAL_COORDINATE_3};
        return geometryFactory.createLineString(originalCoordinates);
    }

    private void setupFixtureForQueryResult(LineString originalGeometry) {
        when(queryResult.getClosestEdge()).thenReturn(edgeIteratorState);
        when(edgeIteratorState.fetchWayGeometry(3)).thenReturn(pointList);
        when(pointList.toLineString(false)).thenReturn(originalGeometry);
        when(edgeIteratorState.getFlags()).thenReturn(intsRef);
        when(flagEncoder.getId(intsRef)).thenReturn(ID);
    }
}
