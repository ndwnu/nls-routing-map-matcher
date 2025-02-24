package nu.ndw.nls.routingmapmatcher.network;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.HGV_ACCESSIBLE_KEY;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TEST_PROFILES;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestLinks;
import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.util.EdgeIteratorState;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class GraphHopperNetworkServiceIT {

    private static final String TEST_NETWORK = "test_network";

    private static final Instant DATA_DATE = Instant.parse("2023-11-07T15:37:23.129Z");
    private static final Instant DATA_DATE_TRUNCATED = Instant.parse("2023-11-07T15:37:23Z");
    private GraphHopperNetworkService graphHopperNetworkService;

    private Supplier<Iterator<TestLink>> iteratorSupplier;

    private GeometryFactoryWgs84 geometryFactoryWgs84 = new GeometryFactoryWgs84();

    @SneakyThrows
    @BeforeEach
    void setUp() {
        List<TestLink> links = getTestLinks("/test-data/network.geojson");
        iteratorSupplier = links::iterator;
        graphHopperNetworkService = TestNetworkProvider.NETWORK_SERVICE;
    }

    @SneakyThrows
    @Test
    void loadFromDisk_ok() {
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(Files.createDirectories(Path.of(TEST_NETWORK)))
                .linkSupplier(iteratorSupplier)
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();
        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);

        verifyCustomEncodedValue(networkGraphHopper, 3666256L, true);
    }


    @SneakyThrows
    @Test
    void loadFromDisk_ok_retrieveExtraEncodedValue() {

        Path tempDirectory = Files.createTempDirectory("graphhopper");

        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(tempDirectory)
                .linkSupplier(() -> List.of(
                        TestLink.builder()
                                .id(1)
                                .hgvAccessible(false)
                                .distanceInMeters(1)
                                .speedInKilometersPerHour(1)
                                .geometry(geometryFactoryWgs84.createLineString(new Coordinate[]{ new Coordinate(1,2),
                                                                                                  new Coordinate(3,4)}))
                                .fromNodeId(1)
                                .toNodeId(2)
                        .build(),
                        TestLink.builder()
                                .id(2)
                                .hgvAccessible(true)
                                .distanceInMeters(1)
                                .speedInKilometersPerHour(1)
                                .geometry(geometryFactoryWgs84.createLineString(new Coordinate[]{ new Coordinate(5,6),
                                                                                                  new Coordinate(7,8)}))
                                .fromNodeId(3)
                                .toNodeId(4)
                                .build()).iterator())
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();

        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);

        verifyCustomEncodedValue(networkGraphHopper, 1L, false);
        verifyCustomEncodedValue(networkGraphHopper, 2L, true);
    }


    private void verifyCustomEncodedValue( NetworkGraphHopper networkGraphHopper, Long roadSectionId,
            boolean expectedValue) {
        Integer edgeKey = networkGraphHopper.getEdgeMap().get(roadSectionId);
        EdgeIteratorState edge = networkGraphHopper.getBaseGraph().getEdgeIteratorStateForKey(edgeKey);
        BooleanEncodedValue encodedValue = networkGraphHopper.getEncodingManager()
                .getBooleanEncodedValue(HGV_ACCESSIBLE_KEY);

        assertThat(edge.get(encodedValue)).isEqualTo(expectedValue);
    }

    @SneakyThrows
    @Test
    void storeOnDisk_ok() {
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(Files.createDirectories(Path.of(TEST_NETWORK)))
                .linkSupplier(iteratorSupplier)
                .dataDate(DATA_DATE)
                .build();
        Path path = Path.of(TEST_NETWORK);
        Files.createDirectories(path);
        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        assertThat(path.resolve(TEST_NETWORK)).exists();
        assertThat(path.resolve(TEST_NETWORK).resolve("properties")).exists();
    }

    @SneakyThrows
    @AfterEach
    void removeDirectory() {
        Path path = Path.of(TEST_NETWORK);
        FileUtils.deleteDirectory(new File(path.toUri()));
    }

}
