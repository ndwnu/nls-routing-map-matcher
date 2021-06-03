package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import org.locationtech.jts.geom.LineString;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class LineStringMatch {

    private final LineStringLocation location;
    private final List<Integer> matchedLinkIds;
    private final double startLinkFraction;
    private final double endLinkFraction;
    private final double reliability;
    private final MatchStatus status;
    private final LineString lineString;

}
