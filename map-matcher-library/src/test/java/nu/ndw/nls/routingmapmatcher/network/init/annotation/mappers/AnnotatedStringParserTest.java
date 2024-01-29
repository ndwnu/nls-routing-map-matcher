package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.StringEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedStringParserTest {

    private static final int EDGE_ID = 1;
    private static final String EXPECTED_A = "A";
    private static final String EXPECTED_B = "B";
    private static final String TAG_KEY = "key";

    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private StringEncodedValue stringEncodedValue;
    @Mock
    private EncodedValueDto<Link, String> encodedTag;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        when(encodedTag.isDirectional()).thenReturn(false);
        when(encodedTag.key()).thenReturn(TAG_KEY);
        when(encodedTag.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueLookup.getStringEncodedValue(TAG_KEY)).thenReturn(stringEncodedValue);

        EncodedStringValueMapper<Link> stringParser = new EncodedStringValueMapper<>(encodedValueLookup, encodedTag);

        stringParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(stringEncodedValue).setString(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        when(encodedTag.isDirectional()).thenReturn(true);
        when(encodedTag.key()).thenReturn(TAG_KEY);
        when(encodedTag.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedTag.valueReverseSupplier()).thenReturn(link -> EXPECTED_B);
        when(encodedValueLookup.getStringEncodedValue(TAG_KEY)).thenReturn(stringEncodedValue);

        EncodedStringValueMapper<Link> stringParser = new EncodedStringValueMapper<>(encodedValueLookup, encodedTag);

        stringParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(stringEncodedValue).setString(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(stringEncodedValue).setString(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

}
