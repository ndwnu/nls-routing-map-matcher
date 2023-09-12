package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.FractionAndDistance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FractionAndDistanceCalculatorTest {

    private static final Coordinate FROM = new Coordinate(5.42670371, 52.17673587);
    private static final Coordinate TO = new Coordinate(5.42672895, 52.17670980);
    private static final double DELTA = 0.00005;

    private LineString lineString;
    private Coordinate snappedPointCoordinate;

    @BeforeEach
    void setup() {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        Coordinate[] coordinates = new Coordinate[]{FROM, TO};
        lineString = geometryFactory.createLineString(coordinates);
        snappedPointCoordinate = new Coordinate(5.426716016, 52.176722770);
    }

    @Test
    void calculateFractionAndDistance_ok() {
        FractionAndDistance fractionAndDistance = FractionAndDistanceCalculator.calculateFractionAndDistance(lineString,
                snappedPointCoordinate);
        assertEquals(0.4986, fractionAndDistance.getFraction(), DELTA);
        assertEquals(1.6833, fractionAndDistance.getFractionDistance(), DELTA);
        assertEquals(3.3758, fractionAndDistance.getTotalDistance(), DELTA);
    }

    @Test
    void calculateLengthInMeters_ok() {
        double lengthInMeters = FractionAndDistanceCalculator.calculateLengthInMeters(lineString);
        assertEquals(3.3758, lengthInMeters, DELTA);
    }
}
