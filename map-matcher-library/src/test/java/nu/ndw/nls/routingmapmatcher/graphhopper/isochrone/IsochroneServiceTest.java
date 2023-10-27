
package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

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
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import java.util.function.Consumer;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm.IsochroneByMunicipality;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm.IsochroneByTimeDistanceAndWeight;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
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
    private static final int START_NODE_ID = 1;
    private static final double ISOCHRONE_VALUE_SECONDS = 8D;
    private static final double SPEED = 100D;
    private static final double REVERSE_SPEED = 50D;
    public static final boolean REVERSED = false;
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
    private IsochroneByTimeDistanceAndWeight isochroneByTimeDistanceAndWeight;

    @Mock
    private IsochroneByMunicipality isochroneByMunicipality;
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

    @Mock
    private Weighting weighting;

    @InjectMocks
    private IsochroneService isochroneService;

    @Test
    void getUpstreamIsochroneMatches_ok_meters() {
        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture();
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(shortestPathTreeFactory.createShortestPathTree(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(isochroneByTimeDistanceAndWeight);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(isochroneByTimeDistanceAndWeight).search(eq(START_NODE_ID), any());
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE_METERS, queryGraph,
                startSegment))
                .thenReturn(IsochroneMatch.builder().build());
        wrapWithStaticMock(() -> isochroneService.getUpstreamIsochroneMatches(point,
                REVERSED,
                location));
        verify(shortestPathTreeFactory).createShortestPathTree(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, true);


    }

    @Test
    void getUpstreamIsochroneMatches_ok_filter() {
        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture();
        setupFixtureForFilter(isoLabel);
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(shortestPathTreeFactory.createShortestPathTree(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(isochroneByTimeDistanceAndWeight);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(isochroneByTimeDistanceAndWeight).search(eq(START_NODE_ID), any());

        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        wrapWithStaticMock(() -> isochroneService.getUpstreamIsochroneMatches(point,
                REVERSED, location));
        verifyNoMoreInteractions(isochroneMatchMapper);
    }

    @Test
    void getUpstreamIsochroneMatches_ok_seconds() {
        IsoLabel endLabel = createIsoLabel(200, 10800, 1, 2, 10800);
        setupFixture();
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(shortestPathTreeFactory.createShortestPathTree(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(isochroneByTimeDistanceAndWeight);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(endLabel);
            return null;
        }).when(isochroneByTimeDistanceAndWeight).search(eq(START_NODE_ID), any());
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_SECONDS);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.SECONDS);
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt()))
                .thenReturn(currentEdge);
        // Segment with average speed of 50 km ph 13.89 meters/second
        when(encodingManager.getDecimalEncodedValue(VehicleSpeed.key(VEHICLE_CAR))).thenReturn(decimalEncodedValue);
        when(currentEdge.getReverse(decimalEncodedValue)).thenReturn(REVERSE_SPEED);
        wrapWithStaticMock(() -> isochroneService.getUpstreamIsochroneMatches(point, REVERSED, location));
        verify(isochroneMatchMapper).mapToIsochroneMatch(eq(endLabel),
                maxDistanceArgumentCaptor.capture(), eq(queryGraph),
                eq(startSegment));
        verify(shortestPathTreeFactory).createShortestPathTree(queryGraph, ISOCHRONE_VALUE_SECONDS,
                IsochroneUnit.SECONDS, true);
        Double maxDistance = maxDistanceArgumentCaptor.getValue();
        // The max distance based on 8 seconds will be around 200 - ((10.8-8) * 13.89 meters/second) ~ 161.1 meters
        assertThat(maxDistance).isCloseTo(161.1, Percentage.withPercentage(0.1));
    }

    @Test
    void getDownstreamIsochroneMatches_ok_seconds() {

        IsoLabel endLabel = createIsoLabel(200, 10800, 1, 2, 10800);
        setupFixture();
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(shortestPathTreeFactory.createShortestPathTree(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(isochroneByTimeDistanceAndWeight);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(endLabel);
            return null;
        }).when(isochroneByTimeDistanceAndWeight).search(eq(START_NODE_ID), any());
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getDownstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_SECONDS);
        when(location.getDownstreamIsochroneUnit()).thenReturn(IsochroneUnit.SECONDS);
        when(queryGraph.getEdgeIteratorState(anyInt(), anyInt()))
                .thenReturn(currentEdge);
        // Segment with average speed of 100 km ph 27.77 meters/second
        when(encodingManager.getDecimalEncodedValue(VehicleSpeed.key(VEHICLE_CAR))).thenReturn(decimalEncodedValue);
        when(currentEdge.get(decimalEncodedValue)).thenReturn(SPEED);
        wrapWithStaticMock(() -> isochroneService.getDownstreamIsochroneMatches(point, REVERSED, location));
        verify(isochroneMatchMapper).mapToIsochroneMatch(eq(endLabel),
                maxDistanceArgumentCaptor.capture(), eq(queryGraph),
                eq(startSegment));
        verify(shortestPathTreeFactory).createShortestPathTree(queryGraph, ISOCHRONE_VALUE_SECONDS,
                IsochroneUnit.SECONDS, false);
        Double maxDistance = maxDistanceArgumentCaptor.getValue();
        // The max distance based on 8 seconds will be around 200 - ((10.8-8) * 27.77 meters/second) ~ 122.2 meters
        assertThat(maxDistance).isCloseTo(122.2, Percentage.withPercentage(0.1));
    }

    @Test
    void getIsochroneMatchesByMunicipalityId_ok() {
        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture();
        when(shortestPathTreeFactory.createShortestPathTree(any(), any(), any(),
                anyInt()))
                .thenReturn(isochroneByMunicipality);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(isochroneByMunicipality).search(eq(START_NODE_ID), any());
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, Double.POSITIVE_INFINITY, queryGraph,
                startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        wrapWithStaticMock(() -> isochroneService.getIsochroneMatchesByMunicipalityId(weighting, point, 1));
        verify(shortestPathTreeFactory).createShortestPathTree(queryGraph,weighting,encodingManager,1);
    }

    @Test
    void getDownstreamIsochroneMatches_ok_meters() {

        IsoLabel isoLabel = createIsoLabel(100, 0);
        setupFixture();
        when(startSegment.getClosestEdge()).thenReturn(startEdge);
        when(shortestPathTreeFactory.createShortestPathTree(any(),
                anyDouble(), any(), anyBoolean()))
                .thenReturn(isochroneByTimeDistanceAndWeight);
        doAnswer(ans -> {
            Consumer<IsoLabel> callback = ans.getArgument(1, Consumer.class);
            callback.accept(isoLabel);
            return null;
        }).when(isochroneByTimeDistanceAndWeight).search(eq(START_NODE_ID), any());
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(false);
        when(location.getDownstreamIsochrone()).thenReturn(ISOCHRONE_VALUE_METERS);
        when(location.getDownstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE_METERS, queryGraph,
                startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        wrapWithStaticMock(() -> isochroneService.getDownstreamIsochroneMatches(point, REVERSED, location));
        verify(shortestPathTreeFactory).createShortestPathTree(queryGraph, ISOCHRONE_VALUE_METERS,
                IsochroneUnit.METERS, false);

    }
    private void wrapWithStaticMock(Runnable function) {
        try (MockedStatic<QueryGraph> queryGraphStaticMock = Mockito.mockStatic(QueryGraph.class)) {
            queryGraphStaticMock.when(() -> QueryGraph.create(eq(baseGraph), any(Snap.class)))
                    .thenReturn(queryGraph);
            function.run();
        }
    }




    private void setupFixtureForFilter(IsoLabel isoLabel) {
        when(encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR))).thenReturn(booleanEncodedValue);
        when(startEdge.get(booleanEncodedValue)).thenReturn(true);
        when(startEdge.getReverse(booleanEncodedValue)).thenReturn(true);
        when(isochroneMatchMapper.isStartSegment(0, startSegment)).thenReturn(true);
        when(queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.node)).thenReturn(currentEdge);
        when(edgeIteratorStateReverseExtractor.hasReversed(currentEdge)).thenReturn(false);
    }



    private void setupFixture() {
        when(point.getY()).thenReturn(Y_COORDINATE);
        when(point.getX()).thenReturn(X_COORDINATE);
        when(locationIndexTree.findClosest(Y_COORDINATE, X_COORDINATE,
                EdgeFilter.ALL_EDGES))
                .thenReturn(startSegment);
        when(startSegment.getClosestNode()).thenReturn(START_NODE_ID);
    }
}

