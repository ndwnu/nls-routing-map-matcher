package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.IntEncodedValue;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntParserTest {

    private static final int EDGE_ID = 1;
    private static final int EXPECTED_A = 2;
    private static final int EXPECTED_B = 3;
    private static final int INT_TOO_LARGE = 100;
    private static final String TAG_KEY = "key";
    private static final int MAX_STORABLE_INT = 10;

    @Mock
    private LinkTag<Integer> linkTag;
    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private IntEncodedValue intEncodedValue;
    @Mock
    private EncodedTag encodedTag;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        IntParser intParser = getIntParser(false);
        when(link.getTag(linkTag, 0)).thenReturn(EXPECTED_A);

        intParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(intEncodedValue).setInt(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        IntParser intParser = getIntParser(true);
        when(link.getTag(linkTag, 0, false)).thenReturn(EXPECTED_A);
        when(link.getTag(linkTag, 0, true)).thenReturn(EXPECTED_B);

        intParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(intEncodedValue).setInt(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(intEncodedValue).setInt(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

    @Test
    void handleWayTags_exception_intOutOfBounds() {
        when(linkTag.getLabel()).thenReturn("test-link-tag");
        when(intEncodedValue.getName()).thenReturn("test_encoded_tag");
        IntParser intParser = getIntParser(false);
        when(link.getTag(linkTag, 0)).thenReturn(INT_TOO_LARGE);

        var exception = assertThrows(IllegalArgumentException.class,
                () -> intParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null));
        assertThat(exception).hasMessage("Cannot store test-link-tag: 100 as it is too large (> 10). "
                            + "You can disable test_encoded_tag if you do not need it.");
    }

    private IntParser getIntParser(boolean separateValuesPerDirection) {
        doReturn(linkTag).when(encodedTag).getLinkTag();
        when(encodedTag.isSeparateValuesPerDirection()).thenReturn(separateValuesPerDirection);
        when(encodedTag.getKey()).thenReturn(TAG_KEY);
        when(encodedValueLookup.getIntEncodedValue(TAG_KEY)).thenReturn(intEncodedValue);
        when(intEncodedValue.getMaxStorableInt()).thenReturn(MAX_STORABLE_INT);

        return new IntParser(encodedValueLookup, encodedTag);
    }
}
