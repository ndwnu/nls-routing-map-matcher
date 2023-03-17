//package nu.ndw.nls.routingmapmatcher.graphhopper;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.mockito.Mockito.when;
//
//import java.nio.file.Path;
//import java.util.Collections;
//import nu.ndw.nls.routingmapmatcher.domain.model.Link;
//import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.LineString;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class NetworkGraphHopperFactoryTest {
//
//    private static final double LONG_1 = 5.358247;
//    private static final double LAT_1 = 52.161257;
//    private static final double LONG_2 = 5.379687;
//    private static final double LAT_2 = 52.158304;
//    private static final double LONG_3 = 5.379667;
//    private static final double LAT_3 = 52.158280;
//    private static final String TEST_NETWORK = "test network";
//    private static final long FROM_NODE_ID = 1;
//    private static final long TO_NODE_ID = 2;
//    private static final Coordinate coordinateA1 = new Coordinate(LONG_1, LAT_1);
//    private static final Coordinate coordinateA2 = new Coordinate(LONG_2, LAT_2);
//    private static final Coordinate coordinateA3 = new Coordinate(LONG_3, LAT_3);
//
//    public static final Path CUSTOM_GRAPHHOPPER_DIRECTORY = Path.of("CUSTOM_GRAPHHOPPER_DIRECTORY");
//    public static final String DEFAULT_GRAPHHOPPER_ROOT_DIRECTORY = "graphhopper_";
//
//    @Mock
//    private RoutingNetwork routingNetwork;
//
//    @Mock
//    private Link link;
//
//    @Mock
//    private LineString lineString;
//
//    private NetworkGraphHopperFactory networkGraphHopperFactory;
//
//    @BeforeEach
//    void setup() {
//        when(link.getFromNodeId()).thenReturn(FROM_NODE_ID);
//        when(link.getToNodeId()).thenReturn(TO_NODE_ID);
//        Coordinate[] coordinates = {coordinateA1, coordinateA2, coordinateA3};
//        when(lineString.getCoordinates()).thenReturn(coordinates);
//        when(link.getGeometry()).thenReturn(lineString);
//        when(routingNetwork.getNetworkNameAndVersion()).thenReturn(TEST_NETWORK);
//        when(routingNetwork.getLinkSupplier()).thenReturn(() -> Collections.singletonList(link).iterator());
//
//        networkGraphHopperFactory = new NetworkGraphHopperFactory();
//    }
//
//    @Test
//    void testCreateNetworkGraphHopper() {
//        NetworkGraphHopper graphHopper = networkGraphHopperFactory.createNetwork(routingNetwork);
//
//        assertThat(graphHopper.getDataReaderFile(),
//                is(Path.of(DEFAULT_GRAPHHOPPER_ROOT_DIRECTORY, TEST_NETWORK).toString()));
//        assertThat(graphHopper.getGraphHopperLocation(),
//                is(Path.of(DEFAULT_GRAPHHOPPER_ROOT_DIRECTORY, TEST_NETWORK).toString()));
//        assertFalse(graphHopper.isCHEnabled());
//        assertFalse(graphHopper.isAllowWrites());
//        assertFalse(graphHopper.hasElevation());
//    }
//
//    @Test
//    void testCreateNetworkGraphHopper_with_Network() {
//        NetworkGraphHopper graphHopper = networkGraphHopperFactory.createNetwork(routingNetwork, false,
//                CUSTOM_GRAPHHOPPER_DIRECTORY);
//
//        assertThat(graphHopper.getDataReaderFile(), is(CUSTOM_GRAPHHOPPER_DIRECTORY.resolve(TEST_NETWORK).toString()));
//        assertThat(graphHopper.getGraphHopperLocation(),
//                is(CUSTOM_GRAPHHOPPER_DIRECTORY.resolve(TEST_NETWORK).toString()));
//
//        assertFalse(graphHopper.isCHEnabled());
//        assertFalse(graphHopper.isAllowWrites());
//        assertFalse(graphHopper.hasElevation());
//    }
//}
