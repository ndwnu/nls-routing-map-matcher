package nu.ndw.nls.routingmapmatcher.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import org.apache.commons.io.IOUtils;
import org.geotools.data.geojson.GeoJSONReader;

public final class TestLineStringProvider {

    private TestLineStringProvider() {
        // Util class
    }

    public static LineStringLocation getLineStringLocation(String path) throws IOException {
        String locationJson;
        try (InputStream resourceAsStream = TestLineStringProvider.class.getResourceAsStream(path)) {
            locationJson = IOUtils.toString(Objects.requireNonNull(resourceAsStream), StandardCharsets.UTF_8);
        }
        try (GeoJSONReader geoJSONReader = new GeoJSONReader(locationJson)) {
            return LineStringLocationDeserializer.deserialize(geoJSONReader.getFeature());
        }
    }
}
