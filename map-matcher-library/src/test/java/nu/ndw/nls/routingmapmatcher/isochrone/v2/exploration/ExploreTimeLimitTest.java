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
class ExploreTimeLimitTest {

    private static final double LIMIT = 3600.0;

    @Mock
    private EncodingManager encodingManager;

    private ExploreTimeLimit<IsochroneLabel> exploreTimeLimit;

    @BeforeEach
    void setUp() {
        exploreTimeLimit = new ExploreTimeLimit<>(LIMIT);
    }

    @Test
    void isInLimit() {
        IsochroneLabel label = new IsochroneLabel(0, 5, 5, null, (long) (LIMIT - 0.1), 0.0, 0.0);

        assertThat(exploreTimeLimit.isInLimit(label, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_timeLimitExceeded() {
        IsochroneLabel label = new IsochroneLabel(0, 5, 5, null, (long) LIMIT, 0.0, 0.0);

        assertThat(exploreTimeLimit.isInLimit(label, encodingManager)).isFalse();
    }
}
