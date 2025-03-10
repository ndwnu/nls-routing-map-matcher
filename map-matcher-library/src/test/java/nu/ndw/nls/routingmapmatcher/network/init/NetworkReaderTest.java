package nu.ndw.nls.routingmapmatcher.network.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.graphhopper.coll.GHLongLongBTree;
import com.graphhopper.coll.LongLongMap;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import java.util.List;
import java.util.Map;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkReaderTest {

    private static final double LONG_1 = 5.358247;
    private static final double LAT_1 = 52.161257;
    private static final double LONG_2 = 5.379687;
    private static final double LAT_2 = 52.158304;
    private static final double LONG_3 = 5.379667;
    private static final double LAT_3 = 52.158280;

    // Link fields
    private static final long FROM_NODE_ID = 101;
    private static final long TO_NODE_ID = 102;
    private static final double DISTANCE = 12.15;
    private static final long LINK_ID = 100;

    // Internal fields
    private static final int FROM_NODE_ID_INTERNAL = 0;
    private static final int TO_NODE_ID_INTERNAL = 1;
    private static final int EDGE_ID = 2;
    private static final int EDGE_KEY = 3;

    private static final Coordinate coordinateA1 = new Coordinate(LONG_1, LAT_1);
    private static final Coordinate coordinateA2 = new Coordinate(LONG_2, LAT_2);
    private static final Coordinate coordinateA3 = new Coordinate(LONG_3, LAT_3);

    @Captor
    private ArgumentCaptor<PointList> pointListArgumentCaptor;

    @Mock
    private BaseGraph baseGraph;
    @Mock
    private BBox bBox;
    @Mock
    private TestLink link;
    @Mock
    private TagParser tagParser;

    private LongLongMap nodeIdToInternalNodeIdMap;
    @Mock
    private Map<Long, Integer> edgeMap;

    @Mock
    private EdgeIntAccess edgeIntAccess;
    @Mock
    private LineString lineString;
    @Mock
    private NodeAccess nodeAccess;
    @Mock
    private EdgeIteratorState edge;

    private NetworkReader networkReader;

    @Test
    void readGraph_ok_doNotExpandBounds() {
        setupWithExpandBoundsParameter(false);

        networkReader.readGraph();

        verifyResults();
        verifyNoMoreInteractions(bBox);
    }

    @Test
    void readGraph_ok_expandBounds() {
        setupWithExpandBoundsParameter(true);
        when(baseGraph.getBounds()).thenReturn(bBox);

        networkReader.readGraph();

        verifyResults();

        verify(bBox).update(LAT_1 + .000001, LONG_1 + .000001);
        verify(bBox).update(LAT_1 - .000001, LONG_1 - .000001);
        verify(bBox).update(LAT_2 + .000001, LONG_2 + .000001);
        verify(bBox).update(LAT_2 - .000001, LONG_2 - .000001);
        verify(bBox).update(LAT_3 + .000001, LONG_3 + .000001);
        verify(bBox).update(LAT_3 - .000001, LONG_3 - .000001);
    }

    void setupWithExpandBoundsParameter(boolean expandBounds) {
        // Use parameters from NetworkGraphHopper class
        nodeIdToInternalNodeIdMap = new GHLongLongBTree(200, 4, -1);
        when(baseGraph.getEdgeAccess()).thenReturn(edgeIntAccess);
        networkReader = new NetworkReader(baseGraph, List.of(link)::iterator, List.of(tagParser),
                nodeIdToInternalNodeIdMap, edgeMap, expandBounds);

        when(link.getGeometry()).thenReturn(lineString);
        Coordinate[] coordinates = {coordinateA1, coordinateA2, coordinateA3};
        when(lineString.getCoordinates()).thenReturn(coordinates);
        when(link.getFromNodeId()).thenReturn(FROM_NODE_ID);
        when(link.getToNodeId()).thenReturn(TO_NODE_ID);
        when(baseGraph.getNodeAccess()).thenReturn(nodeAccess);
        when(baseGraph.edge(FROM_NODE_ID_INTERNAL, TO_NODE_ID_INTERNAL)).thenReturn(edge);
        when(link.getDistanceInMeters()).thenReturn(DISTANCE);
        when(edge.setDistance(DISTANCE)).thenReturn(edge);
        when(edge.getEdge()).thenReturn(EDGE_ID);
        when(edge.getEdgeKey()).thenReturn(EDGE_KEY);
        when(link.getId()).thenReturn(LINK_ID);
    }

    private void verifyResults() {
        assertEquals(FROM_NODE_ID_INTERNAL, nodeIdToInternalNodeIdMap.get(FROM_NODE_ID));
        assertEquals(TO_NODE_ID_INTERNAL, nodeIdToInternalNodeIdMap.get(TO_NODE_ID));
        verify(nodeAccess).setNode(FROM_NODE_ID_INTERNAL, LAT_1, LONG_1);
        verify(nodeAccess).setNode(TO_NODE_ID_INTERNAL, LAT_3, LONG_3);
        verify(tagParser).handleWayTags(EDGE_ID, edgeIntAccess, link, IntsRef.EMPTY);
        verify(edge).setWayGeometry(pointListArgumentCaptor.capture());
        PointList pointList = pointListArgumentCaptor.getValue();
        assertEquals(1, pointList.size());
        assertEquals(LONG_2, pointList.getLon(0));
        assertEquals(LAT_2, pointList.getLat(0));
        verify(edgeMap).put(LINK_ID, EDGE_KEY);
    }

}
