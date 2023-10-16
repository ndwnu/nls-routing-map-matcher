package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoubleParserTest {

    private static final double EXPECTED_A = 0.1;
    private static final double EXPECTED_B = 0.2;
    private static final String TAG_KEY = "key";

    @Mock
    LinkTag<Double> linkTag;
    @Mock
    IntsRef edgeFlags;
    @Mock
    Link link;
    @Mock
    DecimalEncodedValue decimalEncodedValue;
    @Mock
    EncodedTag encodedTag;
    @Mock
    EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        DoubleParser doubleParser = getDoubleParser(false);
        when(link.getTag(linkTag, Double.POSITIVE_INFINITY)).thenReturn(EXPECTED_A);

        doubleParser.handleWayTags(edgeFlags, link, null);

        verify(decimalEncodedValue).setDecimal(false, edgeFlags, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        DoubleParser doubleParser = getDoubleParser(true);
        when(link.getTag(linkTag, Double.POSITIVE_INFINITY, false)).thenReturn(EXPECTED_A);
        when(link.getTag(linkTag, Double.POSITIVE_INFINITY, true)).thenReturn(EXPECTED_B);

        doubleParser.handleWayTags(edgeFlags, link, null);

        verify(decimalEncodedValue).setDecimal(false, edgeFlags, EXPECTED_A);
        verify(decimalEncodedValue).setDecimal(true, edgeFlags, EXPECTED_B);
    }

    private DoubleParser getDoubleParser(boolean separateValuesPerDirection) {
        doReturn(linkTag).when(encodedTag).getLinkTag();
        when(encodedTag.isSeparateValuesPerDirection()).thenReturn(separateValuesPerDirection);
        when(encodedTag.getKey()).thenReturn(TAG_KEY);
        when(encodedValueLookup.getDecimalEncodedValue(TAG_KEY)).thenReturn(decimalEncodedValue);

        return new DoubleParser(encodedValueLookup, encodedTag);
    }

}