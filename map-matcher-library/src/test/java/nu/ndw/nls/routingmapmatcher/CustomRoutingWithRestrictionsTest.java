package nu.ndw.nls.routingmapmatcher;


import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WGS84_SRID;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C7_HGV_ACCESS_FORBIDDEN;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C20_MAX_AXLE_LOAD;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C19_MAX_HEIGHT;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C17_MAX_LENGTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C18_MAX_WIDTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MUNICIPALITY_CODE;
import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.GHPoint;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.VehicleProperties;
import nu.ndw.nls.routingmapmatcher.graphhopper.IndexedNetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomVehicleEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleRestrictionsModel;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkTagParserFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkVehicleTagParsersFactory;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

@Slf4j
public class CustomRoutingWithRestrictionsTest {

    private static final String CAR_PROFILE = "profile_car";


    private static final Coordinate START_NODE = new Coordinate(5.108409, 52.081079);
    private static final Coordinate NODE_A = new Coordinate(5.1099461, 52.0794303);
    private static final Coordinate NODE_B = new Coordinate(5.1110934, 52.0782690);
    private static final Coordinate NODE_C = new Coordinate(5.112295, 52.074527);
    private static final Coordinate END_NODE = new Coordinate(5.1148870, 52.0811334);
    private static final GHPoint START_NODE_GH = new GHPoint(START_NODE.y, START_NODE.x);
    private static final GHPoint END_NODE_GH = new GHPoint(END_NODE.y, END_NODE.x);

    private static final Link link0 = createLineLink(0, 0, 1, 100, START_NODE, NODE_A);
    private static final Link link1 = createLineLink(1, 1, 2, 224, NODE_A, END_NODE);
    private static final Link link2 = createLineLink(2, 1, 3, 100, NODE_A, NODE_B);
    private static final Link link3 = createLineLink(3, 3, 2, 250, NODE_B, END_NODE);
    private static final Link link4 = createLineLink(4, 2, 4, 100, NODE_B, NODE_C);
    private static final List<Link> linkList = List.of(link0, link1, link2, link3, link4);

