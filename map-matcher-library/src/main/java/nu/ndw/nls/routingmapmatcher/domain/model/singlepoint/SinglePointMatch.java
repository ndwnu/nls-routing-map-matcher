package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
@Getter
public class SinglePointMatch {
    private final List<Integer> matchedLinkIds;
    private final MatchStatus status;
    private final List<Point> snappedPoints;
    private final double distance;
}
