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

    @Mock
    private QueryResult startSegment;
    @Mock
    private QueryGraph queryGraph;
    @Mock
    private LinkFlagEncoder flagEncoder;

    @Mock
    private EdgeIteratorState edgeIteratorState;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private PointList pointList;
    @Mock
    private IntsRef intsRef;

    private LineString startSegmentWayGeometry;

    private LineString isoLabelWayGeometry;

    private IsochroneMatchMapper isochroneMatchMapper;

    private IsoLabel isoLabel;

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

        isochroneMatchMapper = IsochroneMatchMapper
                .builder()
                .crsTransformer(new CrsTransformer())
                .flagEncoder(flagEncoder)
                .maxDistance(MAX_DISTANCE)
                .fractionAndDistanceCalculator(new FractionAndDistanceCalculator(new GeodeticCalculator()))
                .queryGraph(queryGraph)
                .startSegment(startSegment)
                .edgeIteratorStateReverseExtractor(edgeIteratorStateReverseExtractor)
                .build();
    }

    @Test
    void when_isolabel_is_start_segment_mapToIsochroneMatch_should_return_original_geometry() {
        setupFixtureForEgdeSegmentIsStartSegmentWithTravelledDistance(false, 0);
        var result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0);
        assertThat(result.getEndFraction()).isEqualTo(0.633761059681);
        assertThat(result.getDirection()).isEqualTo(Direction.FORWARD);
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry);
    }

    @Test
    void when_isolabel_is_start_segment_and_reversed_mapToIsochroneMatch_should_return_reversed_geometry() {
        setupFixtureForEgdeSegmentIsStartSegmentWithTravelledDistance(true, 0);
        var result = isochroneMatchMapper.mapToIsochroneMatch(isoLabel);
        assertThat(result.getMatchedLinkId()).isEqualTo(MATCHED_LINK_ID_ONE);
        assertThat(result.getStartFraction()).isEqualTo(0.366238940319);
        assertThat(result.getEndFraction()).isEqualTo(1);
        assertThat(result.getDirection()).isEqualTo(Direction.BACKWARD);
        assertThat(result.getGeometry()).isEqualTo(isoLabelWayGeometry.reverse());
    }

    private void setupFixtureForEgdeSegmentIsStartSegmentWithTravelledDistance(boolean reversed, int distance) {
        int edgeId = 1;
        int adjNode = 2;
        double weight = 0;
        long time = 0;
        isoLabel = createIsoLabel(edgeId, adjNode, weight, time, distance);
        // If reversed EdgeIterator returns reversed geometry
        var wayGeometry = reversed ? isoLabelWayGeometry.reverse() : isoLabelWayGeometry;
        when(queryGraph.getEdgeIteratorState(edgeId, adjNode)).thenReturn(edgeIteratorState);
        when(edgeIteratorStateReverseExtractor
                .hasReversed(edgeIteratorState)).thenReturn(reversed, false);
        when(edgeIteratorState.getFlags()).thenReturn(intsRef);
        when(startSegment.getClosestEdge()).thenReturn(edgeIteratorState);
        when(flagEncoder.getId(intsRef)).thenReturn(MATCHED_LINK_ID_ONE, MATCHED_LINK_ID_ONE);
        when(edgeIteratorState
                .fetchWayGeometry(3))
                .thenReturn(pointList);
        when(pointList.toLineString(false))
                .thenReturn(wayGeometry, startSegmentWayGeometry);
    }

    @SneakyThrows
    private static IsoLabel createIsoLabel(int edgeId,
            int adjNode,
            double weight,
            long time,
            double distance) {
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
