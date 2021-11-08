package nu.ndw.nls.routingmapmatcher.domain.model.starttoend;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import org.locationtech.jts.geom.LineString;

import java.util.List;

@RequiredArgsConstructor
@Getter
@ToString
public class StartToEndMatch {

    private final int id;
    private final int locationIndex;
    private final List<Integer> matchedLinkIds;
    private final double startLinkFraction;
    private final double endLinkFraction;
    private final double reliability;
    private final MatchStatus status;
    @ToString.Exclude
    private final LineString lineString;
}