package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.MatchedLink;
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
        String linksJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/links.json")),
                StandardCharsets.UTF_8);
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
    void match_ok() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/matched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.18611307333045);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinkIds()).containsExactly(3666097, 3666076, 3666077, 3666078, 3666079,
                3666080, 3666081, 3666082, 3666083, 3666084, 3666085, 3666086);
        assertThat(lineStringMatch.getMatchedLinks()).noneMatch(MatchedLink::isReversed);
        assertThat(lineStringMatch.getUpstreamLinkIds())
                .containsExactlyInAnyOrder(3666097, 3666096, 3666095, 3666094, 7223062, 7223061);
        assertThat(lineStringMatch.getDownstreamLinkIds())
                .containsExactlyInAnyOrder(3666086, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217);
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.8805534312637381);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.45960570331968187);
        assertThat(lineStringMatch.getWeight()).isEqualTo(519.767);
        assertThat(lineStringMatch.getDuration()).isEqualTo(18.713);
        assertThat(lineStringMatch.getDistance()).isEqualTo(519.767);
    }

    @SneakyThrows
    @Test
    void match_noMatch() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/unmatched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(15);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.NO_MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(0.0);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(0);
        assertThat(lineStringMatch.isReversed()).isFalse();
        assertThat(lineStringMatch.getMatchedLinks()).isEmpty();
        assertThat(lineStringMatch.getUpstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getDownstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.0);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.0);
        assertThat(lineStringMatch.getWeight()).isEqualTo(0.0);
        assertThat(lineStringMatch.getDuration()).isEqualTo(0.0);
        assertThat(lineStringMatch.getDistance()).isEqualTo(0.0);
    }
}
