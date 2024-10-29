package nu.ndw.nls.routingmapmatcher.mappers;

import java.util.ArrayList;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedEdgeLink;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.springframework.stereotype.Component;

/**
 * {@link MatchedLinkMapper} maps a {@link List} of {@link MatchedEdgeLink} that describes a Leg/Path into a
 * {@link List of {@link MatchedLink}. The start fraction of the leg is applied to the first link and the end fraction
 * is applied to the last link. All other fractions are either 0 for start and 1 for end as they are fully routed from
 * start to end.
 */
@Component
public class MatchedLinkMapper {

    private static final double START_FRACTION_0 = 0.0D;
    private static final double END_FRACTION_1 = 1.0D;
    private static final int INDEX_FIRST = 0;

    public List<MatchedLink> map(List<MatchedEdgeLink> matchedEdgeLinks, double firstStartFraction,
            double lastEndFraction) {

        final int indexLast = matchedEdgeLinks.size() - 1;

        int i = 0;
        List<MatchedLink> matchedLinks = new ArrayList<>();
        for (MatchedEdgeLink matchedEdgeLink : matchedEdgeLinks) {
            double startFraction;
            double endFraction;

            if (i == INDEX_FIRST) {
                startFraction = firstStartFraction;
            } else {
                startFraction = START_FRACTION_0;
            }

            if (i == indexLast) {
                endFraction = lastEndFraction;
            } else {
                endFraction = END_FRACTION_1;
            }

            matchedLinks.add(MatchedLink.builder()
                    .linkId(matchedEdgeLink.getLinkId())
                    .reversed(matchedEdgeLink.isReversed())
                    .startFraction(startFraction)
                    .distance(matchedEdgeLink.getDistance())
                    .endFraction(endFraction)
                    .build());

            i++;
        }

        return matchedLinks;
    }
}
