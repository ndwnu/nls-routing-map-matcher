package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
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
        private boolean reversed;
        private final List<IsochroneMatch> upstream;
        private final List<IsochroneMatch> downstream;
        private final Point snappedPoint;
        // Fraction of snapped point on link based on length in meters
        private final double fraction;
        // Distance in meters from input point to snapped point
        private final double distance;
        // Bearing of link segment containing snapped point
        private final double bearing;
        // Reliability of this candidate based on distance and bearing
        private final double reliability;
    }

    private final List<CandidateMatch> candidateMatches;

}
