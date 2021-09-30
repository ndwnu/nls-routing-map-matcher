package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
@Getter
@ToString
public class SinglePointMatch {

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class CandidateMatch {

        private final int matchedLinkId;
        private final Set<Integer> upstreamLinkIds;
        private final Set<Integer> downstreamLinkIds;
        private final Point snappedPoint;
    }

    private final List<CandidateMatch> candidateMatches;
    private final double reliability;
    private final MatchStatus status;
}
