package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;
import org.locationtech.jts.geom.LineString;

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

    public LineStringMatch(final int id, final int locationIndex, final boolean reversed,
            final List<Integer> matchedLinkIds, final Set<Integer> upstreamLinkIds,
            final Set<Integer> downstreamLinkIds, final double startLinkFraction, final double endLinkFraction,
            final double reliability, final MatchStatus status, final LineString lineString) {
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
