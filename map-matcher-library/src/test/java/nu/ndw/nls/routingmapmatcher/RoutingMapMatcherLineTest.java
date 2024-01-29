package nu.ndw.nls.routingmapmatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.model.MapMatchingLineRequest;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.viterbi.ViterbiLineStringMapMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingMapMatcherLineTest {

    private static final String PROFILE_NAME = "car";
    @Mock
    private MapMatcherFactory<ViterbiLineStringMapMatcher> lineStringMapMatcherFactory;

    @Mock
    private ViterbiLineStringMapMatcher lineStringMapMatcher;

    @Mock
    private LineStringLocation lineStringLocation;

    @Mock
    private LineStringMatch lineStringMatch;
    @Mock
    private NetworkGraphHopper preInitializedNetwork;

    private MapMatchingLineRequest mapMatchingRequest;

    @InjectMocks
    private RoutingMapMatcher routingMapMatcher;

    @BeforeEach
    void setup() {
        List<LineStringLocation> lineStringLocations = Collections.singletonList(lineStringLocation);

        mapMatchingRequest = MapMatchingLineRequest.builder()
                .locationTypeName("test location type")
                .locationSupplier(() -> lineStringLocations)
                .build();
    }

    @Test
    void testMatchLocations() {
        when(lineStringMapMatcherFactory.createMapMatcher(preInitializedNetwork, PROFILE_NAME))
                .thenReturn(lineStringMapMatcher);
        when(lineStringMapMatcher.match(lineStringLocation)).thenReturn(lineStringMatch);
        when(lineStringMatch.getStatus()).thenReturn(MatchStatus.MATCH);
        Stream<LineStringMatch> results = routingMapMatcher.matchLocations(preInitializedNetwork, mapMatchingRequest,
                PROFILE_NAME);
        assertThat(results).hasSize(1);
    }
}
