package nu.ndw.nls.routingmapmatcher.model.routing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingLegResponseTest {

    private static final double FRACTION = 0.25;
    @Mock
    private MatchedLink matchedLinkA;
    @Mock
    private MatchedLink matchedLinkB;
    @Mock
    private MatchedLink matchedLinkC;


    private RoutingLegResponse routingLegResponse;

    @BeforeEach
    void setUp() {
        routingLegResponse = RoutingLegResponse.builder()
                .matchedLinks(List.of(matchedLinkA, matchedLinkB, matchedLinkC)).build();
    }

    @Test
    void getStartFraction_ok() {
        RoutingLegResponse routingLegResponse = RoutingLegResponse.builder()
                .matchedLinks(List.of(matchedLinkA, matchedLinkB, matchedLinkC)).build();

        when(matchedLinkA.getStartFraction()).thenReturn(FRACTION);
        assertEquals(FRACTION, routingLegResponse.getStartFraction());
        verify(matchedLinkA).getStartFraction();
    }

    @Test
    void getEndFraction_ok() {
        RoutingLegResponse routingLegResponse = RoutingLegResponse.builder()
                .matchedLinks(List.of(matchedLinkA, matchedLinkB, matchedLinkC)).build();

        when(matchedLinkC.getEndFraction()).thenReturn(FRACTION);
        assertEquals(FRACTION, routingLegResponse.getEndFraction());
        verify(matchedLinkC).getEndFraction();
    }


    @Test
    void getFirstLink_ok() {
        assertEquals(matchedLinkA, routingLegResponse.getFirstLink());
    }

    @Test
    void getLastLink_ok() {
        assertEquals(matchedLinkC, routingLegResponse.getLastLink());
    }
}