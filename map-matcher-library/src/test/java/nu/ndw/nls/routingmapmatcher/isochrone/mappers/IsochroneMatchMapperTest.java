package nu.ndw.nls.routingmapmatcher.isochrone.mappers;

import static nu.ndw.nls.routingmapmatcher.isochrone.IsochroneTestHelper.createIsoLabel;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.util.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
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

    @Mock
    private QueryGraph queryGraph;
    @Mock
    private EncodingManager encodingManager;
    @Mock
    private PointListUtil pointListUtil;

    @Mock
    private EdgeIteratorState currentEdge;

    @Mock
    private EdgeIteratorState startEdge;
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

        isochroneMatchMapper = new IsochroneMatchMapper(encodingManager, edgeIteratorStateReverseExtractor,
                pointListUtil);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment() {
        setupFixtureStartSegment(false, 0);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0);
        assertThat(result.getEndFraction()).isEqualTo(0.6337610596812917);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_reversed() {
        setupFixtureStartSegment(true, 0);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.3662389403187084);
        assertThat(result.getEndFraction()).isEqualTo(1);
        assertThat(result.isReversed()).isTrue();
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry.reverse());
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_croppedGeometry() {
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixtureStartSegment(false, 250.30366999283603);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.5063949887068743);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        double lengthInMeters = FractionAndDistanceCalculator.calculateLengthInMeters(result.getGeometry());
        assertThat(lengthInMeters).isCloseTo(MAX_DISTANCE, Percentage.withPercentage(0.0001));
    }

    @Test
    void mapToIsochroneMatch_ok_croppedGeometry() {
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixture(false, 250.30366999283603, 2);
        when(pointListUtil.toLineString(pointList)).thenReturn(isoLabelWayGeometry);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.7990314030362362);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        double lengthInMeters = FractionAndDistanceCalculator.calculateLengthInMeters(result.getGeometry());
        assertThat(lengthInMeters).isCloseTo(MAX_DISTANCE, Percentage.withPercentage(0.0001));
    }

    private void setupFixtureStartSegment(boolean reversed, double distance) {
        // If reversed EdgeIterator returns reversed geometry
        LineString wayGeometry = reversed ? isoLabelWayGeometry.reverse() : isoLabelWayGeometry;
        setupFixture(reversed, distance, MATCHED_LINK_ID_ONE);
        when(startEdge.fetchWayGeometry(FetchMode.ALL)).thenReturn(pointList);
        when(pointListUtil.toLineString(pointList)).thenReturn(wayGeometry, startSegmentWayGeometry);
    }

    private void setupFixture(boolean reversed, double distance, int startSegmentId) {
        isoLabel = createIsoLabel(distance, 0);
        when(queryGraph.getEdgeIteratorState(isoLabel.getEdge(), isoLabel.getNode())).thenReturn(currentEdge);
        when(edgeIteratorStateReverseExtractor.hasReversed(currentEdge)).thenReturn(reversed, false);
        when(encodingManager.getIntEncodedValue(WAY_ID_KEY)).thenReturn(intEncodedValue);
        when(currentEdge.get(intEncodedValue)).thenReturn(MATCHED_LINK_ID_ONE);
        when(startEdge.get(intEncodedValue)).thenReturn(startSegmentId);
        when(currentEdge.fetchWayGeometry(FetchMode.ALL)).thenReturn(pointList);
    }
}
