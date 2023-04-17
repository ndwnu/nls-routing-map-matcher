package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ViterbiLineStringMapMatcherIT {

    private LineStringMapMatcher viterbiLineStringMapMatcher;
    private ObjectMapper mapper;

    @SneakyThrows
    @BeforeEach
    void setup() {
        String linksJson = IOUtils.toString(getClass().getResourceAsStream("/test-data/links.json"));
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        module.addDeserializer(LineStringLocation.class, new LineStringLocationDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });
        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test_network")
                .linkSupplier(links::iterator).build();
        ViterbiLinestringMapMatcherFactory viterbiLinestringMapMatcherFactory =
                new ViterbiLinestringMapMatcherFactory(new NetworkGraphHopperFactory());
        viterbiLineStringMapMatcher = viterbiLinestringMapMatcherFactory.createMapMatcher(routingNetwork);
    }

    @SneakyThrows
    @Test
    void testMatch() {
        String locationJson = IOUtils
                .toString(getClass().getResourceAsStream("/test-data/matched_linestring_location.json"));
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(lineStringMatch.getMatchedLinkIds(), contains(3666097, 3666076, 3666077, 3666078, 3666079, 3666080,
                3666081, 3666082, 3666083, 3666084, 3666085, 3666086));
        assertThat(lineStringMatch.getUpstreamLinkIds(),
                containsInAnyOrder(3666097, 3666096, 3666095, 3666094, 7223062, 7223061));
        assertThat(lineStringMatch.getDownstreamLinkIds(),
                containsInAnyOrder(3666086, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217));
        assertThat(lineStringMatch.getStartLinkFraction(), is(0.8805534312637381));
        assertThat(lineStringMatch.getEndLinkFraction(), is(0.45960570331968187));
        assertThat(lineStringMatch.getReliability(), is(93.18611307333045));
        assertThat(lineStringMatch.getId(), is(29));
        assertThat(lineStringMatch.getLocationIndex(), is(-1));
        assertThat(lineStringMatch.isReversed(), is(true));
    }

    @SneakyThrows
    @Test
    void testNoMatch() {
        String locationJson = IOUtils
                .toString(getClass().getResourceAsStream("/test-data/unmatched_linestring_location.json"));
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getStatus(), is(MatchStatus.NO_MATCH));
        assertThat(lineStringMatch.getMatchedLinkIds(), hasSize(0));
        assertThat(lineStringMatch.getStartLinkFraction(), is(0.0));
        assertThat(lineStringMatch.getEndLinkFraction(), is(0.0));
        assertThat(lineStringMatch.getReliability(), is(0.0));
        assertThat(lineStringMatch.getId(), is(15));
        assertThat(lineStringMatch.getLocationIndex(), is(0));
        assertThat(lineStringMatch.isReversed(), is(false));
    }
}
