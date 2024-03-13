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
    private final List<Point> snappedWaypoints;
    @Builder.Default
    private final LineString geometry;
    private final double weight;
    private final double duration;
    private final double distance;

    private final List<RoutingLegResponse> legs;


    /**
     * Convenience method
     * @return the legs, but then as a stream of sequential leg pairs
     */
    public Stream<RoutingLegResponseSequence> streamRoutingLegResponseSequences() {

        RoutingLegResponseSequencePairMapper routingLegResponseSequencePairMapper=
                new RoutingLegResponseSequencePairMapper();

        return legs.stream()
                    .map(routingLegResponseSequencePairMapper::map)
                    .flatMap(Optional::stream);
    }

    /**
     * Convenience method to get all matched links
     * @return matched links from all leg responses
     */
    public List<MatchedLink> getMatchedLinks() {
        return getLegs()
               .stream()
               .flatMap(routingLegResponse -> routingLegResponse.getMatchedLinks().stream())
               .toList();
    }

    /**
     * @return Combines {@link MatchedLink} if the route remains the same. When a link contains multiple snapped
     * waypoints it will return a {@link MatchedLink} for each section between two snapped points, these sections are
     * grouped back together into a single {@link MatchedLink} and the fraction will be adjusted to cover the entire
     * fraction covering the route over the {@link MatchedLink}.
     */
    public List<MatchedLink> getMatchedLinksGroupedBySameLinkAndDirection() {
        List<MatchedLink> matchedLinks = getMatchedLinks();

        List<MatchedLink> result = new ArrayList<>();
        MatchedLink lastLink = null;

        for (MatchedLink currentLink : matchedLinks) {
            if (lastLink == null || !isSameLinkContinuingInSameDirection(lastLink, currentLink)) {
                lastLink = currentLink;
                result.add(currentLink);
            } else if ( lastLink.getLinkId() == currentLink.getLinkId() &&
                        lastLink.isReversed() == currentLink.isReversed()) {

                // Replace last link with the same link, but extend it's end fraction to include the current links
                // end fraction. Effectively creating a new MatchedLink that covers the same start/end fractions
                // of both MatchedLinks combined
                result.remove(lastLink);
                result.add(lastLink.withEndFraction(currentLink.getEndFraction()));
            }
        }

        return result;
    }

    private boolean isSameLinkContinuingInSameDirection(MatchedLink previousLink, MatchedLink nextLink) {
        return previousLink.getLinkId() == nextLink.getLinkId() &&
                previousLink.isReversed() == nextLink.isReversed();
    }


}
