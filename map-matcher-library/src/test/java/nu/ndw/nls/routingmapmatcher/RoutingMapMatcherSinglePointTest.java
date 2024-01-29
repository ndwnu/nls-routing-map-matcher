package nu.ndw.nls.routingmapmatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.model.MapMatchingSinglePointRequest;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.singlepoint.SinglePointMapMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingMapMatcherSinglePointTest {

    private static final String PROFILE_NAME = "car";
    @Mock
    private MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherFactory;

    @Mock
    private SinglePointMapMatcher singlePointMapMatcher;

    @Mock
    private SinglePointLocation singlePointLocation;

    @Mock
    private SinglePointMatch singlePointMatch;

    @Mock
    private NetworkGraphHopper routingNetworkSettings;

    private MapMatchingSinglePointRequest mapMatchingRequest;

    @InjectMocks
    private RoutingMapMatcher routingMapMatcher;

    @BeforeEach
    void setup() {
        List<SinglePointLocation> singlePointLocations = Collections.singletonList(singlePointLocation);

        mapMatchingRequest = MapMatchingSinglePointRequest.builder()
                .locationTypeName("test location type")
                .locationSupplier(() -> singlePointLocations)
                .build();
    }

    @Test
    void testMatchLocations() {
        when(singlePointMapMatcherFactory.createMapMatcher(routingNetworkSettings, PROFILE_NAME))
                .thenReturn(singlePointMapMatcher);
        when(singlePointMapMatcher.match(singlePointLocation)).thenReturn(singlePointMatch);
        when(singlePointMatch.getStatus()).thenReturn(MatchStatus.MATCH);
        Stream<SinglePointMatch> results = routingMapMatcher.matchLocations(routingNetworkSettings, mapMatchingRequest,
                PROFILE_NAME);
        assertThat(results).hasSize(1);
    }
}
