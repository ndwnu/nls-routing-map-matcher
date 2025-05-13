package nu.ndw.nls.routingmapmatcher.model.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.mappers.RoutingLegResponseSequencePairMapper;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RoutingResponse {

    @Builder.Default
    private final List<Point> snappedWaypoints = List.of();
    private final LineString geometry;
    private final double weight;
    private final double duration;
    private final double distance;
    @Builder.Default
    private final List<RoutingLegResponse> legs = List.of();

    /**
     * Convenience method
     *
     * @return the legs, but then as a stream of sequential leg pairs
     */
    public Stream<RoutingLegResponseSequence> streamRoutingLegResponseSequences() {
        RoutingLegResponseSequencePairMapper routingLegResponseSequencePairMapper = new RoutingLegResponseSequencePairMapper();

        return legs.stream()
                .map(routingLegResponseSequencePairMapper::map)
                .flatMap(Optional::stream);
    }

    /**
     * @return matched links from all route legs, where matched links split at waypoints (same link ID and direction) are merged back
     * together by combining the fractions and distance.
     */
    public List<MatchedLink> getMatchedLinksGroupedBySameLinkAndDirection() {
        List<MatchedLink> result = new ArrayList<>();

        for (MatchedLink currentLink : getMatchedLinks()) {
            if (result.isEmpty() || !isSameLinkContinuingInSameDirection(result.getLast(), currentLink)) {
                result.add(currentLink);
            } else {
                // Merge fractions and distance of last and current link
                MatchedLink lastLink = result.removeLast();
                result.add(lastLink.withEndFraction(currentLink.getEndFraction())
                        .withDistance(lastLink.getDistance() + currentLink.getDistance()));
            }
        }

        return result;
    }

    /**
     * Convenience method to get all matched links
     *
     * @return matched links from all route legs
     */
    private List<MatchedLink> getMatchedLinks() {
        return getLegs()
                .stream()
                .flatMap(routingLegResponse -> routingLegResponse.getMatchedLinks().stream())
                .toList();
    }

    private boolean isSameLinkContinuingInSameDirection(MatchedLink previousLink, MatchedLink nextLink) {
        return previousLink.getLinkId() == nextLink.getLinkId() &&
                previousLink.isReversed() == nextLink.isReversed();
    }
}
