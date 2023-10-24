package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoubleParserTest {

    private static final int EDGE_ID = 1;
    private static final double EXPECTED_A = 0.1;
    private static final double EXPECTED_B = 0.2;
    private static final String TAG_KEY = "key";

    @Mock
    private LinkTag<Double> linkTag;
    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private DecimalEncodedValue decimalEncodedValue;
    @Mock
    private EncodedTag encodedTag;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        DoubleParser doubleParser = getDoubleParser(false);
        when(link.getTag(linkTag, Double.POSITIVE_INFINITY)).thenReturn(EXPECTED_A);

        doubleParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(decimalEncodedValue).setDecimal(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        DoubleParser doubleParser = getDoubleParser(true);
        when(link.getTag(linkTag, Double.POSITIVE_INFINITY, false)).thenReturn(EXPECTED_A);
        when(link.getTag(linkTag, Double.POSITIVE_INFINITY, true)).thenReturn(EXPECTED_B);

        doubleParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(decimalEncodedValue).setDecimal(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(decimalEncodedValue).setDecimal(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

    private DoubleParser getDoubleParser(boolean separateValuesPerDirection) {
        doReturn(linkTag).when(encodedTag).getLinkTag();
        when(encodedTag.isSeparateValuesPerDirection()).thenReturn(separateValuesPerDirection);
        when(encodedTag.getKey()).thenReturn(TAG_KEY);
        when(encodedValueLookup.getDecimalEncodedValue(TAG_KEY)).thenReturn(decimalEncodedValue);

        return new DoubleParser(encodedValueLookup, encodedTag);
    }
}
