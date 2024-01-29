package nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLinkCarMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkAverageSpeedParserTest {

    private static final int EDGE_ID = 1;
    private static final int SPEED = 50;
    private static final int REVERSE_SPEED = 30;
    private static final double SMALLEST_NON_ZERO_VALUE = 5.0;
    private static final double MAX_STORABLE_DECIMAL = 155.0;

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private DecimalEncodedValue averageSpeedEncoder;
    @Mock
    private EdgeIntAccess edgeIntAccess;

    private LinkAverageSpeedParser<TestLink> linkAverageSpeedParser;

    @BeforeEach
    void setUp() {
        when(lookup.getDecimalEncodedValue("car_average_speed")).thenReturn(averageSpeedEncoder);
        linkAverageSpeedParser = new LinkAverageSpeedParser<>(lookup, "car", new TestLinkCarMapper());
    }

    @Test
    void handleWayTags_ok_bidirectionalAccess() {
        mockMinMaxSpeeds();
        Link link = createLink(SPEED, REVERSE_SPEED);
        linkAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, SPEED);
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, REVERSE_SPEED);
    }

    @Test
    void handleWayTags_ok_forwardAccessOnly() {
        mockMinMaxSpeeds();
        Link link = createLink(0, REVERSE_SPEED);
        linkAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder, never()).setDecimal(eq(false), eq(EDGE_ID), eq(edgeIntAccess), anyDouble());
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, REVERSE_SPEED);
    }

    @Test
    void handleWayTags_ok_backwardAccessOnly() {
        mockMinMaxSpeeds();
        Link link = createLink(SPEED, 0);
        linkAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, SPEED);
        verify(averageSpeedEncoder, never()).setDecimal(eq(true), eq(EDGE_ID), eq(edgeIntAccess), anyDouble());
    }

    @Test
    void handleWayTags_ok_noAccess() {
        Link link = createLink(0, 0);
        linkAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder, never()).setDecimal(anyBoolean(), eq(EDGE_ID), eq(edgeIntAccess), anyDouble());
    }

    @Test
    void handleWayTags_ok_belowMinimumSpeed() {
        mockMinMaxSpeeds();
        Link link = createLink(1, 4);
        linkAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, SMALLEST_NON_ZERO_VALUE);
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, SMALLEST_NON_ZERO_VALUE);
    }

    @Test
    void handleWayTags_ok_aboveMinimumSpeed() {
        mockMinMaxSpeeds();
        Link link = createLink(156, 169);
        linkAverageSpeedParser.handleWayTags(EDGE_ID, edgeIntAccess, link);
        verify(averageSpeedEncoder).setDecimal(false, EDGE_ID, edgeIntAccess, MAX_STORABLE_DECIMAL);
        verify(averageSpeedEncoder).setDecimal(true, EDGE_ID, edgeIntAccess, MAX_STORABLE_DECIMAL);
    }

    private void mockMinMaxSpeeds() {
        when(averageSpeedEncoder.getSmallestNonZeroValue()).thenReturn(SMALLEST_NON_ZERO_VALUE);
        when(averageSpeedEncoder.getMaxStorableDecimal()).thenReturn(MAX_STORABLE_DECIMAL);
    }

    private Link createLink(int speed, int reverseSpeed) {
        return TestLink.builder().speedInKilometersPerHour(speed).reverseSpeedInKilometersPerHour(reverseSpeed).build();
    }
}
