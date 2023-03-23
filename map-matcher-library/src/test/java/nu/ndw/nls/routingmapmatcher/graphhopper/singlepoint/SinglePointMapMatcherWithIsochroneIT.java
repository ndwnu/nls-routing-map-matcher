package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSON;
import org.wololo.jts2geojson.GeoJSONWriter;

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
    private void setupNwbNetwork() {
        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("nwb_2022-07-01")
                .linkSupplier(() -> Collections.emptyIterator())
                .build();
        var factory = new NetworkGraphHopperFactory();
        NetworkGraphHopper nwbNetwork = factory.createNetwork(routingNetwork, true,
                Path.of("graphhopper"));
        GraphHopperSinglePointMapMatcherFactory graphHopperSinglePointMapMatcherFactory =
                new GraphHopperSinglePointMapMatcherFactory(factory);
        singlePointMapMatcher = graphHopperSinglePointMapMatcherFactory.createMapMatcher(nwbNetwork);
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
        assertThat(startPoint.getStartFraction(),is(0.672874050063));
        assertThat(startPoint.getEndFraction(),is(1.0));
        assertThat(startPoint.getDirection(),is(Direction.FORWARD));
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
        assertThat(startPoint.getStartFraction(),is(0.672874050063));
        assertThat(startPoint.getEndFraction(),is(1.0));
        assertThat(startPoint.getDirection(),is(Direction.FORWARD));
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
        assertThat(startPoint.getStartFraction(),is(0.327127525272));
        assertThat(startPoint.getEndFraction(),is(1.0));
        assertThat(startPoint.getDirection(),is(Direction.BACKWARD));
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
        assertThat(startPoint.getStartFraction(),is(0.327127525272));
        assertThat(startPoint.getEndFraction(),is(1.0));
        assertThat(startPoint.getDirection(),is(Direction.BACKWARD));
    }

    @SneakyThrows
    @Test
    void test_downStream_bidirectional(){
        setupNwbNetwork();
        Point point = geometryFactory.createPoint(new Coordinate(5.4267250, 52.179661));
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .downstreamIsochrone(200)
                .bearingFilter(new BearingFilter(260,10))
                .downstreamIsochroneUnit(IsochroneUnit.METERS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatchWithIsochrone result = singlePointMapMatcher.matchWithIsochrone(request);
        assertThat(result.getCandidateMatches(), hasSize(1));
        CandidateMatchWithIsochrone match = result.getCandidateMatches().get(0);

        List<Feature> featuresJson = new ArrayList<>();
        match.getDownstream().forEach(m -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("id", m.getMatchedLinkId());
            properties.put("startFraction", m.getStartFraction());
            properties.put("endFraction", m.getEndFraction());
            properties.put("direction", m.getDirection());
            GeoJSONWriter writer = new GeoJSONWriter();
            var geometry = writer.write(m.getGeometry());
            featuresJson.add(new Feature(m.getMatchedLinkId(), geometry, properties));

        });
        GeoJSONWriter writer = new GeoJSONWriter();
        GeoJSON json = writer.write(featuresJson);
        FileUtils.writeStringToFile(new File("/tmp/isochrone.geojson"), json.
                toString(), Charset.defaultCharset().name());


    }

}
