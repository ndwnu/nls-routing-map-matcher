package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import static nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneTestHelper.createIsoLabel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsochroneServiceTest {

    private static final double Y_COORDINATE = 1D;
    private static final double X_COORDINATE = 0D;
    private static final double ISOCHRONE_VALUE_METERS = 200D;
    private static final boolean REVERSED = false;
    private static final int START_NODE_ID = 1;
    private static final double ISOCHRONE_VALUE_SECONDS = 8D;
    @Mock
    private LinkFlagEncoder flagEncoder;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private IsochroneMatchMapper isochroneMatchMapper;
    @Mock
    private IsochroneFactory isochroneFactory;
    @Mock
    private QueryGraph queryGraph;
    @Mock
    private LocationIndexTree locationIndexTree;
    @Mock
    private Point point;
    @Mock
    private QueryResult startSegment;
    @Mock
    private BaseLocation location;
    @Mock
    private Isochrone isochrone;

    @Mock
    private EdgeIteratorState startEdge;

    @Mock
    private EdgeIteratorState currentEdge;

    @Mock
    private BooleanEncodedValue booleanEncodedValue;
    @Mock
    private DecimalEncodedValue decimalEncodedValue;
    @Captor
    private ArgumentCaptor<Double> maxDistanceArgumentCaptor;

    @Mock
    private IntsRef intsRef;

    @InjectMocks
    private IsochroneService isochroneService;

    @Test
    void getUpstreamIsochroneMatches_ok_meters() {
        var matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        var isoLabel = createIsoLabel(100, 0);
        setupFixture(isoLabel);
        when(startEdge.get(any(BooleanEncodedValue.class))).thenReturn(true);
        when(startEdge.getReverse(any(BooleanEncodedValue.class))).thenReturn(false);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE_METERS, queryGraph,
                startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        isochroneService.getUpstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));
        verify(isochroneFactory).createIsochrone(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, true);


    }

    @Test
    void getUpstreamIsochroneMatches_ok_filter() {
        var matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        var isoLabel = createIsoLabel(100, 0);
        setupFixture(isoLabel);
        setupFixtureForFilter(isoLabel);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        isochroneService.getUpstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));
        verifyNoMoreInteractions(isochroneMatchMapper);
    }

    @Test
    void getUpstreamIsochroneMatches_ok_seconds() {
        var matchedPoint = MatchedPoint.builder()
                .reversed(false)
                .snappedPoint(point)
                .build();
        var rootLabel = createIsoLabel(0, 0, -1, -1, 0);
        // start segment with average speed 50 km ph  traverses 100 meters in  100 / ((50 * 1000 / 3600)) seconds ~ 7.2 seconds;
        var startLabel = createIsoLabel(100, 7200, 1, 1, 10800);
        startLabel.parent = rootLabel;
        // end segment with average speed 100 km ph  traverses 100 meters in 100 / ((100 * 1000 / 3600)) ~ 3.6  seconds
        var endLabel = createIsoLabel(200, 10800, 2, 2, 10800);
        // the max distance based on 8 seconds will be around 100 + (0.8 * 27.7 meters/second) ~ 122.2 meters
        endLabel.parent = startLabel;
        setupFixture(endLabel);
        when(startEdge.get(any(BooleanEncodedValue.class))).thenReturn(true);
        when(startEdge.getReverse(any(BooleanEncodedValue.class))).thenReturn(false);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_SECONDS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.SECONDS);
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt()))
                .thenReturn(currentEdge);
        when(flagEncoder.getAverageSpeedEnc()).thenReturn(decimalEncodedValue);
        when(currentEdge.get(decimalEncodedValue)).thenReturn(100D, 50D);
        isochroneService.getUpstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));
        verify(isochroneMatchMapper).mapToIsochroneMatch(eq(endLabel),
                maxDistanceArgumentCaptor.capture(), eq(queryGraph),
                eq(startSegment));
        verify(isochroneFactory).createIsochrone(queryGraph, ISOCHRONE_VALUE_SECONDS,
                IsochroneUnit.SECONDS, true);
        var maxDistance = maxDistanceArgumentCaptor.getValue();
        assertThat(maxDistance).isCloseTo(122.2, Percentage.withPercentage(0.1));
    }

    @Test
    void getDownstreamIsochroneMatches_ok_meters() {
        var matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        var isoLabel = createIsoLabel(100, 0);
        setupFixture(isoLabel);
        when(startEdge.get(any(BooleanEncodedValue.class))).thenReturn(true);
        when(startEdge.getReverse(any(BooleanEncodedValue.class))).thenReturn(false);
        when(location.getDownstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getDownstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE_METERS, queryGraph,
                startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        isochroneService.getDownstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));
        verify(isochroneFactory).createIsochrone(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, false);

    }


    @Test
    void getUpstreamLinkIds_ok() {
        when(isochroneFactory.createIsochrone(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, true))
                .thenReturn(isochrone);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        var isoLabel = createIsoLabel(100, 0);
        when(isochrone.search(anyInt())).thenReturn(List.of(isoLabel));
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt())).thenReturn(currentEdge);
        when(currentEdge.getFlags()).thenReturn(intsRef);
        when(flagEncoder.getId(intsRef)).thenReturn(START_NODE_ID);
        var result = isochroneService.getUpstreamLinkIds(queryGraph, location, START_NODE_ID);
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(START_NODE_ID);
    }

    @Test
    void getDownstreamLinkIds_ok() {
        when(isochroneFactory.createIsochrone(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, false))
                .thenReturn(isochrone);
        when(location.getDownstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getDownstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        var isoLabel = createIsoLabel(100, 0);
        when(isochrone.search(anyInt())).thenReturn(List.of(isoLabel));
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt())).thenReturn(currentEdge);
        when(currentEdge.getFlags()).thenReturn(intsRef);
        when(flagEncoder.getId(intsRef)).thenReturn(START_NODE_ID);
        var result = isochroneService.getDownstreamLinkIds(queryGraph, location, START_NODE_ID);
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(START_NODE_ID);
    }

    private void setupFixtureForFilter(IsoLabel isoLabel) {
        when(startEdge.get(any(BooleanEncodedValue.class))).thenReturn(true);
        when(startEdge.getReverse(any(BooleanEncodedValue.class))).thenReturn(true);
        when(isochroneMatchMapper.isStartSegment(0, startSegment)).thenReturn(true);
        when(queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode)).thenReturn(currentEdge);
        when(edgeIteratorStateReverseExtractor.hasReversed(currentEdge)).thenReturn(true);
    }


    private void setupFixture(IsoLabel isoLabel) {
        when(point.getY()).thenReturn(Y_COORDINATE);
        when(point.getX()).thenReturn(X_COORDINATE);
        when(locationIndexTree.findClosest(Y_COORDINATE, X_COORDINATE,
                EdgeFilter.ALL_EDGES))
                .thenReturn(startSegment);
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(flagEncoder.getAccessEnc()).thenReturn(booleanEncodedValue);
        when(isochroneFactory.createIsochrone(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(isochrone);
        when(startSegment.getClosestNode()).thenReturn(START_NODE_ID);
        when(isochrone.search(START_NODE_ID)).thenReturn(List.of(isoLabel));
    }
}
