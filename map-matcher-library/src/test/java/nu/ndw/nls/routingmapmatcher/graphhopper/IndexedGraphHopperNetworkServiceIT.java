package nu.ndw.nls.routingmapmatcher.graphhopper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LineStringLocationDeserializer;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexedGraphHopperNetworkServiceIT {

    public static final String TEST_NETWORK = "test_network";
    private IndexedGraphHopperNetworkService indexedGraphHopperNetworkService;
    private Supplier<Iterator<Link>> iteratorSupplier;

    @SneakyThrows
    @BeforeEach
    void setup() {
        String linksJson = IOUtils.toString(getClass().getResourceAsStream("/test-data/links.json"));
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        module.addDeserializer(LineStringLocation.class, new LineStringLocationDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });
        iteratorSupplier = links::iterator;
        indexedGraphHopperNetworkService = new IndexedGraphHopperNetworkService();

    }

    @SneakyThrows
    @Test
    void loadFromDisk_ok() {
        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion(TEST_NETWORK)
                .linkSupplier(iteratorSupplier).build();
        final Path path = Path.of(TEST_NETWORK);
        Files.createDirectories(path);
        indexedGraphHopperNetworkService.storeOnDisk(routingNetwork, path);
        IndexedNetworkGraphHopper indexedNetworkGraphHopper = indexedGraphHopperNetworkService.loadFromDisk(
                routingNetwork, path);
        assertThat(indexedNetworkGraphHopper).isNotNull();
    }

    @SneakyThrows
    @Test
    void storeOnDisk_ok() {
        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion(TEST_NETWORK)
                .linkSupplier(iteratorSupplier).build();
        final Path path = Path.of(TEST_NETWORK);
        Files.createDirectories(path);
        indexedGraphHopperNetworkService.storeOnDisk(routingNetwork, path);
        assertThat(path.resolve(TEST_NETWORK)).exists();
        assertThat(path.resolve(TEST_NETWORK).resolve("properties")).exists();
        assertThat(path.resolve(TEST_NETWORK).resolve("edgemap")).exists();
    }

    @SneakyThrows
    @AfterEach
    void removeDirectory() {
        final Path path = Path.of(TEST_NETWORK);
        FileUtils.deleteDirectory(new File(path.toUri()));
    }

}