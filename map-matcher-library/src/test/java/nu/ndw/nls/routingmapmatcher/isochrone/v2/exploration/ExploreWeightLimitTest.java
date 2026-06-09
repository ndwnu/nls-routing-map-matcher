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
class ExploreWeightLimitTest {

    private static final double LIMIT = 100.0;

    @Mock
    private EncodingManager encodingManager;

    private ExploreWeightLimit<IsochroneLabel> exploreWeightLimit;

    @BeforeEach
    void setUp() {
        exploreWeightLimit = new ExploreWeightLimit<>(LIMIT);
    }

    @Test
    void isInLimit() {
        IsochroneLabel label = new IsochroneLabel(0, 5, 5, LIMIT - 0.1, 0L, 0.0, null);

        assertThat(exploreWeightLimit.isInLimit(label, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_weightExceeded() {
        IsochroneLabel label = new IsochroneLabel(0, 5, 5, LIMIT, 0L, 0.0, null);

        assertThat(exploreWeightLimit.isInLimit(label, encodingManager)).isFalse();
    }
}
