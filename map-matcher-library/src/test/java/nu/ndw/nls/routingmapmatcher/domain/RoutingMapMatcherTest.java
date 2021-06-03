package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingMapMatcherTest {
    @Mock
    private LineStringMapMatcherFactory lineStringMapMatcherFactory;

    @Mock
    private LineStringMapMatcher lineStringMapMatcher;

    @Mock
    private LineStringLocation lineStringLocation;

    @Mock
    private LineStringMatch lineStringMatch;

    private RoutingNetwork routingNetwork;

    private MapMatchingRequest mapMatchingRequest;

    @InjectMocks
    private RoutingMapMatcher routingMapMatcher;

    @BeforeEach
    void setup() {
        final Iterator<Link> links = Collections.emptyIterator();
        final List<LineStringLocation> lineStringLocations = Collections.singletonList(lineStringLocation);

        routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test network")
                .linkSupplier(() -> links)
                .build();
        mapMatchingRequest = MapMatchingRequest.builder()
                .locationTypeName("test location type")
                .locationSupplier(() -> lineStringLocations)
                .build();
    }

    @Test
    void testMatchLocations() {
        when(lineStringMapMatcherFactory.createLineStringMapMatcher(routingNetwork))
                .thenReturn(lineStringMapMatcher);
        when(lineStringMapMatcher.match(lineStringLocation)).thenReturn(lineStringMatch);
        when(lineStringMatch.getStatus()).thenReturn(MatchStatus.MATCH);
        final List<LineStringMatch> results = routingMapMatcher.matchLocations(routingNetwork, mapMatchingRequest)
                .collect(Collectors.toList());
        assertThat(results, hasSize(1));
    }
}
