package nu.ndw.nls.routingmapmatcher.model.linestring;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.base.MapMatch;
import org.locationtech.jts.geom.LineString;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LineStringMatch extends MapMatch {

    private final int locationIndex;
    private final boolean reversed;

    private final List<MatchedLink> matchedLinks;
    private final List<IsochroneMatch> upstream;
    private final List<IsochroneMatch> downstream;

    private final double startLinkFraction;
    private final double endLinkFraction;
    @ToString.Exclude
    private final LineString lineString;
    private final double weight;
    private final double duration;
    private final double distance;

    public Set<Integer> getDownstreamLinkIds() {
        if (downstream == null) {
            return null;
        }
        return downstream.stream().map(IsochroneMatch::getMatchedLinkId).collect(Collectors.toSet());
    }

    public Set<Integer> getUpstreamLinkIds() {
        if (upstream == null) {
            return null;
        }
        return upstream.stream().map(IsochroneMatch::getMatchedLinkId).collect(Collectors.toSet());
    }

}
