package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExploreLimitTest {

    @Mock
    private IsochroneLabel isochroneLabel;

    @Mock
    private IsochroneLabel isochroneLabelParent;

    @Mock
    private EncodingManager encodingManager;

    @Test
    void isInLimit() {
        ExploreLimit<IsochroneLabel> exploreLimit = createNewExploreLimit();

        assertThat(exploreLimit.isInLimit(isochroneLabel, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_applyLimitToParent() {
        when(isochroneLabel.getParent()).thenReturn(isochroneLabelParent);

        ExploreLimit<IsochroneLabel> exploreLimit = createNewExploreLimitWithParent();

        assertThat(exploreLimit.isInLimit(isochroneLabel, encodingManager)).isTrue();
    }

    @Test
    void isInLimit_notinLimit() {
        ExploreLimit<IsochroneLabel> exploreLimit = createNewExploreLimit();

        assertThat(exploreLimit.isInLimit(null, encodingManager)).isFalse();
    }

    @Test
    void isInLimit_applyLimitToParent_notinLimit() {

        ExploreLimit<IsochroneLabel> exploreLimit = createNewExploreLimitWithParent();

        assertThat(exploreLimit.isInLimit(isochroneLabel, encodingManager)).isFalse();
    }

    @Test
    void isInLimit_isRoot() {

        when(isochroneLabel.isRoot()).thenReturn(true);

        ExploreLimit<IsochroneLabel> exploreLimit = createNewExploreLimitWithParent();

        assertThat(exploreLimit.isInLimit(isochroneLabel, encodingManager)).isTrue();
    }

    private ExploreLimit<IsochroneLabel> createNewExploreLimit() {
        return new ExploreLimit<>(2, false) {

            @Override
            protected double getValueForLabel(IsochroneLabel incomingIsochroneLabel, EncodingManager incomingEncodingManager) {
                if (incomingIsochroneLabel == isochroneLabel && incomingEncodingManager == encodingManager) {
                    return 1;
                } else {
                    return 2;
                }
            }

            @Override
            public String debug(IsochroneLabel isochroneLabel, EncodingManager encodingManager) {
                return "limit=" + getLimit();
            }
        };
    }

    private ExploreLimit<IsochroneLabel> createNewExploreLimitWithParent() {
        return new ExploreLimit<>(2, true) {

            @Override
            protected double getValueForLabel(IsochroneLabel incomingIsochroneLabel, EncodingManager incomingEncodingManager) {
                if (isochroneLabelParent == incomingIsochroneLabel && encodingManager == incomingEncodingManager) {
                    return 1;
                } else {
                    return 2;
                }
            }

            @Override
            public String debug(IsochroneLabel isochroneLabel, EncodingManager encodingManager) {
                return "limit=" + getLimit();
            }
        };
    }
}
