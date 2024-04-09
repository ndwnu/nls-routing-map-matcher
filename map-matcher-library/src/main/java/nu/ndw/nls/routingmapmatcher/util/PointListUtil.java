package nu.ndw.nls.routingmapmatcher.util;


import com.graphhopper.util.PointList;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

public class PointListUtil {

    private static final int MINIMUM_LINESTRING_SIZE = 2;
    private static final int LINESTRING_DIMENSIONS = 2;
    private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactoryWgs84();

    // PointList.toLineString rounds to 6 digits. This can affect the route slightly, this method prevents that
    public LineString toLineString(PointList pointList) {
        Coordinate[] coordinates = new Coordinate[pointList.size() == 1 ? MINIMUM_LINESTRING_SIZE : pointList.size()];

        for (int i = 0; i < pointList.size(); ++i) {
            coordinates[i] = new Coordinate(pointList.getLon(i), pointList.getLat(i));
        }

        if (pointList.size() == 1) {
            coordinates[1] = coordinates[0];
        }

        return WGS84_GEOMETRY_FACTORY.createLineString(
                new PackedCoordinateSequence.Double(coordinates, LINESTRING_DIMENSIONS));
    }
}
