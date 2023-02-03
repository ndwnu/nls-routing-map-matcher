package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static org.junit.jupiter.api.Assertions.*;

import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BearingCalculatorTest {

    @Mock
    private GeodeticCalculator geodeticCalculator;
    @InjectMocks
    private BearingCalculator bearingCalculator;

    @Test
    void bearingIsInRange_with_no_range_ok() {
        //double convertedBearing, Double inputMinBearing, Double inputMaxBearing
        assertTrue(bearingCalculator.bearingIsInRange(10.D,
                10D,
                20D));

    }
    @Test
    void bearingIsInRange_with_10_20_range_ok() {
        //double convertedBearing, Double inputMinBearing, Double inputMaxBearing
        assertTrue(bearingCalculator.bearingIsInRange(10.D,
                10D,
                20D));

    }

    @Test
    void bearingIsInRange_with_350_10_range_ok() {
        assertTrue(bearingCalculator.bearingIsInRange(10.D,350D,10D));
    }

    @Test
    void calculateBearing_with_azimuth_minus_40_should_return_320() {

    }

    @Test
    void calculateBearing_with_azimuth_40_should_return_40() {
    }
}
