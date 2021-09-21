package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

class ViterbiLineStringMapMatcherIT {
    private LineStringMapMatcher viterbiLineStringMapMatcher;
    private ObjectMapper mapper;


    @SneakyThrows
    @BeforeEach
    private void setup() {
        String linksJson = IOUtils.toString(getClass().getResourceAsStream("/test-data/links.json"));
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        module.addDeserializer(LineStringLocation.class, new LineStringLocationDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<List<Link>>() {
        });
        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test_network")
                .linkSupplier(() -> links.iterator()).build();
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
        assertThat(lineStringMatch.getMatchedLinkIds(), hasSize(12));
        assertThat(lineStringMatch.getStartLinkFraction(), is(0.8802584207113416));
        assertThat(lineStringMatch.getEndLinkFraction(), is(0.45984987610479167));
        assertThat(lineStringMatch.getReliability(), is(93.18054557296587));
        assertThat(lineStringMatch.getLocation(), sameInstance(lineStringLocation));
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
        assertThat(lineStringMatch.getLocation(), sameInstance(lineStringLocation));
    }

}
