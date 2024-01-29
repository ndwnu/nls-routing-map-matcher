package nu.ndw.nls.routingmapmatcher.network.init.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.VehicleTagParsers;
import com.graphhopper.util.PMap;
import java.util.Map;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers.LinkAccessParser;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers.LinkAverageSpeedParser;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkVehicleParserFactoryTest {

    private static final String NAME = "vehicle_name";

    @Mock
    private Map<String, LinkVehicleMapper<Link>> linkVehicleMapperMap;
    @Mock
    private LinkVehicleMapper<Link> linkVehicleMapper;
    @Mock
    private EncodedValueLookup encodedValueLookup;
    @Mock
    private BooleanEncodedValue encodedAccess;
    @Mock
    private DecimalEncodedValue encodedSpeed;
    @Mock
    PMap properties;

    @InjectMocks
    private LinkVehicleParserFactory<Link> linkVehicleParserFactory;

    @Test
    void createParsers_ok() {
        when(linkVehicleMapperMap.get(NAME)).thenReturn(linkVehicleMapper);
        when(encodedValueLookup.getBooleanEncodedValue("vehicle_name_access")).thenReturn(encodedAccess);
        when(encodedValueLookup.getDecimalEncodedValue("vehicle_name_average_speed")).thenReturn(encodedSpeed);

        VehicleTagParsers result = linkVehicleParserFactory.createParsers(encodedValueLookup, NAME, properties);

        LinkAccessParser<Link> accessParser = assertInstanceOf(LinkAccessParser.class, result.getAccessParser());
        assertEquals(linkVehicleMapper, accessParser.getLinkVehicleMapper());
        assertEquals(encodedAccess, accessParser.getAccessEnc());

        LinkAverageSpeedParser<Link> averageSpeedParser = assertInstanceOf(LinkAverageSpeedParser.class,
                result.getSpeedParser());
        assertEquals(linkVehicleMapper, averageSpeedParser.getLinkVehicleMapper());
        assertEquals(encodedSpeed, averageSpeedParser.getAverageSpeedEnc());
    }

}