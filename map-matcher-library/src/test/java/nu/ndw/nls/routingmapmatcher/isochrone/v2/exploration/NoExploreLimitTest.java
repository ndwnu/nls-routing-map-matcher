package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoExploreLimitTest {

    @Mock
    private EncodingManager encodingManager;

    private NoExploreLimit<IsochroneLabel> noExploreLimit;

    @BeforeEach
    void setUp() {
        noExploreLimit = new NoExploreLimit<>();
    }

    @Test
    void isInLimit() {
        IsochroneLabel label = new IsochroneLabel(0, -1, -1, null, Long.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
        assertThat(noExploreLimit.isInLimit(label, encodingManager)).isTrue();

        label = new IsochroneLabel(0, -1, -1, null, Long.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        assertThat(noExploreLimit.isInLimit(label, encodingManager)).isTrue();
    }
}
