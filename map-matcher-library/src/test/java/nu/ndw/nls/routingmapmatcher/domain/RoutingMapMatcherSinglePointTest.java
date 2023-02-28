package nu.ndw.nls.routingmapmatcher.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingSinglePointRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingMapMatcherSinglePointTest {

    @Mock
    private MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherFactory;

    @Mock
    private SinglePointMapMatcher singlePointMapMatcher;

    @Mock
    private SinglePointLocation singlePointLocation;

    @Mock
    private SinglePointMatch singlePointMatch;

    private RoutingNetwork routingNetwork;

    private MapMatchingSinglePointRequest mapMatchingRequest;

    @InjectMocks
    private RoutingMapMatcher routingMapMatcher;

    @BeforeEach
    void setup() {
        Iterator<Link> links = Collections.emptyIterator();
        List<SinglePointLocation> singlePointLocations = Collections.singletonList(singlePointLocation);

        routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test network")
                .linkSupplier(() -> links)
                .build();
        mapMatchingRequest = MapMatchingSinglePointRequest.builder()
                .locationTypeName("test location type")
                .locationSupplier(() -> singlePointLocations)
                .build();
    }

    @Test
    void testMatchLocations() {
        when(singlePointMapMatcherFactory.createMapMatcher(routingNetwork))
                .thenReturn(singlePointMapMatcher);
        when(singlePointMapMatcher.match(singlePointLocation)).thenReturn(singlePointMatch);
        when(singlePointMatch.getStatus()).thenReturn(MatchStatus.MATCH);
        List<SinglePointMatch> results = routingMapMatcher.matchLocations(routingNetwork, mapMatchingRequest)
                .collect(Collectors.toList());
        assertThat(results, hasSize(1));
    }
}
