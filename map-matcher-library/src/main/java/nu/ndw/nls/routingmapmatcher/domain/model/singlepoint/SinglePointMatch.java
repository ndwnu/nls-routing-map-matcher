package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;
import org.locationtech.jts.geom.Point;

@Getter
@ToString
public class SinglePointMatch extends MapMatch {

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class CandidateMatch {

        private final int matchedLinkId;
        private final Set<Integer> upstreamLinkIds;
        private final Set<Integer> downstreamLinkIds;
        private final Point snappedPoint;
        private final double fraction;
        //Distance in meters from input point to snapped point
        private final double distance;
        private final boolean reversed;
    }

    private final List<CandidateMatch> candidateMatches;

    public SinglePointMatch(final int id, final List<CandidateMatch> candidateMatches, final double reliability,
            final MatchStatus status) {
        super(id, status, reliability);
        this.candidateMatches = candidateMatches;
    }
}
