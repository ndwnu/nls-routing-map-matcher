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
class ExploreDistanceLimitTest {

    private static final double LIMIT = 500.0;

    @Mock
    private EncodingManager encodingManager;

    private ExploreDistanceLimit<IsochroneLabel> exploreDistanceLimit;

    @BeforeEach
    void setUp() {
        exploreDistanceLimit = new ExploreDistanceLimit<>(LIMIT);
    }

    @Test
    void isInLimit() {
        IsochroneLabel label = new IsochroneLabel(0, 5, 5, 0.0, 0L, LIMIT - 0.1, null);

        assertThat(exploreDistanceLimit.isInLimit(label, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_distanceToLarge() {
        IsochroneLabel label = new IsochroneLabel(0, 5, 5, 0.0, 0L, LIMIT, null);

        assertThat(exploreDistanceLimit.isInLimit(label, encodingManager)).isFalse();
    }
}
