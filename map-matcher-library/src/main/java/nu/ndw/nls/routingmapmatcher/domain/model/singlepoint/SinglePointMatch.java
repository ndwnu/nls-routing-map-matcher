package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;
import org.locationtech.jts.geom.Point;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SinglePointMatch extends MapMatch {

    @Builder
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class CandidateMatch {

        private final int matchedLinkId;
        private final Set<Integer> upstreamLinkIds;
        private final Set<Integer> downstreamLinkIds;
        private final Point snappedPoint;
        private final double fraction;
        // Distance in meters from input point to snapped point
        private final double distance;
        private final Double bearing;
        private final boolean reversed;
    }

    private final List<CandidateMatch> candidateMatches;
}
