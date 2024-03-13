package nu.ndw.nls.routingmapmatcher.model.routing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
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

    @Mock
    private MatchedLink matchedLinkA;
    @Mock
    private MatchedLink matchedLinkB;
    @Mock
    private MatchedLink matchedLinkC;

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
    void getMatchedLinks_ok_asIs() {
        RoutingResponse routingResponse = RoutingResponse.builder()
                .legs(List.of(routingLegResponseA, routingLegResponseB))
                .build();

        when(routingLegResponseA.getMatchedLinks()).thenReturn(List.of(matchedLinkA, matchedLinkB));
        when(routingLegResponseB.getMatchedLinks()).thenReturn(List.of(matchedLinkC));

        assertEquals(List.of(matchedLinkA, matchedLinkB, matchedLinkC), routingResponse.getMatchedLinks());
    }

    @Test
    void getMatchedLinksGroupedBySameLinkAndDirection_ok_combinedMatchedLinks() {

        RoutingResponse routingResponse = RoutingResponse.builder()
                .legs(List.of(routingLegResponseA, routingLegResponseB))
                .build();

        when(routingLegResponseA.getMatchedLinks()).thenReturn(List.of(matchedLinkA, matchedLinkB));
        when(routingLegResponseB.getMatchedLinks()).thenReturn(List.of(matchedLinkC));

        when(matchedLinkA.getLinkId()).thenReturn(1);
        when(matchedLinkB.getLinkId()).thenReturn(2);
        when(matchedLinkC.getLinkId()).thenReturn(2);

        when(matchedLinkB.isReversed()).thenReturn(true);
        when(matchedLinkC.isReversed()).thenReturn(true);

        when(matchedLinkC.getEndFraction()).thenReturn(0.75);

        MatchedLink combinedMatchedLink = mock(MatchedLink.class);

        when(matchedLinkB.withEndFraction(0.75)).thenReturn(combinedMatchedLink);

        assertEquals(List.of(matchedLinkA, combinedMatchedLink),
                routingResponse.getMatchedLinksGroupedBySameLinkAndDirection());
    }

}