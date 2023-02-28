package nu.ndw.nls.routingmapmatcher.util;

import java.io.IOException;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

public class GeometryHelper {

    private final WKBReader wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID));

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

    public synchronized Point convertToPoint(byte[] geometryWkb) throws IOException {
        try {
            Geometry geometry = this.wkbReader.read(geometryWkb);
            if (!(geometry instanceof Point point)) {
                throw new IOException("Unexpected geometry type: expected Point");
            }
            return point;
        } catch (ParseException e) {
            throw new IOException("Unable to parse WKB", e);
        }
    }
}
