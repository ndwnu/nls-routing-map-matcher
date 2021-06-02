package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import org.locationtech.jts.geom.LineString;

import java.util.List;
import java.util.Optional;

public class LineStringMatch {

    private final LineStringLocation location;
    private final List<Integer> ndwLinkIds;
    private final double startLinkFraction;
    private final double endLinkFraction;
    private final double reliability;
    private final MatchStatus status;
    private final LineString lineString;

    public LineStringMatch(final LineStringLocation location, final List<Integer> ndwLinkIds,
                           final double startLinkFraction, final double endLinkFraction, 
                           final double reliability, final MatchStatus status,
                           final LineString lineString) {
        this.location = location;
        this.ndwLinkIds = ndwLinkIds;
        this.startLinkFraction = startLinkFraction;
        this.endLinkFraction = endLinkFraction;
        this.reliability = reliability;
        this.status = status;
        this.lineString = lineString;
    }

    public LineStringLocation getLocation() {
        return location;
    }

    public List<Integer> getNdwLinkIds() {
        return ndwLinkIds;
    }

    public double getStartLinkFraction() {
        return startLinkFraction;
    }

    public double getEndLinkFraction() {
        return endLinkFraction;
    }

    public double getReliability() {
        return reliability;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public LineString getLineString() {
        return lineString;
    }

    public int getId() {
        return location.getId();
    }

    public Optional<Integer> getLocationIndex() {
        return location.getLocationIndex();
    }

    public Optional<Boolean> getReversed() {
        return location.getReversed();
    }
}
