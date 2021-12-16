package nu.ndw.nls.routingmapmatcher.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.storage.index.QueryResult.Position;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PathUtilTest {

    public static final int ALL_NODES_MODE = 3;
    private PathUtil pathUtil;

    private final DistanceCalc distanceCalc = spy(new DistanceCalcEarth());

    @Mock
    private QueryResult queryResult;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private LinkFlagEncoder flagEncoder;

    @Mock
    private BooleanEncodedValue booleanEncodedValue;

    @BeforeEach
    void setUp() {
        this.pathUtil = new PathUtil(null);
    }

    @Test
    void determineSnappedPointFraction_startBaseNode() {
        when(this.queryResult.getSnappedPosition()).thenReturn(Position.TOWER);
        when(this.queryResult.getWayIndex()).thenReturn(0);

        assertEquals(0, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);
        verifyNoInteractions(this.distanceCalc);
    }

    @Test
    void determineSnappedPointFraction_endAdjacentNode() {
        when(this.queryResult.getSnappedPosition()).thenReturn(Position.TOWER);

        when(this.queryResult.getWayIndex()).thenReturn(1);
        assertEquals(1, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);

        when(this.queryResult.getWayIndex()).thenReturn(2);
        assertEquals(1, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);

        when(this.queryResult.getWayIndex()).thenReturn(3);
        assertEquals(1, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);

        verifyNoInteractions(this.distanceCalc);
    }

    @Test
    void determineSnappedPointFraction_pillarHalfWay() {
        when(this.flagEncoder.getAccessEnc()).thenReturn(booleanEncodedValue);
        when(this.edgeIteratorState.get(booleanEncodedValue)).thenReturn(true);
        when(this.edgeIteratorState.getReverse(booleanEncodedValue)).thenReturn(false);


        when(this.queryResult.getSnappedPosition()).thenReturn(Position.PILLAR);
        when(this.queryResult.getWayIndex()).thenReturn(0);

        when(this.queryResult.getSnappedPoint()).thenReturn(new GHPoint3D(1,0, 0));
        when(this.queryResult.getClosestEdge()).thenReturn(this.edgeIteratorState);

        PointList pointList = new PointList();
        pointList.add(0,0);
        pointList.add(1,0);
        pointList.add(2,0);
        when(this.edgeIteratorState.fetchWayGeometry(ALL_NODES_MODE)).thenReturn(pointList);

        assertEquals(0.5, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);

        verify(this.distanceCalc, times(2)).calcDist(0,0, 1, 0);
        verify(this.distanceCalc, times(1)).calcDist(1,0, 2, 0);
    }

    @Test
    void determineSnappedPointFraction_edgeHalfWay() {
        when(this.flagEncoder.getAccessEnc()).thenReturn(booleanEncodedValue);
        when(this.edgeIteratorState.get(booleanEncodedValue)).thenReturn(true);
        when(this.edgeIteratorState.getReverse(booleanEncodedValue)).thenReturn(false);

        when(this.queryResult.getSnappedPosition()).thenReturn(Position.EDGE);
        when(this.queryResult.getWayIndex()).thenReturn(0);

        when(this.queryResult.getSnappedPoint()).thenReturn(new GHPoint3D(1,0, 0));
        when(this.queryResult.getClosestEdge()).thenReturn(this.edgeIteratorState);

        PointList pointList = new PointList();
        pointList.add(0,0);
        pointList.add(2,0);
        when(this.edgeIteratorState.fetchWayGeometry(ALL_NODES_MODE)).thenReturn(pointList);

        assertEquals(0.5, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);

        // From start to snapped
        verify(this.distanceCalc, times(1)).calcDist(0,0, 1, 0);

        // Total length calculation
        verify(this.distanceCalc, times(1)).calcDist(0,0, 2, 0);
    }

    @Test
    void determineSnappedPointFraction_edgeQuarter() {
        when(this.flagEncoder.getAccessEnc()).thenReturn(booleanEncodedValue);
        when(this.edgeIteratorState.get(booleanEncodedValue)).thenReturn(true);
        when(this.edgeIteratorState.getReverse(booleanEncodedValue)).thenReturn(false);

        when(this.queryResult.getSnappedPosition()).thenReturn(Position.EDGE);
        when(this.queryResult.getWayIndex()).thenReturn(0);

        when(this.queryResult.getSnappedPoint()).thenReturn(new GHPoint3D(0.25,0, 0));
        when(this.queryResult.getClosestEdge()).thenReturn(this.edgeIteratorState);

        PointList pointList = new PointList();
        pointList.add(0,0);
        pointList.add(1,0);
        when(this.edgeIteratorState.fetchWayGeometry(ALL_NODES_MODE)).thenReturn(pointList);

        assertEquals(0.25, this.pathUtil.determineSnappedPointFraction(this.queryResult, distanceCalc,
                flagEncoder), 0.00001);

        // From start to snapped
        verify(this.distanceCalc, times(1)).calcDist(0,0, 0.25, 0);

        // Total length calculation
        verify(this.distanceCalc, times(1)).calcDist(0,0, 1, 0);
    }
}