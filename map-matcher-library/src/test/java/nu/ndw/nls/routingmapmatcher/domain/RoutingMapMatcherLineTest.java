package nu.ndw.nls.routingmapmatcher.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingLineRequest;
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

@ExtendWith(MockitoExtension.class)
class RoutingMapMatcherLineTest {

    @Mock
    private MapMatcherFactory<LineStringMapMatcher> lineStringMapMatcherFactory;

    @Mock
    private LineStringMapMatcher lineStringMapMatcher;

    @Mock
    private LineStringLocation lineStringLocation;

    @Mock
    private LineStringMatch lineStringMatch;

    private RoutingNetwork routingNetwork;

    private MapMatchingLineRequest mapMatchingRequest;

    @InjectMocks
    private RoutingMapMatcher routingMapMatcher;

    @BeforeEach
    void setup() {
        Iterator<Link> links = Collections.emptyIterator();
        List<LineStringLocation> lineStringLocations = Collections.singletonList(lineStringLocation);

        routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test network")
                .linkSupplier(() -> links)
                .build();
        mapMatchingRequest = MapMatchingLineRequest.builder()
                .locationTypeName("test location type")
                .locationSupplier(() -> lineStringLocations)
                .build();
    }

    @Test
    void testMatchLocations() {
        when(lineStringMapMatcherFactory.createMapMatcher(routingNetwork))
                .thenReturn(lineStringMapMatcher);
        when(lineStringMapMatcher.match(lineStringLocation)).thenReturn(lineStringMatch);
        when(lineStringMatch.getStatus()).thenReturn(MatchStatus.MATCH);
        List<LineStringMatch> results = routingMapMatcher.matchLocations(routingNetwork, mapMatchingRequest).toList();
        assertThat(results, hasSize(1));
    }
}
