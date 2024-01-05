package nu.ndw.nls.routingmapmatcher.graphhopper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private static final Coordinate coordinateA1 = new Coordinate(LONG_1, LAT_1);
    private static final Coordinate coordinateA2 = new Coordinate(LONG_2, LAT_2);
    private static final Coordinate coordinateA3 = new Coordinate(LONG_3, LAT_3);
    private static final Path CUSTOM_GRAPHHOPPER_DIRECTORY = Path.of("CUSTOM_GRAPHHOPPER_DIRECTORY");
    private static final String DEFAULT_GRAPHHOPPER_ROOT_DIRECTORY = "graphhopper_";
    private static final Instant DATA_DATE = Instant.parse("2023-11-07T15:37:23.129Z");
    private static final Instant DATA_DATE_TRUNCATED = Instant.parse("2023-11-07T15:37:23Z");
    private static final boolean EXPAND_BOUNDS = true;

    @Mock
    private RoutingNetwork routingNetwork;

    private Link link;

    @Mock
    private LineString lineString;

    private NetworkGraphHopperFactory networkGraphHopperFactory;

    @BeforeEach
    void setUp() {
        Coordinate[] coordinates = {coordinateA1, coordinateA2, coordinateA3};
        when(lineString.getCoordinates()).thenReturn(coordinates);
        link = Link.builder()
                .id(20L)
                .fromNodeId(FROM_NODE_ID)
                .toNodeId(TO_NODE_ID)
                .distanceInMeters(100)
                .geometry(lineString)
                .reverseSpeedInKilometersPerHour(0)
                .speedInKilometersPerHour(100)
                .build();
        when(routingNetwork.getNetworkNameAndVersion()).thenReturn(TEST_NETWORK);
        when(routingNetwork.getLinkSupplier()).thenReturn(() -> Collections.singletonList(link).iterator());
        when(routingNetwork.getDataDate()).thenReturn(DATA_DATE);
        when(routingNetwork.isExpandBounds()).thenReturn(EXPAND_BOUNDS);
        networkGraphHopperFactory = new NetworkGraphHopperFactory();
    }

    @Test
    void createNetwork_ok() {
        NetworkGraphHopper graphHopper = networkGraphHopperFactory.createNetwork(routingNetwork);
        assertThat(graphHopper.getGraphHopperLocation(),
                is(Path.of(DEFAULT_GRAPHHOPPER_ROOT_DIRECTORY, TEST_NETWORK).toString()));
        assertFalse(graphHopper.isAllowWrites());
        assertFalse(graphHopper.hasElevation());
        assertThat(graphHopper.getImportDate(), notNullValue());
        assertThat(graphHopper.getDataDate(), is(DATA_DATE_TRUNCATED));
        assertTrue(graphHopper.isExpandBounds());
    }

    @Test
    void createNetwork_ok_withNetwork() {
        NetworkGraphHopper graphHopper = networkGraphHopperFactory.createNetwork(routingNetwork, false,
                CUSTOM_GRAPHHOPPER_DIRECTORY);
        assertThat(graphHopper.getGraphHopperLocation(),
                is(CUSTOM_GRAPHHOPPER_DIRECTORY.resolve(TEST_NETWORK).toString()));
        assertFalse(graphHopper.isAllowWrites());
        assertFalse(graphHopper.hasElevation());
        assertThat(graphHopper.getImportDate(), notNullValue());
        assertThat(graphHopper.getDataDate(), is(DATA_DATE_TRUNCATED));
        assertTrue(graphHopper.isExpandBounds());
    }
}
