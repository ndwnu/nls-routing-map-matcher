package nu.ndw.nls.routingmapmatcher.constants;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class GlobalConstants {

    public static final int WGS84_SRID = 4326;
    public static final GeometryFactory WGS84_GEOMETRY_FACTORY = createGeometryFactory(WGS84_SRID);
    public static final int RD_NEW_SRID = 28992;
    public static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = createGeometryFactory(RD_NEW_SRID);
    public static final String CAR_FASTEST = "car_fastest";
    public static final String CAR_SHORTEST = "car_shortest";

    public static final String VEHICLE_CAR = "car";
    public static final String WEIGHTING_FASTEST = "fastest";
    public static final String WEIGHTING_SHORTEST = "shortest";

    private GlobalConstants() {
    }

    private static GeometryFactory createGeometryFactory(int srid) {
        return new GeometryFactory(new PrecisionModel(), srid);
    }
}
