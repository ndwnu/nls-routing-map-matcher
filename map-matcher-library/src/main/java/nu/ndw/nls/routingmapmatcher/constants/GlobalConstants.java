package nu.ndw.nls.routingmapmatcher.constants;

import com.graphhopper.util.DistancePlaneProjection;
import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class GlobalConstants {

    public static final int WGS84_SRID = 4326;
    public static final GeometryFactory WGS84_GEOMETRY_FACTORY = createGeometryFactory(WGS84_SRID);
    public static final int RD_NEW_SRID = 28992;
    public static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = createGeometryFactory(RD_NEW_SRID);
    public static final CoordinateReferenceSystem WGS84_GEOGRAPHIC_CRS = DefaultGeographicCRS.WGS84;
    public static final DistancePlaneProjection DIST_PLANE = DistancePlaneProjection.DIST_PLANE;

    public static final String VEHICLE_CAR = VehicleType.CAR.getName();
    public static final Path DEFAULT_FOLDER_PREFIX = Path.of("graphhopper_");

    private GlobalConstants() {
    }

    private static GeometryFactory createGeometryFactory(int srid) {
        return new GeometryFactory(new PrecisionModel(), srid);
    }
}
