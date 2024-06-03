package nu.ndw.nls.routingmapmatcher.isochrone.mappers;

import static nu.ndw.nls.routingmapmatcher.isochrone.IsochroneTestHelper.createIsoLabel;
import static nu.ndw.nls.routingmapmatcher.isochrone.IsochroneTestHelper.createIsoLabelWithNonRootParent;
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
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(classes = {TestConfig.class})
class IsochroneMatchMapperTest {

    private static final int MAX_DISTANCE = 200;
    private static final Coordinate COORDINATE_1 = new Coordinate(5.426794, 52.176669);
    private static final Coordinate COORDINATE_2 = new Coordinate(5.4293175, 52.1750401);
    private static final Coordinate COORDINATE_3 = new Coordinate(5.430860, 52.174151);
    private static final int MATCHED_LINK_ID_ONE = 1;
    private static final int PARENT_LINK_ID = 2;

    @Mock
    private QueryGraph queryGraph;
    @Mock
    private EncodingManager encodingManager;
    @Mock
    private PointListUtil pointListUtil;

    @Mock
    private EdgeIteratorState currentEdge;

    @Mock
    private EdgeIteratorState parentEdge;

    @Mock
    private EdgeIteratorState startEdge;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private PointList pointList;

    @Mock
    private IntEncodedValue intEncodedValue;
    @Autowired
    private FractionAndDistanceCalculator fractionAndDistanceCalculator;

    private LineString startSegmentWayGeometry;

    private LineString isoLabelWayGeometry;

    private IsochroneMatchMapper isochroneMatchMapper;


    @BeforeEach
    void setup() {
        GeometryFactory geometryFactory = new GeometryFactoryWgs84();
        startSegmentWayGeometry = geometryFactory.createLineString(new Coordinate[]{
                COORDINATE_1,
                COORDINATE_2,
                COORDINATE_3});

        isoLabelWayGeometry = geometryFactory.createLineString(new Coordinate[]{
                COORDINATE_1,
                COORDINATE_2});

        isochroneMatchMapper = new IsochroneMatchMapper(encodingManager, edgeIteratorStateReverseExtractor,
                pointListUtil, fractionAndDistanceCalculator);
    }

    @Test
    void mapToIsochroneMatch_ok_parent() {
        IsoLabel isoLabel = createIsoLabelWithNonRootParent(0, 0);
        setupFixture(false, 0, isoLabel);
        when(pointListUtil.toLineString(pointList)).thenReturn(isoLabelWayGeometry);
        when(queryGraph.getEdgeIteratorState(isoLabel.getParent().getEdge(), isoLabel.getParent().getNode()))
                .thenReturn(parentEdge);
        when(parentEdge.get(intEncodedValue)).thenReturn(PARENT_LINK_ID);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0);
        assertThat(result.getEndFraction()).isEqualTo(1.0);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry);
        assertThat(result.getParentLinkId()).isEqualTo(PARENT_LINK_ID);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment() {
        IsoLabel isoLabel = createIsoLabel(0, 0);
        setupFixtureStartSegment(false, isoLabel);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0);
        assertThat(result.getEndFraction()).isEqualTo(0.6337610596812917);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry);
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_reversed() {
        IsoLabel isoLabel = createIsoLabel(0, 0);
        setupFixtureStartSegment(true, isoLabel);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        //assertThat(result.getStartFraction()).isEqualTo(0.3662389403187084);
       // assertThat(result.getEndFraction()).isEqualTo(1);
        //assertThat(result.isReversed()).isTrue();
       // assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry.reverse());
    }

    @Test
    void mapToIsochroneMatch_ok_startSegment_croppedGeometry() {
        IsoLabel isoLabel = createIsoLabel(250.30366999283603, 0);
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixtureStartSegment(false, isoLabel);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.5063949887068743);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        double lengthInMeters = fractionAndDistanceCalculator.calculateLengthInMeters(result.getGeometry());
        assertThat(lengthInMeters).isCloseTo(MAX_DISTANCE, Percentage.withPercentage(0.0001));
    }

    @Test
    void mapToIsochroneMatch_ok_croppedGeometry() {
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        IsoLabel isoLabel = createIsoLabel(250.30366999283603, 0);
        setupFixture(false, 2, isoLabel);
        when(pointListUtil.toLineString(pointList)).thenReturn(isoLabelWayGeometry);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE, queryGraph, startEdge);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.7990314030362362);
        assertThat(result.isReversed()).isFalse();
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        double lengthInMeters = fractionAndDistanceCalculator.calculateLengthInMeters(result.getGeometry());
        assertThat(lengthInMeters).isCloseTo(MAX_DISTANCE, Percentage.withPercentage(0.0001));
    }

    private void setupFixtureStartSegment(boolean reversed, IsoLabel isoLabel) {
        // If reversed EdgeIterator returns reversed geometry
        LineString wayGeometry = reversed ? isoLabelWayGeometry.reverse() : isoLabelWayGeometry;
        setupFixture(reversed, MATCHED_LINK_ID_ONE, isoLabel);
        when(startEdge.fetchWayGeometry(FetchMode.ALL)).thenReturn(pointList);
        when(pointListUtil.toLineString(pointList)).thenReturn(wayGeometry, startSegmentWayGeometry);
    }

    private void setupFixture(boolean reversed, int startSegmentId, IsoLabel isoLabel) {
        when(queryGraph.getEdgeIteratorState(isoLabel.getEdge(), isoLabel.getNode())).thenReturn(currentEdge);
        //when(edgeIteratorStateReverseExtractor.hasReversed(currentEdge)).thenReturn(reversed, false);
        when(encodingManager.getIntEncodedValue(WAY_ID_KEY)).thenReturn(intEncodedValue);
        when(currentEdge.get(intEncodedValue)).thenReturn(MATCHED_LINK_ID_ONE);
        when(startEdge.get(intEncodedValue)).thenReturn(startSegmentId);
        when(currentEdge.fetchWayGeometry(FetchMode.ALL)).thenReturn(pointList);
    }
}
