package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import org.locationtech.jts.geom.LineString;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@ToString
public class LineStringMatch {

    private final int id;
    private final int locationIndex;
    private final boolean reversed;
    private final List<Integer> matchedLinkIds;
    private final Set<Integer> upstreamLinkIds;
    private final Set<Integer> downstreamLinkIds;
    private final double startLinkFraction;
    private final double endLinkFraction;
    private final double reliability;
    private final MatchStatus status;
    @ToString.Exclude
    private final LineString lineString;
}
