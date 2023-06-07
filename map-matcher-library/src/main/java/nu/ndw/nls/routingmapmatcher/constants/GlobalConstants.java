package nu.ndw.nls.routingmapmatcher.constants;

import com.graphhopper.util.DistancePlaneProjection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public final class GlobalConstants {

    public static final int WGS84_SRID = 4326;
    public static final GeometryFactory WGS84_GEOMETRY_FACTORY = createGeometryFactory(WGS84_SRID);
    public static final int RD_NEW_SRID = 28992;
    public static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = createGeometryFactory(RD_NEW_SRID);
    public static final CoordinateReferenceSystem WGS84_GEOGRAPHIC_CRS = DefaultGeographicCRS.WGS84;
    public static final DistancePlaneProjection DIST_PLANE = DistancePlaneProjection.DIST_PLANE;
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
