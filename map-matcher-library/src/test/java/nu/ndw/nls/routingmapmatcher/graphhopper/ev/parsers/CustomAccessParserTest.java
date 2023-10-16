package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.storage.IntsRef;
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

    private static final VehicleType VEHICLE_TYPE = VehicleType.HGV;

    @Mock
    EncodedValueLookup lookup;
    @Mock
    PMap properties;
    @Mock
    BooleanEncodedValue booleanEncodedValue;
    @Mock
    IntsRef edgeFlags;
    @Mock
    Link link;

    private CustomAccessParser customAccessParser;

    @BeforeEach
    void setup() {
        when(properties.getString("name", VEHICLE_TYPE.getName())).thenReturn(VEHICLE_TYPE.getName());
        when(lookup.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_TYPE.getName()))).thenReturn(booleanEncodedValue);
        this.customAccessParser = new CustomAccessParser(lookup, properties, VEHICLE_TYPE);
    }

    @Test
    void handleWayTags_ok_booleanInaccessible() {
        when(link.getTag(VEHICLE_TYPE.getAccessTag(), true, false)).thenReturn(false);
        when(link.getTag(VEHICLE_TYPE.getAccessTag(), true, true)).thenReturn(false);

        customAccessParser.handleWayTags(edgeFlags, link);

        verify(booleanEncodedValue).setBool(false, edgeFlags, false);
        verify(booleanEncodedValue).setBool(true, edgeFlags, false);
    }

    @Test
    void handleWayTags_ok_speedLimitZero() {
        when(link.getTag(VEHICLE_TYPE.getAccessTag(), true, false)).thenReturn(true);
        when(link.getTag(VEHICLE_TYPE.getAccessTag(), true, true)).thenReturn(true);
        when(link.getSpeedInKilometersPerHour()).thenReturn(0.0);
        when(link.getReverseSpeedInKilometersPerHour()).thenReturn(0.0);

        customAccessParser.handleWayTags(edgeFlags, link);

        verify(booleanEncodedValue).setBool(false, edgeFlags, false);
        verify(booleanEncodedValue).setBool(true, edgeFlags, false);
    }

    @Test
    void handleWayTags_ok_accessible() {
        when(link.getTag(VEHICLE_TYPE.getAccessTag(), true, false)).thenReturn(true);
        when(link.getTag(VEHICLE_TYPE.getAccessTag(), true, true)).thenReturn(true);
        when(link.getSpeedInKilometersPerHour()).thenReturn(10.0);
        when(link.getReverseSpeedInKilometersPerHour()).thenReturn(10.0);

        customAccessParser.handleWayTags(edgeFlags, link);

        verify(booleanEncodedValue).setBool(false, edgeFlags, true);
        verify(booleanEncodedValue).setBool(true, edgeFlags, true);
    }

}