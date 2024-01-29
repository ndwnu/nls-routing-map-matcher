package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedBooleanParserTest {

    private static final int EDGE_ID = 1;
    private static final boolean EXPECTED_A = true;
    private static final boolean EXPECTED_B = false;
    private static final String KEY = "key";

    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private BooleanEncodedValue booleanEncodedValue;
    @Mock
    private EncodedValueDto<Link, Boolean> encodedValueDto;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.isDirectional()).thenReturn(false);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueLookup.getBooleanEncodedValue(KEY)).thenReturn(booleanEncodedValue);

        EncodedBooleanMapper<Link> booleanParser = new EncodedBooleanMapper<>(encodedValueLookup, encodedValueDto);

        booleanParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.isDirectional()).thenReturn(true);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueDto.valueReverseSupplier()).thenReturn(link -> EXPECTED_B);
        when(encodedValueLookup.getBooleanEncodedValue(KEY)).thenReturn(booleanEncodedValue);

        EncodedBooleanMapper<Link> booleanParser = new EncodedBooleanMapper<>(encodedValueLookup, encodedValueDto);

        booleanParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

}
