package nu.ndw.nls.routingmapmatcher.util;

import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

import java.io.IOException;

public class GeometryHelper {

    /*
        Warning This class is not thread-safe; each thread should create its own instance.
        https://locationtech.github.io/jts/javadoc/org/locationtech/jts/io/WKBReader.html
     */
    private final WKBReader wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID));

    public LineString convertToLinestring(final byte[] geometryWkb) throws IOException {
        try {
            final Geometry geometry = wkbReader.read(geometryWkb);
            if (!(geometry instanceof LineString)) {
                throw new IOException("Unexpected geometry type: expected LineString");
            }
            return (LineString) geometry;
        } catch (final ParseException e) {
            throw new IOException("Unable to parse WKB", e);
        }
    }
}
