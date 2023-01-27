package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryResultWithBearingTest {

    private QueryResultWithBearing queryResultWithBearing;
    @BeforeEach
    void setup(){
        queryResultWithBearing =  QueryResultWithBearing
                .builder()
                .inputMinBearing(340.0)
                .inputMaxBearing(20.0)
                .build();
    }

    @Test
    void bearingIsInRange_ok() {
        assertThat(queryResultWithBearing.bearingIsInRange(350.0))
                .isTrue();
        assertThat(queryResultWithBearing.bearingIsInRange(10))
                .isTrue();

        assertThat(queryResultWithBearing.bearingIsInRange(360))
                .isTrue();

        assertThat(queryResultWithBearing.bearingIsInRange(0))
                .isTrue();

    }
}
