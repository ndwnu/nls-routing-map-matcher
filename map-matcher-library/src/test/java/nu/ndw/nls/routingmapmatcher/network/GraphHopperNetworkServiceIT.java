package nu.ndw.nls.routingmapmatcher.network;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.HGV_ACCESSIBLE_KEY;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TEST_PROFILES;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestLinks;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.util.EdgeIteratorState;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLinkCarMapper;
import nu.ndw.nls.springboot.test.logging.LoggerExtension;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;

class GraphHopperNetworkServiceIT {

    private static final String TEST_NETWORK = "test_network";

    private static final Instant DATA_DATE = Instant.parse("2023-11-07T15:37:23.129Z");

    private static final Instant DATA_DATE_TRUNCATED = Instant.parse("2023-11-07T15:37:23Z");

    private Supplier<Iterator<TestLink>> iteratorSupplier;

    private final GeometryFactoryWgs84 geometryFactoryWgs84 = new GeometryFactoryWgs84();

    @RegisterExtension
    LoggerExtension loggerExtension = new LoggerExtension();

    @SneakyThrows
    @BeforeEach
    void setUp() {
        List<TestLink> links = getTestLinks("/test-data/network.geojson");
        iteratorSupplier = links::iterator;
    }

    @SneakyThrows
    @Test
    void loadFromDisk() {

        Path graphHopperLocation = Path.of(TEST_NETWORK);

        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(Files.createDirectories(graphHopperLocation))
                .linkSupplier(iteratorSupplier)
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();

        Files.createDirectories(graphHopperLocation);
        GraphHopperNetworkService graphHopperNetworkService = getNewGraphHopperNetworkService();
        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);

        assertThat(graphHopperLocation.resolve(TEST_NETWORK)).exists();
        assertThat(graphHopperLocation.resolve(TEST_NETWORK).resolve("properties")).exists();

        graphHopperNetworkService = getNewGraphHopperNetworkService();
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);

        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);

        verifyCustomEncodedValue(networkGraphHopper, 3666256L, true);

        HashMap<Long, Integer> expectedWayIdToEdgeKey = new HashMap<>();
        HashMap<Long, Integer> expectedWayIdToReverseEdgeKey = new HashMap<>();

        AllEdgesIterator edgeIterator = networkGraphHopper.getBaseGraph().getAllEdges();
        IntEncodedValue wayIdEncodedValue = networkGraphHopper.getEncodingManager().getIntEncodedValue(Link.WAY_ID_KEY);
        while (edgeIterator.next()) {
            long wayId = edgeIterator.get(wayIdEncodedValue);
            expectedWayIdToEdgeKey.put(wayId, edgeIterator.getEdgeKey());
            expectedWayIdToReverseEdgeKey.put(wayId, edgeIterator.getReverseEdgeKey());
        }

        assertThat(networkGraphHopper.getWayIdToEdgeKey()).isEqualTo(expectedWayIdToEdgeKey);
        assertThat(networkGraphHopper.getWayIdToReverseEdgeKey()).isEqualTo(expectedWayIdToReverseEdgeKey);

        assertThat(loggerExtension.getLogEvents().stream()
                .filter(event -> event.getLevel() == Level.DEBUG)
                .filter(event -> event.getMessage().startsWith("Build wayId to edgeKey maps in "))
                .count())
                .withFailMessage("Expected debug log message with edge map build time but was not found.")
                .isEqualTo(2);
    }

    private GraphHopperNetworkService getNewGraphHopperNetworkService() {
        return TestNetworkProvider.getNetworkService(List.of(
                new TestLinkCarMapper("car"),
                new TestLinkCarMapper("car_no_u_turns")));
    }

    @SneakyThrows
    @Test
    void loadFromDisk_retrieveExtraEncodedValue() {

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
                                .geometry(geometryFactoryWgs84.createLineString(new Coordinate[]{
                                        new Coordinate(1, 2),
                                        new Coordinate(3, 4)}))
                                .fromNodeId(1)
                                .toNodeId(2)
                                .build(),
                        TestLink.builder()
                                .id(2)
                                .hgvAccessible(true)
                                .distanceInMeters(1)
                                .speedInKilometersPerHour(1)
                                .geometry(geometryFactoryWgs84.createLineString(new Coordinate[]{
                                        new Coordinate(5, 6),
                                        new Coordinate(7, 8)}))
                                .fromNodeId(3)
                                .toNodeId(4)
                                .build()).iterator())
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();

        GraphHopperNetworkService graphHopperNetworkService = getNewGraphHopperNetworkService();
        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);

        verifyCustomEncodedValue(networkGraphHopper, 1L, false);
        verifyCustomEncodedValue(networkGraphHopper, 2L, true);
    }

    private void verifyCustomEncodedValue(
            NetworkGraphHopper networkGraphHopper,
            Long roadSectionId,
            boolean expectedValue) {
        Integer edgeKey = networkGraphHopper.getWayIdToEdgeKey().get(roadSectionId);
        EdgeIteratorState edge = networkGraphHopper.getBaseGraph().getEdgeIteratorStateForKey(edgeKey);
        BooleanEncodedValue encodedValue = networkGraphHopper.getEncodingManager()
                .getBooleanEncodedValue(HGV_ACCESSIBLE_KEY);

        assertThat(edge.get(encodedValue)).isEqualTo(expectedValue);
    }

    @SneakyThrows
    @Test
    void storeOnDisk() {
        Path graphHopperLocation = Path.of(TEST_NETWORK);

        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(Files.createDirectories(graphHopperLocation))
                .linkSupplier(iteratorSupplier)
                .dataDate(DATA_DATE)
                .build();
        Files.createDirectories(graphHopperLocation);

        GraphHopperNetworkService graphHopperNetworkService = getNewGraphHopperNetworkService();
        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        assertThat(graphHopperLocation.resolve(TEST_NETWORK)).exists();
        assertThat(graphHopperLocation.resolve(TEST_NETWORK).resolve("properties")).exists();
    }

    @SneakyThrows
    @AfterEach
    void removeDirectory() {
        Path path = Path.of(TEST_NETWORK);
        FileUtils.deleteDirectory(new File(path.toUri()));
    }
}
