package nu.ndw.nls.routingmapmatcher.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;

/**
 * The result of a route between two points is stored in a {@link RoutingLegResponse}.
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class RoutingLegResponse {

    private static final int INDEX_FIRST = 0;
    private static final int INDEX_SIZE_TO_LAST_CORRECTION = 1;

    private final List<MatchedLink> matchedLinks;

    /**
     * @return start fraction of the first matched link of this leg
     */
    public double getStartFraction() {
        return getFirstLink().getStartFraction();
    }

    /**
     @return end fraction of the last matched link of this leg
     */
    public double getEndFraction() {
        return getLastLink().getEndFraction();
    }

    /**
     * @return the first matched link
     */
    public MatchedLink getFirstLink() {
        return matchedLinks.get(INDEX_FIRST);
    }

    /**
     * @return the last matched link
     */
    public MatchedLink getLastLink() {
        return matchedLinks.get(matchedLinks.size()- INDEX_SIZE_TO_LAST_CORRECTION);
    }
}
