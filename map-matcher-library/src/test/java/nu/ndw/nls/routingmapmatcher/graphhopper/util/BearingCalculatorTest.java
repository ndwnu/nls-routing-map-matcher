package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BearingCalculatorTest {

    private static final double MIN_BEARING_10 = 10D;
    private static final double MAX_BEARING_20 = 20D;
    private static final double MIN_BEARING_350 = 350D;
    private static final double CONVERTED_BEARING_IN_RANGE = 10D;


    @Mock
    private GeodeticCalculator geodeticCalculator;
    @InjectMocks
    private BearingCalculator bearingCalculator;

    @Test
    void bearingIsInRange_with_no_range_should_return_true() {
        assertTrue(bearingCalculator.bearingIsInRange(CONVERTED_BEARING_IN_RANGE,
                null));

    }

    @Test
    void bearingIsInRange_with_10_20_range_should_return_true() {
        assertTrue(bearingCalculator.bearingIsInRange(CONVERTED_BEARING_IN_RANGE,
                new BearingRange(MIN_BEARING_10,
                        MAX_BEARING_20)));

    }

    @Test
    void bearingIsInRange_with_350_10_range_should_return_true() {
        assertTrue(bearingCalculator.bearingIsInRange(CONVERTED_BEARING_IN_RANGE,
                new BearingRange(MIN_BEARING_350, MIN_BEARING_10)));
    }

    @Test
    void bearingIsInRange_with_10_20_range_should_return_false() {
        assertFalse(bearingCalculator.bearingIsInRange(9D,
                new BearingRange(MIN_BEARING_10,
                        MAX_BEARING_20)));

    }
    @Test
    void bearingIsInRange_with_350_10_range_should_return_false() {
        assertFalse(bearingCalculator.bearingIsInRange(10.1,
                new BearingRange(MIN_BEARING_350, MIN_BEARING_10)));
    }


    @Test
    void calculateBearing_with_azimuth_minus_40_should_return_320() {


    }

    @Test
    void calculateBearing_with_azimuth_40_should_return_40() {
    }
}
