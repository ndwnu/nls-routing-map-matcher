package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.util.DistancePlaneProjection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeometryConstants {

    public static final DistancePlaneProjection DIST_PLANE = DistancePlaneProjection.DIST_PLANE;

    private GeometryConstants() {
    }

    private static GeometryFactory createGeometryFactory(int srid) {
        return new GeometryFactory(new PrecisionModel(), srid);
    }
}
