package nu.ndw.nls.routingmapmatcher.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedEdgeLink;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.junit.jupiter.api.Test;

class MatchedLinkMapperTest {

    private static final double LAST_END_FRACTION = 0.75;
    private static final double FIRST_START_FRACTION = 0.25;
    private static final int LINK_ID_A = 1;
    private static final boolean REVERSED_A = true;
    private static final int LINK_ID_B = 2;
    private static final boolean REVERSED_B = false;
    private static final double END_FRACTION_MAX = 1.0;
    private static final double START_FRACTION_MIN = 0.0;
    private static final int LINK_ID_C = 3;
    private static final boolean REVERSED_C = true;
    private final MatchedLinkMapper matchedLinkMapper = new MatchedLinkMapper();

    MatchedEdgeLink matchedEdgeLinkA = MatchedEdgeLink.builder().linkId(LINK_ID_A).reversed(REVERSED_A).build();
    MatchedEdgeLink matchedEdgeLinkB = MatchedEdgeLink.builder().linkId(LINK_ID_B).reversed(REVERSED_B).build();
    MatchedEdgeLink matchedEdgeLinkC = MatchedEdgeLink.builder().linkId(LINK_ID_C).reversed(REVERSED_C).build();
    @Test
    void map_ok_oneLink() {
        List<MatchedEdgeLink> matchedEdgeLinks = List.of(matchedEdgeLinkA);

        assertEquals(
                List.of(MatchedLink.builder()
                        .linkId(LINK_ID_A)
                        .reversed(REVERSED_A)
                        .startFraction(FIRST_START_FRACTION)
                        .endFraction(LAST_END_FRACTION)
                        .build()),
                matchedLinkMapper.map(matchedEdgeLinks, FIRST_START_FRACTION, LAST_END_FRACTION));
    }


    @Test
    void map_ok_twoLinks() {
        List<MatchedEdgeLink> matchedEdgeLinks = List.of(matchedEdgeLinkA, matchedEdgeLinkB);

        assertEquals(
                List.of(MatchedLink.builder()
                            .linkId(LINK_ID_A)
                            .reversed(REVERSED_A)
                            .startFraction(FIRST_START_FRACTION)
                            .endFraction(END_FRACTION_MAX)
                            .build(),
                        MatchedLink.builder()
                            .linkId(LINK_ID_B)
                            .reversed(REVERSED_B)
                            .startFraction(START_FRACTION_MIN)
                            .endFraction(LAST_END_FRACTION)
                            .build()),
                matchedLinkMapper.map(matchedEdgeLinks, FIRST_START_FRACTION, LAST_END_FRACTION));
    }

    @Test
    void map_ok_threeLinks() {
        List<MatchedEdgeLink> matchedEdgeLinks = List.of(matchedEdgeLinkA, matchedEdgeLinkB, matchedEdgeLinkC);

        assertEquals(
                List.of(MatchedLink.builder()
                                .linkId(LINK_ID_A)
                                .reversed(REVERSED_A)
                                .startFraction(FIRST_START_FRACTION)
                                .endFraction(END_FRACTION_MAX)
                                .build(),
                        MatchedLink.builder()
                                .linkId(LINK_ID_B)
                                .reversed(REVERSED_B)
                                .startFraction(START_FRACTION_MIN)
                                .endFraction(END_FRACTION_MAX)
                                .build(),
                        MatchedLink.builder()
                                .linkId(LINK_ID_C)
                                .reversed(REVERSED_C)
                                .startFraction(START_FRACTION_MIN)
                                .endFraction(LAST_END_FRACTION)
                                .build()),
                matchedLinkMapper.map(matchedEdgeLinks, FIRST_START_FRACTION, LAST_END_FRACTION));
    }
}
