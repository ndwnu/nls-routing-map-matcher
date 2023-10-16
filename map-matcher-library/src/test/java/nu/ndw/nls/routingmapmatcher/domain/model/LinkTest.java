package nu.ndw.nls.routingmapmatcher.domain.model;

import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_WEIGHT;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MUNICIPALITY_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LinkTest {

    private final Link link = Link.builder().build();

    @Test
    void setAndGetTag_ok_separateValuesPerDirection() {
        link.setTag(MAX_WEIGHT, 2.0, false);
        link.setTag(MAX_WEIGHT, 3.0, true);

        assertEquals(2.0, link.getTag(MAX_WEIGHT, null, false));
        assertEquals(3.0, link.getTag(MAX_WEIGHT, null, true));
    }

    @Test
    void setAndGetTag_ok_oneValueForBothDirections() {
        link.setTag(MUNICIPALITY_CODE, 5);

        assertEquals(5, link.getTag(MUNICIPALITY_CODE, null));
    }

    @Test
    void setTag_exception_separateValuesButNoDirectionSpecified() {
        var exception = assertThrows(IllegalArgumentException.class, () -> link.setTag(MAX_WEIGHT, 2.0));
        assertThat(exception).hasMessage("Link tag max-weight stores separate values for both directions. "
                                         + "Use setTag method with boolean 'reverse' parameter.");
    }

    @Test
    void getTag_exception_separateValuesButNoDirectionSpecified() {
        var exception = assertThrows(IllegalArgumentException.class, () -> link.getTag(MAX_WEIGHT, null));
        assertThat(exception).hasMessage("Link tag max-weight stores separate values for both directions. "
                                         + "Use getTag method with boolean 'reverse' parameter.");
    }

    @Test
    void setTag_exception_oneValueButDirectionSpecified() {
        var exception = assertThrows(IllegalArgumentException.class, () -> link.setTag(MUNICIPALITY_CODE, 5, false));
        assertThat(exception).hasMessage("Link tag municipality-code does not store separate values for both directions."
                                         + " Use setTag method without boolean 'reverse' parameter.");
    }

    @Test
    void getTag_exception_oneValueButDirectionSpecified() {
        var exception = assertThrows(IllegalArgumentException.class, () -> link.getTag(MUNICIPALITY_CODE, 5, false));
        assertThat(exception).hasMessage("Link tag municipality-code does not store separate values for both directions."
                                         + " Use getTag method without boolean 'reverse' parameter.");
    }

}