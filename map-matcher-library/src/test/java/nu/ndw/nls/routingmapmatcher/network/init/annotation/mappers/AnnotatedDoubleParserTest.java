package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedDoubleParserTest {

    private static final int EDGE_ID = 1;
    private static final double EXPECTED_A = 0.1;
    private static final double EXPECTED_B = 0.2;
    private static final String KEY = "key";

    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private DecimalEncodedValue decimalEncodedValue;
    @Mock
    private EncodedValueDto<Link, Double> encodedValueDto;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        when(encodedValueDto.isDirectional()).thenReturn(false);
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueLookup.getDecimalEncodedValue(KEY)).thenReturn(decimalEncodedValue);

        EncodedDoubleMapper<Link> doubleParser = new EncodedDoubleMapper<>(encodedValueLookup, encodedValueDto);

        doubleParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(decimalEncodedValue).setDecimal(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        when(encodedValueDto.isDirectional()).thenReturn(true);
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueDto.valueReverseSupplier()).thenReturn(link -> EXPECTED_B);
        when(encodedValueLookup.getDecimalEncodedValue(KEY)).thenReturn(decimalEncodedValue);

        EncodedDoubleMapper<Link> doubleParser = new EncodedDoubleMapper<>(encodedValueLookup, encodedValueDto);

        doubleParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(decimalEncodedValue).setDecimal(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(decimalEncodedValue).setDecimal(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

}
