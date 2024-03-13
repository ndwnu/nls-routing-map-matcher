package nu.ndw.nls.routingmapmatcher.model.routing;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@Getter
@ToString
@RequiredArgsConstructor
public class RoutingLegResponseSequence {

    private static final double DELTA = 0.000000001;
    private final RoutingLegResponse previousRoutingLegResponse;

    private final RoutingLegResponse nextRoutingLegResponse;

    public static RoutingLegResponseSequence of(RoutingLegResponse previous, RoutingLegResponse next) {
        return new RoutingLegResponseSequence(previous, next);
    }

    /**
     *
     * @return true if previous leg ends on the same road section as next leg begins, but in a reverse driving direction
     */
    public boolean isMakingUTurn() {
        return (isContinuingOnSameRoadSection() && !isContinuingInSameDrivingDirection() && !isLegsTransitionOnNode());
    }

    public boolean isLegsTransitionOnNode() {
        return isPreviousRoutingLegEndOnNode() && isNextRoutingLegBeginOnNode();
    }

    public boolean isPreviousRoutingLegEndOnNode() {
        return getPreviousRoutingLegResponse().getLastLink().getEndFraction()-1.0 < DELTA;
    }

    public boolean isNextRoutingLegBeginOnNode() {
        return getNextRoutingLegResponse().getFirstLink().getStartFraction() < DELTA;
    }


    /**
     * @return true if the previous leg ends on the same road section where the next leg begins
     */
    public boolean isContinuingOnSameRoadSection() {
        return  getPreviousRoutingLegResponse().getLastLink().getLinkId() ==
                getNextRoutingLegResponse().getFirstLink().getLinkId();
    }

    /**
     * Only meaningful when {@link RoutingLegResponseSequence#isContinuingOnSameRoadSection} is true
     * @return true if the previous legs last link and first link of the next leg have the same reverse boolean state
     */
    public boolean isContinuingInSameDrivingDirection() {
        return getPreviousRoutingLegResponse().getLastLink().isReversed() ==
                getNextRoutingLegResponse().getFirstLink().isReversed();
    }
}
