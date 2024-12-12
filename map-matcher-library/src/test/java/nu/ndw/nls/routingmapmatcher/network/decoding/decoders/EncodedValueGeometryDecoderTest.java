package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import java.util.HashMap;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedValueGeometryDecoderTest {

    private static final long ROAD_SECTION_ID = 123;
    private static final int INTERNAL_EDGE_ID = 678;

    @Mock
    private GeometryFactoryWgs84 geometryFactoryWgs84;

    @InjectMocks
    private EncodedValueGeometryDecoder encodedValueGeometryDecoder;

    @Mock
    private NetworkGraphHopper networkGraphHopper;

    @Mock
    private BaseGraph baseGraph;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private LineString geometryLineString;

    @Mock
    private PointList geometryPointList;

    @Test
    void decode_ok_is2D() {
        HashMap<Long, Integer> edgeMap = new HashMap<>();
        edgeMap.put(ROAD_SECTION_ID, INTERNAL_EDGE_ID);
        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(INTERNAL_EDGE_ID)).thenReturn(edgeIteratorState);

        when(edgeIteratorState.fetchWayGeometry(FetchMode.ALL)).thenReturn(geometryPointList);

        when(geometryPointList.is3D()).thenReturn(false);
        when(geometryPointList.toLineString(false)).thenReturn(geometryLineString);

        assertThat(encodedValueGeometryDecoder.decode(networkGraphHopper, ROAD_SECTION_ID)).isEqualTo(geometryLineString);
    }


    @Test
    void decode_ok_is3D() {
        HashMap<Long, Integer> edgeMap = new HashMap<>();
        edgeMap.put(ROAD_SECTION_ID, INTERNAL_EDGE_ID);
        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(INTERNAL_EDGE_ID)).thenReturn(edgeIteratorState);

        when(edgeIteratorState.fetchWayGeometry(FetchMode.ALL)).thenReturn(geometryPointList);

        when(geometryPointList.is3D()).thenReturn(true);
        when(geometryPointList.toLineString(true)).thenReturn(geometryLineString);

        assertThat(encodedValueGeometryDecoder.decode(networkGraphHopper, ROAD_SECTION_ID)).isEqualTo(geometryLineString);
    }
}