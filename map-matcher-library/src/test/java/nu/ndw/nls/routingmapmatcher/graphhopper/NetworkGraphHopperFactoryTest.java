package nu.ndw.nls.routingmapmatcher.graphhopper;

import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkGraphHopperFactoryTest {
    private static final double LONG_1 = 5.358247;
    private static final double LAT_1 = 52.161257;
    private static final double LONG_2 = 5.379687;
    private static final double LAT_2 = 52.158304;
    private static final double LONG_3 = 5.379667;
    private static final double LAT_3 = 52.158280;
    private static final String TEST_NETWORK = "test network";
    private static final long FROM_NODE_ID = 1;
    private static final long TO_NODE_ID = 2;
    @Mock
    private RoutingNetwork routingNetwork;

    @Mock
    private Link link;

    @Mock
    private LineString lineString;

    private Coordinate coordinateA1;

    private Coordinate coordinateA2;

    private Coordinate coordinateA3;


    private NetworkGraphHopperFactory networkGraphHopperFactory;

    @BeforeEach
    void setup() {
        networkGraphHopperFactory = new NetworkGraphHopperFactory();
        when(link.getFromNodeId()).thenReturn(FROM_NODE_ID);
        when(link.getToNodeId()).thenReturn(TO_NODE_ID);
        createCoordinates();
        final Coordinate[] coordinates = {coordinateA1, coordinateA2, coordinateA3};
        when(lineString.getCoordinates()).thenReturn(coordinates);
        when(link.getGeometry()).thenReturn(lineString);

    }

    @Test
    void testCreateNetworkGraphHopper() {
        when(routingNetwork.getNetworkNameAndVersion()).thenReturn(TEST_NETWORK);
        when(routingNetwork.getLinkSupplier()).thenReturn(() -> Collections.singletonList(link).iterator());
        NetworkGraphHopper graphHopper = networkGraphHopperFactory.createNetworkGraphHopper(routingNetwork);
        assertThat(graphHopper.getDataReaderFile(), is("graphhopper_" + TEST_NETWORK));
        assertThat(graphHopper.getGraphHopperLocation(), is("graphhopper_" + TEST_NETWORK));

    }

    void createCoordinates() {
        coordinateA1 = new Coordinate(LONG_1, LAT_1);
        coordinateA2 = new Coordinate(LONG_2, LAT_2);
        coordinateA3 = new Coordinate(LONG_3, LAT_3);
    }
}
