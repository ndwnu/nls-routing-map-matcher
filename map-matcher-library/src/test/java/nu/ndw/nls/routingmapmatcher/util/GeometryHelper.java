package nu.ndw.nls.routingmapmatcher.util;

import java.io.IOException;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

public class GeometryHelper {

    private final WKBReader wkbReader = new WKBReader(new GeometryFactoryWgs84());

    public synchronized LineString convertToLinestring(byte[] geometryWkb) throws IOException {
        try {
            Geometry geometry = wkbReader.read(geometryWkb);
            if (!(geometry instanceof LineString lineString)) {
                throw new IOException("Unexpected geometry type: expected LineString");
            }
            return lineString;
        } catch (ParseException e) {
            throw new IOException("Unable to parse WKB", e);
        }
    }

}
