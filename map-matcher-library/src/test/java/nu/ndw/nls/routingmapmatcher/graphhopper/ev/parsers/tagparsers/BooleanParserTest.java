package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
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
class BooleanParserTest {

    private static final int EDGE_ID = 1;
    private static final boolean EXPECTED_A = true;
    private static final boolean EXPECTED_B = false;
    private static final String TAG_KEY = "key";

    @Mock
    private LinkTag<Boolean> linkTag;
    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private BooleanEncodedValue booleanEncodedValue;
    @Mock
    private EncodedTag encodedTag;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        BooleanParser booleanParser = getBooleanParser(false);
        when(link.getTag(linkTag, true)).thenReturn(EXPECTED_A);

        booleanParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        BooleanParser booleanParser = getBooleanParser(true);
        when(link.getTag(linkTag, true, false)).thenReturn(EXPECTED_A);
        when(link.getTag(linkTag, true, true)).thenReturn(EXPECTED_B);

        booleanParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(booleanEncodedValue).setBool(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(booleanEncodedValue).setBool(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

    private BooleanParser getBooleanParser(boolean separateValuesPerDirection) {
        doReturn(linkTag).when(encodedTag).getLinkTag();
        when(encodedTag.isSeparateValuesPerDirection()).thenReturn(separateValuesPerDirection);
        when(encodedTag.getKey()).thenReturn(TAG_KEY);
        when(encodedValueLookup.getBooleanEncodedValue(TAG_KEY)).thenReturn(booleanEncodedValue);

        return new BooleanParser(encodedValueLookup, encodedTag);
    }
}
