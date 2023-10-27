package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomAverageSpeedParserTest {

    private static final int EDGE_ID = 1;
    private static final int SPEED = 50;
    private static final int REVERSE_SPEED = 30;

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private PMap properties;
    @Mock
    private DecimalEncodedValue averageSpeedEncoder;
    @Mock
    private DecimalEncodedValue ferrySpeedEncoder;
    @Mock
    private EdgeIntAccess edgeIntAccess;

    private CustomAverageSpeedParser customAverageSpeedParser;

    @BeforeEach
    void setUp() {
        when(properties.getString("name", "car")).thenReturn("car");
        when(lookup.getDecimalEncodedValue("car_average_speed")).thenReturn(averageSpeedEncoder);
        when(lookup.getDecimalEncodedValue("ferry_speed")).thenReturn(ferrySpeedEncoder);
        customAverageSpeedParser = new CustomAverageSpeedParser(lookup, properties, VehicleType.CAR);
    }

    @Test
    void handleWayTags_ok_bidirectionalAccess() {
        Link link = createLink(SPEED, REVERSE_SPEED);
        customAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, SPEED);
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, REVERSE_SPEED);
    }

    @Test
    void handleWayTags_ok_forwardAccessOnly() {
        Link link = createLink(0, REVERSE_SPEED);
        customAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder, never()).setDecimal(eq(false), eq(EDGE_ID), eq(edgeIntAccess), anyDouble());
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, REVERSE_SPEED);
    }

    @Test
    void handleWayTags_ok_backwardAccessOnly() {
        Link link = createLink(SPEED, 0);
        customAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, SPEED);
        verify(averageSpeedEncoder, never()).setDecimal(eq(true), eq(EDGE_ID), eq(edgeIntAccess), anyDouble());
    }

    @Test
    void handleWayTags_ok_noAccess() {
        Link link = createLink(0, 0);
        customAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder, never()).setDecimal(anyBoolean(), eq(EDGE_ID), eq(edgeIntAccess), anyDouble());
    }

    @Test
    void handleWayTags_ok_belowMinimumSpeed() {
        double smallestNonZeroValue = 5.0;
        when(averageSpeedEncoder.getSmallestNonZeroValue()).thenReturn(smallestNonZeroValue);
        Link link = createLink(1, 4);
        customAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, smallestNonZeroValue);
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, smallestNonZeroValue);
    }

    private Link createLink(int speed, int reverseSpeed) {
        return Link.builder().speedInKilometersPerHour(speed).reverseSpeedInKilometersPerHour(reverseSpeed).build();
    }
}
