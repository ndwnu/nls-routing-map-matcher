package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import java.lang.reflect.Constructor;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch.Direction;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.assertj.core.data.Percentage;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsochroneMatchMapperTest {

    private static final int MAX_DISTANCE = 200;
    private static final Coordinate COORDINATE_1 = new Coordinate(5.426794, 52.176669);
    private static final Coordinate COORDINATE_2 = new Coordinate(5.4293175, 52.1750401);
    private static final Coordinate COORDINATE_3 = new Coordinate(5.430860, 52.174151);
    private static final int MATCHED_LINK_ID_ONE = 1;
    private static final boolean INCLUDE_ELEVATION = false;
    private static final int ALL_NODES = 3;

    @Mock
    private QueryResult startSegment;
    @Mock
    private QueryGraph queryGraph;
    @Mock
    private LinkFlagEncoder flagEncoder;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private EdgeIteratorState edgeIteratorStateStartSegment;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private PointList pointList;
    @Mock
    private IntsRef intsRef;

    @Mock
    private IntsRef intsRefStartSegment;

    private LineString startSegmentWayGeometry;

    private LineString isoLabelWayGeometry;

    private IsochroneMatchMapper isochroneMatchMapper;

    private IsoLabel isoLabel;
    private FractionAndDistanceCalculator fractionAndDistanceCalculator;

    @BeforeEach
    void setup() {
        GeometryFactory geometryFactory =
                new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        startSegmentWayGeometry = geometryFactory.createLineString(new Coordinate[]{
                COORDINATE_1,
                COORDINATE_2,
                COORDINATE_3});

        isoLabelWayGeometry = geometryFactory.createLineString(new Coordinate[]{
                COORDINATE_1,
                COORDINATE_2});

        fractionAndDistanceCalculator = new FractionAndDistanceCalculator(new GeodeticCalculator());
        isochroneMatchMapper = IsochroneMatchMapper
                .builder()
                .crsTransformer(new CrsTransformer())
                .flagEncoder(flagEncoder)
                .fractionAndDistanceCalculator(fractionAndDistanceCalculator)
                .edgeIteratorStateReverseExtractor(edgeIteratorStateReverseExtractor)
                .build();
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment() {
        setupFixtureStartSegment(false, 0);
        var result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
                queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0);
        assertThat(result.getEndFraction()).isEqualTo(0.633761059681);
        assertThat(result.getDirection()).isEqualTo(Direction.FORWARD);
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_reversed() {
        setupFixtureStartSegment(true, 0);
        var result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
                queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.366238940319);
        assertThat(result.getEndFraction()).isEqualTo(1);
        assertThat(result.getDirection()).isEqualTo(Direction.BACKWARD);
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry.reverse());
    }


    @Test
    void mapToIsochroneMatch_ok_startSegment_cropped_geometry() {
        var originalGeometry = isoLabelWayGeometry.copy();
        setupFixtureStartSegment(false, 250.30366999283603);
        var result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
                queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(2.285606E-5);
        assertThat(result.getEndFraction()).isEqualTo(0.506377235259);
        assertThat(result.getDirection()).isEqualTo(Direction.FORWARD);
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        var fractionAndDistance = fractionAndDistanceCalculator.calculateFractionAndDistance(
                result.getGeometry(),
                result.getGeometry().getStartPoint().getCoordinate());
        assertThat(fractionAndDistance.getTotalDistance()).isCloseTo(MAX_DISTANCE, Percentage
                .withPercentage(0.0001));

    }


    @Test
    void mapToIsochroneMatch_ok_cropped_geometry() {
        var originalGeometry = isoLabelWayGeometry.copy();
        setupFixture(false, 250.30366999283603,2);
        when(pointList.toLineString(INCLUDE_ELEVATION))
                .thenReturn(isoLabelWayGeometry);
        var result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
                queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.799003390195);
        assertThat(result.getDirection()).isEqualTo(Direction.FORWARD);
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        var fractionAndDistance = fractionAndDistanceCalculator.calculateFractionAndDistance(
                result.getGeometry(),
                result.getGeometry().getStartPoint().getCoordinate());
        assertThat(fractionAndDistance.getTotalDistance()).isCloseTo(MAX_DISTANCE, Percentage
                .withPercentage(0.0001));

    }


    private void setupFixtureStartSegment(boolean reversed, double distance) {
        // If reversed EdgeIterator returns reversed geometry
        var wayGeometry = reversed ? isoLabelWayGeometry.reverse() : isoLabelWayGeometry;
        setupFixture(reversed, distance, MATCHED_LINK_ID_ONE);
        when(edgeIteratorStateStartSegment
                .fetchWayGeometry(ALL_NODES))
                .thenReturn(pointList);
        when(pointList.toLineString(false))
                .thenReturn(wayGeometry, startSegmentWayGeometry);
    }

    private void setupFixture(boolean reversed, double distance, int startSegmentId) {
        isoLabel = createIsoLabel(distance);
        when(queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode)).thenReturn(edgeIteratorState);
        when(edgeIteratorStateReverseExtractor
                .hasReversed(edgeIteratorState)).thenReturn(reversed, false);
        when(edgeIteratorState.getFlags()).thenReturn(intsRef);
        when(flagEncoder.getId(intsRef)).thenReturn(MATCHED_LINK_ID_ONE);
        when(startSegment.getClosestEdge()).thenReturn(edgeIteratorStateStartSegment);
        when(edgeIteratorStateStartSegment.getFlags()).thenReturn(intsRefStartSegment);
        when(flagEncoder.getId(intsRefStartSegment)).thenReturn(startSegmentId);
        when(edgeIteratorState
                .fetchWayGeometry(ALL_NODES))
                .thenReturn(pointList);


    }

    @SneakyThrows
    private static IsoLabel createIsoLabel(double distance) {
        int edgeId = 1;
        int adjNode = 2;
        double weight = 0;
        long time = 0;
        Constructor<IsoLabel> constructor = IsoLabel.class.getDeclaredConstructor(
                int.class,
                int.class,
                double.class,
                long.class,
                double.class);
        constructor.setAccessible(true);
        return constructor.newInstance(edgeId, adjNode, weight, time, distance);
    }
}
