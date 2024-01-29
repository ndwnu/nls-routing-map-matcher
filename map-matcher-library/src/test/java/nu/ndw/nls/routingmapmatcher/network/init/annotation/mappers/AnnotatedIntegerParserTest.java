package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.IntEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedIntegerParserTest {

    private static final int EDGE_ID = 1;
    private static final int EXPECTED_A = 2;
    private static final int EXPECTED_B = 3;
    private static final int INT_TOO_LARGE = 100;
    private static final String KEY = "test-link-value";
    private static final int MAX_STORABLE_INT = 10;

    @Mock
    private EdgeIntAccess egdeIntAccess;
    @Mock
    private Link link;
    @Mock
    private IntEncodedValue intEncodedValue;
    @Mock
    private EncodedValueDto<Link, Integer> encodedValueDto;
    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Test
    void handleWayTags_ok_oneValueForBothDirections() {
        when(encodedValueDto.isDirectional()).thenReturn(false);
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueLookup.getIntEncodedValue(KEY)).thenReturn(intEncodedValue);
        when(intEncodedValue.getMaxStorableInt()).thenReturn(MAX_STORABLE_INT);

        EncodedIntegerMapper<Link> intParser = new EncodedIntegerMapper<>(encodedValueLookup, encodedValueDto);

        intParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(intEncodedValue).setInt(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
    }

    @Test
    void handleWayTags_ok_separateValuesPerDirection() {
        when(encodedValueDto.isDirectional()).thenReturn(true);
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> EXPECTED_A);
        when(encodedValueDto.valueReverseSupplier()).thenReturn(link -> EXPECTED_B);
        when(encodedValueLookup.getIntEncodedValue(KEY)).thenReturn(intEncodedValue);
        when(intEncodedValue.getMaxStorableInt()).thenReturn(MAX_STORABLE_INT);

        EncodedIntegerMapper<Link> intParser = new EncodedIntegerMapper<>(encodedValueLookup, encodedValueDto);

        intParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null);

        verify(intEncodedValue).setInt(false, EDGE_ID, egdeIntAccess, EXPECTED_A);
        verify(intEncodedValue).setInt(true, EDGE_ID, egdeIntAccess, EXPECTED_B);
    }

    @Test
    void handleWayTags_exception_intOutOfBounds() {
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.valueSupplier()).thenReturn(link -> INT_TOO_LARGE);
        when(encodedValueLookup.getIntEncodedValue(KEY)).thenReturn(intEncodedValue);
        when(intEncodedValue.getName()).thenReturn("test_encoded_value");
        when(intEncodedValue.getMaxStorableInt()).thenReturn(MAX_STORABLE_INT);

        EncodedIntegerMapper<Link> intParser = new EncodedIntegerMapper<>(encodedValueLookup, encodedValueDto);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> intParser.handleWayTags(EDGE_ID, egdeIntAccess, link, null));
        assertThat(exception).hasMessage("Cannot store test-link-value: 100 as it is too large (> 10). "
                            + "You can disable test_encoded_value if you do not need it.");
    }

}
