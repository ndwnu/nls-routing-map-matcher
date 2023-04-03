
package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkWayIdEncodedValuesFactory.ID_NAME;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
import static nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneTestHelper.createIsoLabel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import java.util.Set;
import java.util.function.Consumer;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.ShortestPathTree.IsoLabel;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsochroneServiceTest {

    private static final double Y_COORDINATE = 1D;
    private static final double X_COORDINATE = 0D;
    private static final double ISOCHRONE_VALUE_METERS = 200D;
    private static final boolean REVERSED = false;
    private static final int START_NODE_ID = 1;
    private static final double ISOCHRONE_VALUE_SECONDS = 8D;
    private static final double SPEED = 100D;
    @Mock
    private EncodingManager encodingManager;
    @Mock
    private BaseGraph baseGraph;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private IsochroneMatchMapper isochroneMatchMapper;
    @Mock
    private ShortestPathTreeFactory shortestPathTreeFactory;
    @Mock
    private QueryGraph queryGraph;
    @Mock
    private LocationIndexTree locationIndexTree;
    @Mock
    private Point point;
    @Mock
    private Snap startSegment;
    @Mock
    private BaseLocation location;
    @Mock
    private ShortestPathTree shortestPathTree;

    @Mock
    private EdgeIteratorState startEdge;

    @Mock
    private EdgeIteratorState currentEdge;
    @Mock
    private DecimalEncodedValue decimalEncodedValue;
    @Mock
    private BooleanEncodedValue booleanEncodedValue;
    @Captor
    private ArgumentCaptor<Double> maxDistanceArgumentCaptor;

    @Mock
    private IntEncodedValue intEncodedValue;

    @InjectMocks
    private IsochroneService isochroneService;

    @Test
    void getUpstreamIsochroneMatches_ok_meters() {
        MatchedPoint matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture(isoLabel);
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE_METERS, queryGraph,
                startSegment))
                .thenReturn(IsochroneMatch.builder().build());
        wrapWithStaticMock(() -> isochroneService.getUpstreamIsochroneMatches(matchedPoint, location));
        verify(shortestPathTreeFactory).createShortestPathtree(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, true);


    }

    @Test
    void getUpstreamIsochroneMatches_ok_filter() {
        MatchedPoint matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture(isoLabel);
        setupFixtureForFilter(isoLabel);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        wrapWithStaticMock(() -> isochroneService.getUpstreamIsochroneMatches(matchedPoint, location));
        verifyNoMoreInteractions(isochroneMatchMapper);
    }

    @Test
    void getUpstreamIsochroneMatches_ok_seconds() {
        MatchedPoint matchedPoint = MatchedPoint.builder()
                .reversed(false)
                .snappedPoint(point)
                .build();

        IsoLabel endLabel = createIsoLabel(200, 10800, 1, 2, 10800);
        setupFixture(endLabel);

        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_SECONDS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.SECONDS);
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt()))
                .thenReturn(currentEdge);
        // Segment with average speed of 100 km ph 27.77 meters/second
        when(encodingManager.getDecimalEncodedValue(VehicleSpeed.key(VEHICLE_CAR))).thenReturn(decimalEncodedValue);
        when(currentEdge.get(decimalEncodedValue)).thenReturn(SPEED);
        wrapWithStaticMock(() -> isochroneService.getUpstreamIsochroneMatches(matchedPoint, location));
        verify(isochroneMatchMapper).mapToIsochroneMatch(eq(endLabel),
                maxDistanceArgumentCaptor.capture(), eq(queryGraph),
                eq(startSegment));
        verify(shortestPathTreeFactory).createShortestPathtree(queryGraph, ISOCHRONE_VALUE_SECONDS,
                IsochroneUnit.SECONDS, true);
        Double maxDistance = maxDistanceArgumentCaptor.getValue();
        // The max distance based on 8 seconds will be around 200 - ((10.8-8) * 27.77 meters/second) ~ 122.2 meters
        assertThat(maxDistance).isCloseTo(122.2, Percentage.withPercentage(0.1));
    }

    private void wrapWithStaticMock(Runnable function) {
        try (MockedStatic<QueryGraph> queryGraphStaticMock = Mockito.mockStatic(QueryGraph.class)) {
            queryGraphStaticMock.when(() -> QueryGraph.create(eq(baseGraph), any(Snap.class)))
                    .thenReturn(queryGraph);
            function.run();
        }
    }

    @Test
    void getDownstreamIsochroneMatches_ok_meters() {
        MatchedPoint matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture(isoLabel);
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getDownstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getDownstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE_METERS, queryGraph,
                startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        wrapWithStaticMock(() -> isochroneService.getDownstreamIsochroneMatches(matchedPoint, location));
        verify(shortestPathTreeFactory).createShortestPathtree(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, false);

    }

    @Test
    void getUpstreamLinkIds_ok() {
        when(shortestPathTreeFactory.createShortestPathtree(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, true))
                .thenReturn(shortestPathTree);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        IsoLabel isoLabel = createIsoLabel(100, 0);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(shortestPathTree).search(eq(START_NODE_ID), any());
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt())).thenReturn(currentEdge);
        when(encodingManager.getIntEncodedValue(ID_NAME)).thenReturn(intEncodedValue);
        when(currentEdge.get(intEncodedValue)).thenReturn(START_NODE_ID);
        Set<Integer> result = isochroneService.getUpstreamLinkIds(queryGraph, location, START_NODE_ID);
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(START_NODE_ID);
    }

    @Test
    void getDownstreamLinkIds_ok() {
        when(shortestPathTreeFactory.createShortestPathtree(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, false))
                .thenReturn(shortestPathTree);
        when(location.getDownstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getDownstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        IsoLabel isoLabel = createIsoLabel(100, 0);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(shortestPathTree).search(eq(START_NODE_ID), any());
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt())).thenReturn(currentEdge);
        when(encodingManager.getIntEncodedValue(ID_NAME)).thenReturn(intEncodedValue);
        when(currentEdge.get(intEncodedValue)).thenReturn(START_NODE_ID);
        Set<Integer> result = isochroneService.getDownstreamLinkIds(queryGraph, location, START_NODE_ID);
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(START_NODE_ID);
    }

    private void setupFixtureForFilter(IsoLabel isoLabel) {
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(true);
        when(isochroneMatchMapper.isStartSegment(0, startSegment)).thenReturn(true);
        when(queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.node)).thenReturn(currentEdge);
        when(edgeIteratorStateReverseExtractor.hasReversed(currentEdge)).thenReturn(false);
    }


    private void setupFixture(IsoLabel isoLabel) {
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(shortestPathTree).search(eq(START_NODE_ID), any());

        when(point.getY()).thenReturn(Y_COORDINATE);
        when(point.getX()).thenReturn(X_COORDINATE);
        when(locationIndexTree.findClosest(Y_COORDINATE, X_COORDINATE,
                EdgeFilter.ALL_EDGES))
                .thenReturn(startSegment);
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(shortestPathTreeFactory.createShortestPathtree(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(shortestPathTree);
        when(startSegment.getClosestNode()).thenReturn(START_NODE_ID);
    }
}

