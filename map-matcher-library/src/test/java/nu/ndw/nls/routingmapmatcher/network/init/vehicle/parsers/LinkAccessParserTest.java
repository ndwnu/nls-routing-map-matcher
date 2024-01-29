package nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLinkCarMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkAccessParserTest {

    private static final int EDGE_ID = 1;

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private BooleanEncodedValue booleanEncodedValue;
    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private TestLink link;

    private LinkAccessParser<TestLink> linkAccessParser;

    @BeforeEach
    void setup() {
        when(lookup.getBooleanEncodedValue(VehicleAccess.key("car"))).thenReturn(booleanEncodedValue);
        this.linkAccessParser = new LinkAccessParser<>(lookup, "car", new TestLinkCarMapper());
    }

    @Test
    void handleWayTags_ok_booleanInaccessible() {
        linkAccessParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);
        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, false);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, false);
    }

    @Test
    void handleWayTags_ok_speedLimitZero() {
        when(link.getSpeedInKilometersPerHour()).thenReturn(0.0);
        when(link.getReverseSpeedInKilometersPerHour()).thenReturn(0.0);
        linkAccessParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);
        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, false);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, false);
    }

    @Test
    void handleWayTags_ok_accessible() {
        when(link.getSpeedInKilometersPerHour()).thenReturn(10.0);
        when(link.getReverseSpeedInKilometersPerHour()).thenReturn(10.0);
        linkAccessParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);
        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, true);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, true);
    }
}
