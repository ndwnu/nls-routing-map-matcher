package nu.ndw.nls.routingmapmatcher.util;

import static nu.ndw.nls.routingmapmatcher.util.GeometryConstants.WGS84_GEOMETRY_FACTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import nu.ndw.nls.routingmapmatcher.model.FractionAndDistance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FractionAndDistanceCalculatorTest {

    private static final Coordinate FROM = new Coordinate(5.42670371, 52.17673587);
    private static final Coordinate TO = new Coordinate(5.42672895, 52.17670980);
    private static final LineString LINE_STRING = WGS84_GEOMETRY_FACTORY.createLineString(new Coordinate[]{FROM, TO});
    private static final double DELTA = 0.00005;

    @Test
    void calculateFractionAndDistance_ok() {
        FractionAndDistance fractionAndDistance = FractionAndDistanceCalculator.calculateFractionAndDistance(
                LINE_STRING, new Coordinate(5.426716016, 52.17672277));
        assertEquals(0.4953, fractionAndDistance.getFraction(), DELTA);
        assertEquals(1.6719, fractionAndDistance.getFractionDistance(), DELTA);
        assertEquals(3.3758, fractionAndDistance.getTotalDistance(), DELTA);
    }

    @Test
    void calculateFractionAndDistance_ok_beforeStartPointMinimum0() {
        FractionAndDistance fractionAndDistance = FractionAndDistanceCalculator.calculateFractionAndDistance(
                LINE_STRING, new Coordinate(5.426702865, 52.176737201));
        assertEquals(0.0, fractionAndDistance.getFraction(), DELTA);
        assertEquals(0.0, fractionAndDistance.getFractionDistance(), DELTA);
        assertEquals(3.3758, fractionAndDistance.getTotalDistance(), DELTA);
    }

    @Test
    void calculateFractionAndDistance_ok_afterEndPointMaximum1() {
        FractionAndDistance fractionAndDistance = FractionAndDistanceCalculator.calculateFractionAndDistance(
                LINE_STRING, new Coordinate(5.426729913, 52.17670903));
        assertEquals(1.0, fractionAndDistance.getFraction(), DELTA);
        assertEquals(3.3758, fractionAndDistance.getFractionDistance(), DELTA);
        assertEquals(3.3758, fractionAndDistance.getTotalDistance(), DELTA);
    }

    @Test
    void calculateLengthInMeters_ok() {
        double lengthInMeters = FractionAndDistanceCalculator.calculateLengthInMeters(LINE_STRING);
        assertEquals(3.3758, lengthInMeters, DELTA);
    }
}
