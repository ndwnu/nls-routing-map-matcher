package nu.ndw.nls.routingmapmatcher.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrsTransformerTest {

    private final CrsTransformer crsTransformer = new CrsTransformer();

    private final GeometryFactory wgs84GeometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
    private final GeometryFactory rdNewGeometryFactory = GeometryConstants.RD_NEW_GEOMETRY_FACTORY;

    @Test
    void transformFromWgs84ToRdNew_ok() {
        Point pointWgs84 = wgs84GeometryFactory.createPoint(new Coordinate(5.3872036, 52.1551723));
        Point pointRd = (Point) crsTransformer.transformFromWgs84ToRdNew(pointWgs84);
        assertEquals(155000, pointRd.getX(), 0.01);
        assertEquals(463000, pointRd.getY(), 0.01);
    }

    @Test
    void transformFromWgs84ToRdNew_exception() {
        Point pointWgs84 = wgs84GeometryFactory.createPoint(new Coordinate(5.3872036, 90.1551723));
        assertThrows(IllegalStateException.class, () -> crsTransformer.transformFromWgs84ToRdNew(pointWgs84));
    }

    @Test
    void transformFromRdNewToWgs84_ok() {
        Point pointRd = rdNewGeometryFactory.createPoint(new Coordinate(155000, 463000));
        Point pointWgs84 = (Point) crsTransformer.transformFromRdNewToWgs84(pointRd);
        assertEquals(5.3872036, pointWgs84.getX(), 0.0000001);
        assertEquals(52.1551723, pointWgs84.getY(), 0.0000001);
    }

    @Test
    void transformFromRdNewToWgs84_exception() {
        Point pointRd = rdNewGeometryFactory.createPoint(new Coordinate(155001, 463001));
        assertThrows(IllegalStateException.class, () -> crsTransformer.transformFromRdNewToWgs84(pointRd));
    }
}
