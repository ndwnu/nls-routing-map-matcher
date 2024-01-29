package nu.ndw.nls.routingmapmatcher.network;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TEST_PROFILES;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestLinks;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphHopperNetworkServiceIT {

    private static final String TEST_NETWORK = "test_network";

    private static final Instant DATA_DATE = Instant.parse("2023-11-07T15:37:23.129Z");
    private static final Instant DATA_DATE_TRUNCATED = Instant.parse("2023-11-07T15:37:23Z");
    private GraphHopperNetworkService graphHopperNetworkService;

    private Supplier<Iterator<TestLink>> iteratorSupplier;
    @SneakyThrows
    @BeforeEach
    void setUp() {
        List<TestLink> links = getTestLinks("/test-data/links.json");
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
                .build();
        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);
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
