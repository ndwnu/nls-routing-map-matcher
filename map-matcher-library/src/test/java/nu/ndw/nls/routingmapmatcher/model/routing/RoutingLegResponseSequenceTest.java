package nu.ndw.nls.routingmapmatcher.model.routing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingLegResponseSequenceTest {

    @Mock
    private RoutingLegResponse previous;
    @Mock
    private MatchedLink previousLastMatchedLink;

    @Mock
    private RoutingLegResponse next;
    @Mock
    private MatchedLink nextFirstMatchedLink;

    private RoutingLegResponseSequence routingLegResponseSequence;

    @BeforeEach
    void setUp() {
        routingLegResponseSequence = RoutingLegResponseSequence.of(previous, next);
    }

    @Test
    void of_ok() {
        assertEquals(previous, routingLegResponseSequence.getPreviousRoutingLegResponse());
        assertEquals(next, routingLegResponseSequence.getNextRoutingLegResponse());
    }

    @Test
    void isMakingUTurn_ok_trueSameRoadSectionReverseDirectionAndNotTurningOnNode() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);
        when(previousLastMatchedLink.isReversed()).thenReturn(false);
        when(nextFirstMatchedLink.isReversed()).thenReturn(true);
        when(previousLastMatchedLink.getLinkId()).thenReturn(1);
        when(nextFirstMatchedLink.getLinkId()).thenReturn(1);
        when(previousLastMatchedLink.getEndFraction()).thenReturn(0.75);

        assertTrue(routingLegResponseSequence.isMakingUTurn());

        verify(previousLastMatchedLink).getLinkId();
        verify(nextFirstMatchedLink).getLinkId();
        verify(previousLastMatchedLink).isReversed();
        verify(nextFirstMatchedLink).isReversed();
        verify(previousLastMatchedLink).getEndFraction();
    }

    @Test
    void isMakingUTurn_ok_falseDifferentRoadSectionReverseDirection() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);
        when(previousLastMatchedLink.getLinkId()).thenReturn(1);
        when(nextFirstMatchedLink.getLinkId()).thenReturn(2);

        assertFalse(routingLegResponseSequence.isMakingUTurn());

        verify(previousLastMatchedLink).getLinkId();
        verify(nextFirstMatchedLink).getLinkId();
    }

    @Test
    void isMakingUTurn_ok_falseSameRoadSectionSameDirection() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);
        when(previousLastMatchedLink.isReversed()).thenReturn(true);
        when(nextFirstMatchedLink.isReversed()).thenReturn(true);
        when(previousLastMatchedLink.getLinkId()).thenReturn(1);
        when(nextFirstMatchedLink.getLinkId()).thenReturn(1);

        assertFalse(routingLegResponseSequence.isMakingUTurn());

        verify(previousLastMatchedLink).getLinkId();
        verify(nextFirstMatchedLink).getLinkId();
        verify(previousLastMatchedLink).isReversed();
        verify(nextFirstMatchedLink).isReversed();
    }

    @Test
    void isContinuingOnSameRoadSection_ok_true() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);

        when(previousLastMatchedLink.getLinkId()).thenReturn(1);
        when(nextFirstMatchedLink.getLinkId()).thenReturn(1);
        assertTrue(routingLegResponseSequence.isContinuingOnSameRoadSection());
        verify(previousLastMatchedLink).getLinkId();
        verify(nextFirstMatchedLink).getLinkId();
    }

    @Test
    void isContinuingOnSameRoadSection_ok_false() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);

        when(previousLastMatchedLink.getLinkId()).thenReturn(1);
        when(nextFirstMatchedLink.getLinkId()).thenReturn(2);
        assertFalse(routingLegResponseSequence.isContinuingOnSameRoadSection());
        verify(previousLastMatchedLink).getLinkId();
        verify(nextFirstMatchedLink).getLinkId();
    }

    @Test
    void isContinuingInOppositeDrivingDirection_ok_bothTrue() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);

        when(previousLastMatchedLink.isReversed()).thenReturn(false);
        when(nextFirstMatchedLink.isReversed()).thenReturn(false);
        assertTrue(routingLegResponseSequence.isContinuingInSameDrivingDirection());
        verify(previousLastMatchedLink).isReversed();
        verify(nextFirstMatchedLink).isReversed();
    }

    @Test
    void isContinuingInOppositeDrivingDirection_ok_bothFalse() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);

        when(previousLastMatchedLink.isReversed()).thenReturn(false);
        when(nextFirstMatchedLink.isReversed()).thenReturn(false);
        assertTrue(routingLegResponseSequence.isContinuingInSameDrivingDirection());
        verify(previousLastMatchedLink).isReversed();
        verify(nextFirstMatchedLink).isReversed();
    }
    @Test
    void isContinuingInOppositeDrivingDirection_ok_trueFalse() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);

        when(previousLastMatchedLink.isReversed()).thenReturn(true);
        when(nextFirstMatchedLink.isReversed()).thenReturn(false);
        assertFalse(routingLegResponseSequence.isContinuingInSameDrivingDirection());
        verify(previousLastMatchedLink).isReversed();
        verify(nextFirstMatchedLink).isReversed();
    }

    @Test
    void isContinuingInOppositeDrivingDirection_ok_falseTrue() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);

        when(previousLastMatchedLink.isReversed()).thenReturn(false);
        when(nextFirstMatchedLink.isReversed()).thenReturn(true);
        assertFalse(routingLegResponseSequence.isContinuingInSameDrivingDirection());
        verify(previousLastMatchedLink).isReversed();
        verify(nextFirstMatchedLink).isReversed();
    }

    @Test
    void isLegsTransitionOnNode_ok_true() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(previousLastMatchedLink.getEndFraction()).thenReturn(1.0);
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);
        when(nextFirstMatchedLink.getStartFraction()).thenReturn(0.0);
        assertTrue(routingLegResponseSequence.isLegsTransitionOnNode());
        verify(previous).getLastLink();
        verify(previousLastMatchedLink).getEndFraction();
        verify(next).getFirstLink();
        verify(nextFirstMatchedLink).getStartFraction();
    }

    @Test
    void isLegsTransitionOnNode_ok_false() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(previousLastMatchedLink.getEndFraction()).thenReturn(0.25);
        assertFalse(routingLegResponseSequence.isLegsTransitionOnNode());
        verify(previous).getLastLink();
        verify(previousLastMatchedLink).getEndFraction();
    }

    @Test
    void isPreviousRoutingLegEndOnNode_ok_true() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(previousLastMatchedLink.getEndFraction()).thenReturn(1.0);
        assertTrue(routingLegResponseSequence.isPreviousRoutingLegEndOnNode());
        verify(previous).getLastLink();
        verify(previousLastMatchedLink).getEndFraction();
    }

    @Test
    void isPreviousRoutingLegEndOnNode_ok_false() {
        when(previous.getLastLink()).thenReturn(previousLastMatchedLink);
        when(previousLastMatchedLink.getEndFraction()).thenReturn(0.24);
        assertFalse(routingLegResponseSequence.isPreviousRoutingLegEndOnNode());
        verify(previous).getLastLink();
        verify(previousLastMatchedLink).getEndFraction();
    }

    @Test
    void isNextRoutingLegBeginOnNode_ok_true() {
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);
        when(nextFirstMatchedLink.getStartFraction()).thenReturn(0.0);
        assertTrue(routingLegResponseSequence.isNextRoutingLegBeginOnNode());
        verify(next).getFirstLink();
        verify(nextFirstMatchedLink).getStartFraction();
    }

    @Test
    void isNextRoutingLegBeginOnNode_ok_false() {
        when(next.getFirstLink()).thenReturn(nextFirstMatchedLink);
        when(nextFirstMatchedLink.getStartFraction()).thenReturn(0.24);
        assertFalse(routingLegResponseSequence.isNextRoutingLegBeginOnNode());
        verify(next).getFirstLink();
        verify(nextFirstMatchedLink).getStartFraction();
    }
}
