package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import static nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneTestHelper.createIsoLabel;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsochroneServiceTest {

    private static final double Y_COORDINATE = 1D;
    private static final double X_COORDINATE = 0D;
    private static final double ISOCHRONE_VALUE = 200D;
    private static final boolean REVERSED = false;
    private static final int START_NODE_ID = 1;
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
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE, queryGraph, startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        isochroneService.getUpstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));
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
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        isochroneService.getUpstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));
        verifyNoMoreInteractions(isochroneMatchMapper);
    }

    @Test
    void getUpstreamIsochroneMatches_ok_seconds() {
        /*var matchedPoint = MatchedPoint.builder()
                .reversed(REVERSED)
                .snappedPoint(point)
                .build();
        var isoLabel = createIsoLabel(100, 30000);
        setupFixture(isoLabel);
        when(location.getUpstreamIsochrone()).thenReturn(ISOCHRONE_VALUE);
        when(location.getUpstreamIsochroneUnit()).thenReturn(IsochroneUnit.METERS);
        when(isochroneMatchMapper.mapToIsochroneMatch(isoLabel, ISOCHRONE_VALUE, queryGraph, startSegment)).thenReturn(
                IsochroneMatch.builder().build());
        isochroneService.getUpstreamIsochroneMatches(matchedPoint, queryGraph, location, locationIndexTree);
        verify(queryGraph).lookup(List.of(startSegment));*/
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
        when(isochroneFactory.createIsochrone(queryGraph,
                ISOCHRONE_VALUE, IsochroneUnit.METERS, true))
                .thenReturn(isochrone);
        when(startSegment.getClosestNode()).thenReturn(START_NODE_ID);
        when(isochrone.search(START_NODE_ID)).thenReturn(List.of(isoLabel));
    }

    @Test
    void getDownstreamIsochroneMatches_ok() {
    }

    @Test
    void getUpstreamLinkIds_ok() {
    }

    @Test
    void getDownstreamLinkIds_ok() {
    }
}
