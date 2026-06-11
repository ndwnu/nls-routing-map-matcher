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
class ExploreLimitTest {

    private ExploreLimit<IsochroneLabel> exploreLimit;

    @Mock
    private IsochroneLabel isochroneLabel;

    @Mock
    private EncodingManager encodingManager;

    @BeforeEach
    void setUp() {

        exploreLimit = new ExploreLimit<>(2) {

            @Override
            protected double getLimit(IsochroneLabel incomingIsochroneLabel, EncodingManager incomingEncodingManager) {
                if (isochroneLabel == incomingIsochroneLabel && encodingManager == incomingEncodingManager) {
                    return 1;
                } else {
                    return 2;
                }
            }
        };
    }

    @Test
    void isInLimit() {

        assertThat(exploreLimit.isInLimit(isochroneLabel, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_notinLimit() {

        assertThat(exploreLimit.isInLimit(null, null)).isFalse();
    }
}
