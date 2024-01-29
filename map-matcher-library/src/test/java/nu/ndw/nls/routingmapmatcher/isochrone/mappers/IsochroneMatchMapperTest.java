package nu.ndw.nls.routingmapmatcher.isochrone.mappers;

import static nu.ndw.nls.routingmapmatcher.isochrone.IsochroneTestHelper.createIsoLabel;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.util.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
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

    @Mock
    private Snap startSegment;
    @Mock
    private QueryGraph queryGraph;
    @Mock
    private EncodingManager encodingManager;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private EdgeIteratorState edgeIteratorStateStartSegment;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private PointList pointList;

    @Mock
    private IntEncodedValue intEncodedValue;

    private LineString startSegmentWayGeometry;

    private LineString isoLabelWayGeometry;

    private IsochroneMatchMapper isochroneMatchMapper;

    private IsoLabel isoLabel;

    @BeforeEach
    void setup() {
        GeometryFactory geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
        startSegmentWayGeometry = geometryFactory.createLineString(new Coordinate[]{
                COORDINATE_1,
                COORDINATE_2,
                COORDINATE_3});

        isoLabelWayGeometry = geometryFactory.createLineString(new Coordinate[]{
                COORDINATE_1,
                COORDINATE_2});

        isochroneMatchMapper = new IsochroneMatchMapper(new CrsTransformer(), encodingManager,
                edgeIteratorStateReverseExtractor);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment() {
        setupFixtureStartSegment(false, 0);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0);
        assertThat(result.getEndFraction()).isEqualTo(0.633761059681);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_reversed() {
        setupFixtureStartSegment(true, 0);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.366238940319);
        assertThat(result.getEndFraction()).isEqualTo(1);
        assertThat(result.isReversed()).isTrue();
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry.reverse());
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_cropped_geometry() {
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixtureStartSegment(false, 250.30366999283603);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.506385770788);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        double lengthInMeters = FractionAndDistanceCalculator.calculateLengthInMeters(result.getGeometry());
        assertThat(lengthInMeters).isCloseTo(MAX_DISTANCE, Percentage.withPercentage(0.0001));
    }

    @Test
    void mapToIsochroneMatch_ok_cropped_geometry() {
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixture(false, 250.30366999283603, 2);
        when(pointList.toLineString(INCLUDE_ELEVATION)).thenReturn(isoLabelWayGeometry);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.79901685825);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        double lengthInMeters = FractionAndDistanceCalculator.calculateLengthInMeters(result.getGeometry());
        assertThat(lengthInMeters).isCloseTo(MAX_DISTANCE, Percentage.withPercentage(0.0001));
    }

    private void setupFixtureStartSegment(boolean reversed, double distance) {
        // If reversed EdgeIterator returns reversed geometry
        LineString wayGeometry = reversed ? isoLabelWayGeometry.reverse() : isoLabelWayGeometry;
        setupFixture(reversed, distance, MATCHED_LINK_ID_ONE);
        when(edgeIteratorStateStartSegment.fetchWayGeometry(FetchMode.ALL)).thenReturn(pointList);
        when(pointList.toLineString(false)).thenReturn(wayGeometry, startSegmentWayGeometry);
    }

    private void setupFixture(boolean reversed, double distance, int startSegmentId) {
        isoLabel = createIsoLabel(distance, 0);
        when(queryGraph.getEdgeIteratorState(isoLabel.getEdge(), isoLabel.getNode())).thenReturn(edgeIteratorState);
        when(edgeIteratorStateReverseExtractor.hasReversed(edgeIteratorState)).thenReturn(reversed, false);
        when(encodingManager.getIntEncodedValue(WAY_ID_KEY)).thenReturn(intEncodedValue);
        when(edgeIteratorState.get(intEncodedValue)).thenReturn(MATCHED_LINK_ID_ONE);
        when(startSegment.getClosestEdge()).thenReturn(edgeIteratorStateStartSegment);
        when(edgeIteratorStateStartSegment.get(intEncodedValue)).thenReturn(startSegmentId);
        when(edgeIteratorState.fetchWayGeometry(FetchMode.ALL)).thenReturn(pointList);
    }
}