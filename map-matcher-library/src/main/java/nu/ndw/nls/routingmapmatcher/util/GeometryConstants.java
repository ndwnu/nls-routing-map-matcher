package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.util.DistancePlaneProjection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeometryConstants {

    public static final int WGS84_SRID = 4326;
    public static final GeometryFactory WGS84_GEOMETRY_FACTORY = createGeometryFactory(WGS84_SRID);
    public static final int RD_NEW_SRID = 28992;
    public static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = createGeometryFactory(RD_NEW_SRID);
    public static final DistancePlaneProjection DIST_PLANE = DistancePlaneProjection.DIST_PLANE;

    private GeometryConstants() {
    }

    private static GeometryFactory createGeometryFactory(int srid) {
        return new GeometryFactory(new PrecisionModel(), srid);
    }
}
