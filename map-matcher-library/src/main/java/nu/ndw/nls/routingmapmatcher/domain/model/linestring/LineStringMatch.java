package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;
import org.locationtech.jts.geom.LineString;

import java.util.List;
import java.util.Set;

@Getter
@ToString
public class LineStringMatch extends MapMatch {

    private final int locationIndex;
    private final boolean reversed;
    private final List<Integer> matchedLinkIds;
    private final Set<Integer> upstreamLinkIds;
    private final Set<Integer> downstreamLinkIds;
    private final double startLinkFraction;
    private final double endLinkFraction;

    @ToString.Exclude
    private final LineString lineString;

    public LineStringMatch(int id, int locationIndex, boolean reversed,
            List<Integer> matchedLinkIds, Set<Integer> upstreamLinkIds,
            Set<Integer> downstreamLinkIds, double startLinkFraction, double endLinkFraction,
            double reliability, MatchStatus status, LineString lineString) {
        super(id, status, reliability);
        this.locationIndex = locationIndex;
        this.reversed = reversed;
        this.matchedLinkIds = matchedLinkIds;
        this.upstreamLinkIds = upstreamLinkIds;
        this.downstreamLinkIds = downstreamLinkIds;
        this.startLinkFraction = startLinkFraction;
        this.endLinkFraction = endLinkFraction;
        this.lineString = lineString;
    }
}
