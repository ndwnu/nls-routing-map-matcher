package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.StringEncodedValue;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringParserTest {

    private static final int EDGE_ID = 1;
    private static final String EXPECTED_A = "A";
    private static final String EXPECTED_B = "B";
    private static final String TAG_KEY = "key";

    @Mock
    private LinkTag<String> linkTag;
    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private StringEncodedValue stringEncodedValue;
    @Mock
    private EncodedTag encodedTag;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        StringParser stringParser = getStringParser(false);
        when(link.getTag(linkTag, "")).thenReturn(EXPECTED_A);

        stringParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(stringEncodedValue).setString(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        StringParser stringParser = getStringParser(true);
        when(link.getTag(linkTag, "", false)).thenReturn(EXPECTED_A);
        when(link.getTag(linkTag, "", true)).thenReturn(EXPECTED_B);

        stringParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(stringEncodedValue).setString(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(stringEncodedValue).setString(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

    private StringParser getStringParser(boolean separateValuesPerDirection) {
        doReturn(linkTag).when(encodedTag).getLinkTag();
        when(encodedTag.isSeparateValuesPerDirection()).thenReturn(separateValuesPerDirection);
        when(encodedTag.getKey()).thenReturn(TAG_KEY);
        when(encodedValueLookup.getStringEncodedValue(TAG_KEY)).thenReturn(stringEncodedValue);

        return new StringParser(encodedValueLookup, encodedTag);
    }
}
