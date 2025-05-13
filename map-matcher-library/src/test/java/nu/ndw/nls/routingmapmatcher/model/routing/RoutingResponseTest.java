package nu.ndw.nls.routingmapmatcher.model.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingResponseTest {

    @Mock
    private RoutingLegResponse routingLegResponseA;
    @Mock
    private RoutingLegResponse routingLegResponseB;
    @Mock
    private RoutingLegResponse routingLegResponseC;
    @Mock
    private RoutingLegResponse routingLegResponseD;

    @Test
    void streamRoutingLegResponseSequences_ok() {
        RoutingResponse routingResponse = RoutingResponse.builder()
                .legs(List.of(routingLegResponseA, routingLegResponseB, routingLegResponseC, routingLegResponseD))
                .build();

        assertEquals(List.of(RoutingLegResponseSequence.of(routingLegResponseA, routingLegResponseB),
                        RoutingLegResponseSequence.of(routingLegResponseB, routingLegResponseC),
                        RoutingLegResponseSequence.of(routingLegResponseC, routingLegResponseD)),
                routingResponse.streamRoutingLegResponseSequences().toList());
    }

    @Test
    void getMatchedLinksGroupedBySameLinkAndDirection_ok_combinedMatchedLinks() {
        RoutingResponse routingResponse = RoutingResponse.builder()
                .legs(List.of(routingLegResponseA, routingLegResponseB, routingLegResponseC))
                .build();

        MatchedLink matchedLink1 = new MatchedLink(1, false, 0, 1, 2);
        MatchedLink matchedLink2 = new MatchedLink(2, true, 0, 0.33, 1);
        MatchedLink matchedLink3 = new MatchedLink(2, true, 0.33, 0.67, 1);
        MatchedLink matchedLink4 = new MatchedLink(2, true, 0.67, 1, 1);
        MatchedLink matchedLink5 = new MatchedLink(2, false, 0, 1, 1);

        when(routingLegResponseA.getMatchedLinks()).thenReturn(List.of(matchedLink1, matchedLink2));
        when(routingLegResponseB.getMatchedLinks()).thenReturn(List.of(matchedLink3));
        when(routingLegResponseC.getMatchedLinks()).thenReturn(List.of(matchedLink4, matchedLink5));

        List<MatchedLink> result = routingResponse.getMatchedLinksGroupedBySameLinkAndDirection();

        assertEquals(List.of(matchedLink1, new MatchedLink(2, true, 0, 1, 3), matchedLink5), result);
    }
}
