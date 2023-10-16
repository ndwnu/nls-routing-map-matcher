package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.StringEncodedValue;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringParserTest {

    private static final String EXPECTED_A = "A";
    private static final String EXPECTED_B = "B";
    private static final String TAG_KEY = "key";

    @Mock
    LinkTag<String> linkTag;
    @Mock
    IntsRef edgeFlags;
    @Mock
    Link link;
    @Mock
    StringEncodedValue stringEncodedValue;
    @Mock
    EncodedTag encodedTag;
    @Mock
    EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        StringParser stringParser = getStringParser(false);
        when(link.getTag(linkTag, "")).thenReturn(EXPECTED_A);

        stringParser.handleWayTags(edgeFlags, link, null);

        verify(stringEncodedValue).setString(false, edgeFlags, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        StringParser stringParser = getStringParser(true);
        when(link.getTag(linkTag, "", false)).thenReturn(EXPECTED_A);
        when(link.getTag(linkTag, "", true)).thenReturn(EXPECTED_B);

        stringParser.handleWayTags(edgeFlags, link, null);

        verify(stringEncodedValue).setString(false, edgeFlags, EXPECTED_A);
        verify(stringEncodedValue).setString(true, edgeFlags, EXPECTED_B);
    }

    private StringParser getStringParser(boolean separateValuesPerDirection) {
        doReturn(linkTag).when(encodedTag).getLinkTag();
        when(encodedTag.isSeparateValuesPerDirection()).thenReturn(separateValuesPerDirection);
        when(encodedTag.getKey()).thenReturn(TAG_KEY);
        when(encodedValueLookup.getStringEncodedValue(TAG_KEY)).thenReturn(stringEncodedValue);

        return new StringParser(encodedValueLookup, encodedTag);
    }

}