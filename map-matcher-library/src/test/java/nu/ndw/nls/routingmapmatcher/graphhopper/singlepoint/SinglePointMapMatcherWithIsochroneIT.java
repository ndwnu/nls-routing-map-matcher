package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch.Direction;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatchWithIsochrone;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatchWithIsochrone.CandidateMatchWithIsochrone;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class SinglePointMapMatcherWithIsochroneIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private static final Coordinate START_POINT = new Coordinate(5.4267250, 52.1767242);
    private static final int UPSTREAM_ISOCHRONE_METERS = 200;
    private static final BearingFilter BEARING_FILTER = new BearingFilter(135, 10);
    private static final int CUTOFF_DISTANCE = 20;
    private static final int ID = 1;

    private SinglePointMapMatcher singlePointMapMatcher;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork() {
        String linksJson = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream(
                        SinglePointMapMatcherWithIsochroneIT.LINKS_RESOURCE)),
                StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });
        RoutingNetwork routingNetwork = RoutingNetwork.builder().networkNameAndVersion("test_network")
                .linkSupplier(links::iterator).build();

        GraphHopperSinglePointMapMatcherFactory graphHopperSinglePointMapMatcherFactory =
                new GraphHopperSinglePointMapMatcherFactory(new NetworkGraphHopperFactory());
        singlePointMapMatcher = graphHopperSinglePointMapMatcherFactory.createMapMatcher(routingNetwork);
        geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    @SneakyThrows
    @Test
    void matchWithDownstreamIsochrone_Meters_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .downstreamIsochrone(UPSTREAM_ISOCHRONE_METERS)
                .bearingFilter(BEARING_FILTER)
                .downstreamIsochroneUnit(IsochroneUnit.METERS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatchWithIsochrone result = singlePointMapMatcher.matchWithIsochrone(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatchWithIsochrone match = result.getCandidateMatches().get(0);
        assertThat(match.getDownstream(), hasSize(5));
        var startPoint = match.getDownstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.672874050063));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertThat(startPoint.getDirection(), is(Direction.FORWARD));
    }

    @SneakyThrows
    @Test
    void matchWithDownstreamIsochrone_Seconds_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .downstreamIsochrone(20)
                .bearingFilter(BEARING_FILTER)
                .downstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatchWithIsochrone result = singlePointMapMatcher.matchWithIsochrone(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatchWithIsochrone match = result.getCandidateMatches().get(0);
        assertThat(match.getDownstream(), hasSize(14));
        var startPoint = match.getDownstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.672874050063));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertThat(startPoint.getDirection(), is(Direction.FORWARD));
    }


    @SneakyThrows
    @Test
    void matchWithUpstreamIsochrone_Meters_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .upstreamIsochrone(UPSTREAM_ISOCHRONE_METERS)
                .bearingFilter(BEARING_FILTER)
                .upstreamIsochroneUnit(IsochroneUnit.METERS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatchWithIsochrone result = singlePointMapMatcher.matchWithIsochrone(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatchWithIsochrone match = result.getCandidateMatches().get(0);
        assertThat(match.getUpstream(), hasSize(5));
        var startPoint = match.getUpstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.327127525272));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertThat(startPoint.getDirection(), is(Direction.BACKWARD));
    }

    @SneakyThrows
    @Test
    void matchWithUpstreamIsochrone_Seconds_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .upstreamIsochrone(20)
                .bearingFilter(BEARING_FILTER)
                .upstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatchWithIsochrone result = singlePointMapMatcher.matchWithIsochrone(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatchWithIsochrone match = result.getCandidateMatches().get(0);
        assertThat(match.getUpstream(), hasSize(11));
        var startPoint = match.getUpstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.327127525272));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertThat(startPoint.getDirection(), is(Direction.BACKWARD));
    }

}
