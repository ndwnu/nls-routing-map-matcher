package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers;

import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkWayIdEncodedValuesFactory.ID_NAME;
import static nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneTestHelper.createIsoLabel;
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
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.Direction;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.ShortestPathTree.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.FractionAndDistance;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.assertj.core.data.Percentage;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
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
    IntEncodedValue intEncodedValue;


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
        isochroneMatchMapper = new IsochroneMatchMapper(new CrsTransformer(), encodingManager,
                fractionAndDistanceCalculator, edgeIteratorStateReverseExtractor);

    }

    @Test
    void mapToIsochroneMatch_ok_startSegment() {
        setupFixtureStartSegment(false, 0);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
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
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
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
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixtureStartSegment(false, 250.30366999283603);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
                queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(2.285606E-5);
        assertThat(result.getEndFraction()).isEqualTo(0.506377235259);
        assertThat(result.getDirection()).isEqualTo(Direction.FORWARD);
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        FractionAndDistance fractionAndDistance = fractionAndDistanceCalculator.calculateFractionAndDistance(
                result.getGeometry(),
                result.getGeometry().getStartPoint().getCoordinate());
        assertThat(fractionAndDistance.getTotalDistance()).isCloseTo(MAX_DISTANCE, Percentage
                .withPercentage(0.0001));

    }


    @Test
    void mapToIsochroneMatch_ok_cropped_geometry() {
        Geometry originalGeometry = isoLabelWayGeometry.copy();
        setupFixture(false, 250.30366999283603, 2);
        when(pointList.toLineString(INCLUDE_ELEVATION))
                .thenReturn(isoLabelWayGeometry);
        IsochroneMatch result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel, MAX_DISTANCE,
                queryGraph,
                startSegment);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.0);
        assertThat(result.getEndFraction()).isEqualTo(0.799003390195);
        assertThat(result.getDirection()).isEqualTo(Direction.FORWARD);
        assertThat(result.getGeometry().getLength()).isLessThan(originalGeometry.getLength());
        FractionAndDistance fractionAndDistance = fractionAndDistanceCalculator.calculateFractionAndDistance(
                result.getGeometry(),
                result.getGeometry().getStartPoint().getCoordinate());
        assertThat(fractionAndDistance.getTotalDistance()).isCloseTo(MAX_DISTANCE, Percentage
                .withPercentage(0.0001));

    }


    private void setupFixtureStartSegment(boolean reversed, double distance) {
        // If reversed EdgeIterator returns reversed geometry
        LineString wayGeometry = reversed ? isoLabelWayGeometry.reverse() : isoLabelWayGeometry;
        setupFixture(reversed, distance, MATCHED_LINK_ID_ONE);
        when(edgeIteratorStateStartSegment
                .fetchWayGeometry(FetchMode.ALL))
                .thenReturn(pointList);
        when(pointList.toLineString(false))
                .thenReturn(wayGeometry, startSegmentWayGeometry);
    }

    private void setupFixture(boolean reversed, double distance, int startSegmentId) {
        isoLabel = createIsoLabel(distance, 0);
        when(queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.node)).thenReturn(edgeIteratorState);
        when(edgeIteratorStateReverseExtractor
                .hasReversed(edgeIteratorState)).thenReturn(reversed, false);
        when(encodingManager.getIntEncodedValue(ID_NAME)).thenReturn(intEncodedValue);
        when(edgeIteratorState.get(intEncodedValue)).thenReturn(MATCHED_LINK_ID_ONE);
        when(startSegment.getClosestEdge()).thenReturn(edgeIteratorStateStartSegment);
        when(edgeIteratorStateStartSegment.get(intEncodedValue)).thenReturn(startSegmentId);
        when(edgeIteratorState
                .fetchWayGeometry(FetchMode.ALL))
                .thenReturn(pointList);


    }


}
