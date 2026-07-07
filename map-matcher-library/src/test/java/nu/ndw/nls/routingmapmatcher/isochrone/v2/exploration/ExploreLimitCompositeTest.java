package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExploreLimitCompositeTest {

    private ExploreLimitComposite<IsochroneLabel> exploreLimitComposite;

    @Mock
    private ExploreLimit<IsochroneLabel> exploreLimit1;

    @Mock
    private ExploreLimit<IsochroneLabel> exploreLimit2;

    @Mock
    private IsochroneLabel isochroneLabel;

    @Mock
    private EncodingManager encodingManager;

    @BeforeEach
    void setUp() {

        exploreLimitComposite = new ExploreLimitComposite<>(exploreLimit1, exploreLimit2);
    }

    @Test
    void constructor() {

        assertThat(exploreLimitComposite.isApplyLimitToParent()).isFalse();
    }

    @Test
    void isInLimit_inLimit() {

        when(exploreLimit1.isInLimit(isochroneLabel, encodingManager)).thenReturn(true);
        when(exploreLimit2.isInLimit(isochroneLabel, encodingManager)).thenReturn(true);

        assertThat(exploreLimitComposite.isInLimit(isochroneLabel, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_notInLimit() {

        when(exploreLimit1.isInLimit(isochroneLabel, encodingManager)).thenReturn(true);
        when(exploreLimit2.isInLimit(isochroneLabel, encodingManager)).thenReturn(false);

        assertThat(exploreLimitComposite.isInLimit(isochroneLabel, encodingManager)).isFalse();
    }

    @Test
    void debug() {
        when(exploreLimit1.isInLimit(isochroneLabel, encodingManager)).thenReturn(true);
        when(exploreLimit1.debug(isochroneLabel, encodingManager)).thenReturn("limit1");
        when(exploreLimit2.isInLimit(isochroneLabel, encodingManager)).thenReturn(true);
        when(exploreLimit2.debug(isochroneLabel, encodingManager)).thenReturn("limit2");

        assertThat(exploreLimitComposite.debug(isochroneLabel, encodingManager)).isEqualTo(
                "ExploreLimitComposite{limit=1.0, exploreLimits=[limit1, limit2], reached=false}");
    }
}