    @Test
    void route_okWithShorterRoute_notRestrictedForCar() {
        IndexedNetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(START_NODE_GH, END_NODE_GH);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 324.0);
    }

    @Test
    void route_okWithLongerRoute_restrictedForTruck() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(START_NODE_GH, END_NODE_GH);
        req.setCustomModel(new VehicleRestrictionsModel(VehicleProperties
                .builder().hgvAccessForbidden(true).build()));
        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 450.0);
    }

    @Test
    void route_okWithLongerRoute_restrictedInReverseForTruck() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH);
        req.setCustomModel(new VehicleRestrictionsModel(VehicleProperties
                .builder().hgvAccessForbidden(true).build()));
        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 450.0);
    }

    @Test
    void route_okWithShorterRoute_restrictionsLiftedAfterInit() {
        IndexedNetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH);
        req.setCustomModel(new VehicleRestrictionsModel(VehicleProperties
                .builder().hgvAccessForbidden(true).build()));

        GHResponse responseBefore = graphHopper.route(req);

        // Make faster route hgv accessible again
        setVehicleAccess(graphHopper, true);

        GHResponse responseAfter = graphHopper.route(req);

        assertBestDistance(responseBefore, 450.0);
        assertBestDistance(responseAfter, 324.0);
    }

    @Test
    void route_ok_vehicleLengthSpecificRestriction() {
        IndexedNetworkGraphHopper graphHopper = createGhNetwork();
        setVehicleAccess(graphHopper, false);

        VehicleProperties shortHgv = VehicleProperties.builder().length(3.5).build();
        VehicleProperties longHgv = VehicleProperties.builder().length(4.5).build();

        GHRequest shortHgvRequest = getRequest(START_NODE_GH, END_NODE_GH)
                .setCustomModel(new VehicleRestrictionsModel(shortHgv));
        GHRequest longHgvRequest = getRequest(START_NODE_GH, END_NODE_GH)
                .setCustomModel(new VehicleRestrictionsModel(longHgv));

        GHResponse shortHgvResponse = graphHopper.route(shortHgvRequest);
        GHResponse longHgvResponse = graphHopper.route(longHgvRequest);

        assertBestDistance(shortHgvResponse, 324.0);
        assertBestDistance(longHgvResponse, 450.0);
    }

    private static void setVehicleAccess(IndexedNetworkGraphHopper graphHopper,
            boolean reverse) {
        Integer edgeKey = graphHopper.getEdgeMap().get(1L);
        EdgeIteratorState edge = graphHopper.getBaseGraph().getEdgeIteratorStateForKey(edgeKey);
        BooleanEncodedValue encodedValue = graphHopper.getEncodingManager()
                .getBooleanEncodedValue(EncodedTag.HGV_ACCESS_FORBIDDEN.getKey());
        if (reverse) {
            edge.setReverse(encodedValue, false);
        } else {
            edge.set(encodedValue, false);
        }
    }

    private static IndexedNetworkGraphHopper createGhNetwork() {
        // Add restrictions on link 1
        addTags();
        return getNetworkGraphHopper();
    }

    private static GHRequest getRequest(GHPoint from, GHPoint to) {
        return new GHRequest()
                .setProfile(CustomRoutingWithRestrictionsTest.CAR_PROFILE)
                .addPoint(from)
                .addPoint(to);
    }

    private static void assertBestDistance(GHResponse response, double expectedDistance) {
        assertThat(response).isNotNull();
        assertThat(response.getBest()).isNotNull();
        assertThat(response.getBest().getDistance()).isEqualTo(expectedDistance);
    }

    private static void addTags() {
        CustomRoutingWithRestrictionsTest.link1.setTag(C19_MAX_HEIGHT, 3.5, false);
        CustomRoutingWithRestrictionsTest.link1.setTag(C18_MAX_WIDTH, 2.0, false);
        CustomRoutingWithRestrictionsTest.link1.setTag(C17_MAX_LENGTH, 4.0, false);
        CustomRoutingWithRestrictionsTest.link1.setTag(C20_MAX_AXLE_LOAD, 3.0, false);
        CustomRoutingWithRestrictionsTest.link1.setTag(C7_HGV_ACCESS_FORBIDDEN, true, false);

        CustomRoutingWithRestrictionsTest.link1.setTag(C19_MAX_HEIGHT, 3.5, true);
        CustomRoutingWithRestrictionsTest.link1.setTag(C18_MAX_WIDTH, 2.0, true);
        CustomRoutingWithRestrictionsTest.link1.setTag(C17_MAX_LENGTH, 4.0, true);
        CustomRoutingWithRestrictionsTest.link1.setTag(C20_MAX_AXLE_LOAD, 3.0, true);
        CustomRoutingWithRestrictionsTest.link1.setTag(C7_HGV_ACCESS_FORBIDDEN, true, true);

        CustomRoutingWithRestrictionsTest.link1.setTag(MUNICIPALITY_CODE, 200);
    }


    private static IndexedNetworkGraphHopper getNetworkGraphHopper() {
        IndexedNetworkGraphHopper graphHopper = new IndexedNetworkGraphHopper(
                CustomRoutingWithRestrictionsTest.linkList::iterator);
        Path path = Path.of("graphhopper", "test_network_2");
        graphHopper.setStoreOnFlush(false);
        graphHopper.setElevation(false);
        graphHopper.setVehicleEncodedValuesFactory(new CustomVehicleEncodedValuesFactory());
        graphHopper.setVehicleTagParserFactory(new LinkVehicleTagParsersFactory());
        graphHopper.setEncodedValueFactory(new CustomEncodedValuesFactory());
        graphHopper.setTagParserFactory(new LinkTagParserFactory());
        graphHopper.setProfiles(
                VehicleType.CAR.createProfile(CAR_PROFILE)
        );
        graphHopper.setEncodedValuesString(
                Stream.of(EncodedTag.values())
                        .map(EncodedTag::getKey)
                        .collect(Collectors.joining(",")));
        graphHopper.setMinNetworkSize(0);
        graphHopper.setGraphHopperLocation(path.toString());
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);
        return graphHopper;
    }

    private static Link createLineLink(long id, long fromNodeId, long toNodeId, int distance,
            Coordinate... coordinates) {
        return Link.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(50)
                .reverseSpeedInKilometersPerHour(50)
                .distanceInMeters(distance)
                .geometry(createLineStringWktReader(coordinates))
                .build();
    }

    private static LineString createLineStringWktReader(Coordinate... coordinates) {
        return new LineString(new CoordinateArraySequence(coordinates),
                new GeometryFactory(new PrecisionModel(), WGS84_SRID));
    }

}
