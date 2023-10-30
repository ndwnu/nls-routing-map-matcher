package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomAccessParserTest {

    private static final int EDGE_ID = 1;
    private static final VehicleType VEHICLE_TYPE = VehicleType.CAR;

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private PMap properties;
    @Mock
    private BooleanEncodedValue booleanEncodedValue;
    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;

    private CustomAccessParser customAccessParser;

    @BeforeEach
    void setup() {
        when(properties.getString("name", VEHICLE_TYPE.getName())).thenReturn(VEHICLE_TYPE.getName());
        when(lookup.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_TYPE.getName()))).thenReturn(booleanEncodedValue);
        this.customAccessParser = new CustomAccessParser(lookup, properties, VEHICLE_TYPE);
    }

    @Test
    void handleWayTags_ok_booleanInaccessible() {
        customAccessParser.handleWayTags(EDGE_ID, egdeIntAccess, link);
        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, false);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, false);
    }

    @Test
    void handleWayTags_ok_speedLimitZero() {
        when(link.getSpeedInKilometersPerHour()).thenReturn(0.0);
        when(link.getReverseSpeedInKilometersPerHour()).thenReturn(0.0);
        customAccessParser.handleWayTags(EDGE_ID, egdeIntAccess, link);
        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, false);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, false);
    }

    @Test
    void handleWayTags_ok_accessible() {
        when(link.getSpeedInKilometersPerHour()).thenReturn(10.0);
        when(link.getReverseSpeedInKilometersPerHour()).thenReturn(10.0);
        customAccessParser.handleWayTags(EDGE_ID, egdeIntAccess, link);
        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, true);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, true);
    }
}
