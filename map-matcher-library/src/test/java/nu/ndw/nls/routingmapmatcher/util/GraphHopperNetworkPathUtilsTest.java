package nu.ndw.nls.routingmapmatcher.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GraphHopperNetworkPathUtilsTest {

    public static final Path GRAPHHOPPER_ROOT_PATH = Path.of("/root/path");
    public static final Path GRAPHHOPPER_UNNORMALIZED_ROOT_PATH = Path.of("/root/path/lalala/../");

    @Test
    void formatNetworkNameAndVersion() {
        assertEquals("name123",
                GraphHopperNetworkPathUtils.formatNetworkNameAndVersion("name", "123"));
    }

    @Test
    void formatNormalizedPath_rootPath_prefix_version() {
        assertEquals(GRAPHHOPPER_ROOT_PATH.resolve("lalala_12345"),
                GraphHopperNetworkPathUtils.formatNormalizedPath(GRAPHHOPPER_ROOT_PATH,
                "lalala_", "12345"));

        assertEquals(GRAPHHOPPER_ROOT_PATH.resolve("_inbetween_987865"),
                GraphHopperNetworkPathUtils.formatNormalizedPath(GRAPHHOPPER_ROOT_PATH,
                "_inbetween_", "987865"));


        assertEquals(GRAPHHOPPER_ROOT_PATH.resolve("_inbetween_987865"),
                GraphHopperNetworkPathUtils.formatNormalizedPath(GRAPHHOPPER_UNNORMALIZED_ROOT_PATH,
                        "_inbetween_", "987865"));
    }

    @Test
    void formatNormalizedPath_rootPath_networkNameAndVersion() {
        assertEquals(GRAPHHOPPER_ROOT_PATH.resolve("networkAndVersion"),
                GraphHopperNetworkPathUtils.formatNormalizedPath(GRAPHHOPPER_ROOT_PATH,
                        "networkAndVersion"));

        assertEquals(GRAPHHOPPER_ROOT_PATH.resolve("networkAndVersion"),
                GraphHopperNetworkPathUtils.formatNormalizedPath(GRAPHHOPPER_UNNORMALIZED_ROOT_PATH,
                        "networkAndVersion"));}

}
