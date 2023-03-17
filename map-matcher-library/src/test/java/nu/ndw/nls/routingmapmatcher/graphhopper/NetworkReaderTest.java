//package nu.ndw.nls.routingmapmatcher.graphhopper;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyDouble;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import com.graphhopper.coll.LongIntMap;
//import com.graphhopper.routing.util.EncodingManager;
//import com.graphhopper.storage.GraphHopperStorage;
//import com.graphhopper.util.EdgeIteratorState;
//import com.graphhopper.util.PointList;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.List;
//import java.util.function.Supplier;
//import nu.ndw.nls.routingmapmatcher.domain.model.Link;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.LineString;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class NetworkReaderTest {
//
//    private static final double LONG_1 = 5.358247;
//    private static final double LAT_1 = 52.161257;
//    private static final double LONG_2 = 5.379687;
//    private static final double LAT_2 = 52.158304;
//    private static final double LONG_3 = 5.379667;
//    private static final double LAT_3 = 52.158280;
//
//    private static final long FROM_NODE_ID = 1;
//    private static final long TO_NODE_ID = 2;
//    private static final Coordinate coordinateA1 = new Coordinate(LONG_1, LAT_1);
//    private static final Coordinate coordinateA2 = new Coordinate(LONG_2, LAT_2);
//    private static final Coordinate coordinateA3 = new Coordinate(LONG_3, LAT_3);
//
//    @Captor
//    private ArgumentCaptor<PointList> pointListArgumentCaptor;
//
//    @Mock
//    private EncodingManager encodingManager;
//    @Mock
//    private GraphHopperStorage ghStorage;
//
//    @Mock
//    private EdgeIteratorState edgeIteratorState;
//
//    @Mock
//    private LineString lineString;
//
//    @Mock
//    private EdgeIteratorState edge;
//
//    @Mock
//    private Link link;
//    @Mock
//    private LongIntMap nodeIdToInternalNodeIdMap;
//
//    private NetworkReader networkReader;
//
//    @BeforeEach
//    void setup() {
//        when(ghStorage.getEncodingManager()).thenReturn(encodingManager);
//        List<Link> links = Collections.singletonList(link);
//        Supplier<Iterator<Link>> networkSupplier = links::iterator;
//        networkReader = new NetworkReader(ghStorage, networkSupplier, nodeIdToInternalNodeIdMap);
//    }
//
//    @Test
//    void testReadGraph() {
//        when(ghStorage.edge(anyInt(), anyInt())).thenReturn(edge);
//        when(edge.setDistance(anyDouble())).thenReturn(edge);
//        when(edge.setFlags(any())).thenReturn(edge);
//        when(edge.setWayGeometry(any())).thenReturn(edge);
//        when(link.getFromNodeId()).thenReturn(FROM_NODE_ID);
//        when(link.getToNodeId()).thenReturn(TO_NODE_ID);
//        Coordinate[] coordinates = {coordinateA1, coordinateA2, coordinateA3};
//        when(lineString.getCoordinates()).thenReturn(coordinates);
//        when(link.getGeometry()).thenReturn(lineString);
//        networkReader.readGraph();
//        verify(edge).setWayGeometry(pointListArgumentCaptor.capture());
//        PointList pointList = pointListArgumentCaptor.getValue();
//        assertThat(pointList.getSize(), is(1));
//        assertThat(pointList.getLon(0), is(LONG_2));
//        assertThat(pointList.getLat(0), is(LAT_2));
//    }
//}
