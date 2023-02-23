package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static org.assertj.core.api.Assertions.assertThat;

import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import org.assertj.core.data.Percentage;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FractionAndDistanceCalculatorTest {

    public static final Coordinate FROM = new Coordinate(5.42670371, 52.17673587);
    public static final Coordinate TO = new Coordinate(5.42672895, 52.17670980);


    private LineString lineString;

    private Coordinate snappedPointCoordinate;
    @InjectMocks
    private FractionAndDistanceCalculator fractionAndDistanceCalculator;

    @BeforeEach
    void setup() {
        fractionAndDistanceCalculator = new FractionAndDistanceCalculator(new GeodeticCalculator());
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        Coordinate[] coordinates = new Coordinate[]{FROM, TO};
        lineString = geometryFactory.createLineString(coordinates);
        snappedPointCoordinate = new Coordinate(5.426716016, 52.176722770);
    }

    @Test
    void calculateFraction_ok() {
        double fraction = fractionAndDistanceCalculator.calculateFraction(lineString, snappedPointCoordinate);
        assertThat(fraction).isEqualTo(0.4986298177545211);


    }

    @Test
    void calculateDistance() {
        double distance = fractionAndDistanceCalculator.calculateDistance(FROM, TO);
        assertThat(distance).isCloseTo(3.4, Percentage.withPercentage(1));
    }
}
