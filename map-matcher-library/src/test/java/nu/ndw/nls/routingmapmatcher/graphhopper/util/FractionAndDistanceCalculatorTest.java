package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static org.assertj.core.api.Assertions.assertThat;

import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import org.assertj.core.data.Percentage;
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
    void calculateFraction_ok() {
        double fraction = FractionAndDistanceCalculator.calculateFractionAndDistance(lineString, snappedPointCoordinate)
                .getFraction();
        assertThat(fraction).isCloseTo(0.4986, Percentage.withPercentage(1));
    }
}
