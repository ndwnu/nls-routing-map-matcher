package nu.ndw.nls.routingmapmatcher;


import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WGS84_SRID;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.GEN_ACCESSIBLE;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.HGV_ACCESSIBLE;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_AXLE_LOAD;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_HEIGHT;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_LENGTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_WIDTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MUNICIPALITY_CODE;
import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.GHPoint;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.IndexedNetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomVehicleEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleDimensionModel;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleDimensions;
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
    private static final String TRUCK_PROFILE = "profile_truck";

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
        GHRequest req = getRequest(START_NODE_GH, END_NODE_GH, CAR_PROFILE);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 324.0);
    }

    @Test
    void route_okWithLongerRoute_restrictedForTruck() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(START_NODE_GH, END_NODE_GH, TRUCK_PROFILE);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 450.0);
    }

    @Test
    void route_okWithLongerRoute_restrictedInReverseForTruck() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH, TRUCK_PROFILE);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 450.0);
    }

    @Test
    void route_okWithShorterRoute_restrictionsLiftedAfterInit() {
        IndexedNetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH, TRUCK_PROFILE);

        GHResponse responseBefore = graphHopper.route(req);

        // Make faster route hgv accessible again
        setVehicleAccess(graphHopper, 1L, VehicleType.HGV, true, true);

        GHResponse responseAfter = graphHopper.route(req);

        assertBestDistance(responseBefore, 450.0);
        assertBestDistance(responseAfter, 324.0);
    }

    @Test
    void route_ok_vehicleLengthSpecificRestriction() {
        IndexedNetworkGraphHopper graphHopper = createGhNetwork();
        setVehicleAccess(graphHopper, 1L, VehicleType.HGV, true, false);

        VehicleDimensions shortHgv = VehicleDimensions.builder().length(3.5).build();
        VehicleDimensions longHgv = VehicleDimensions.builder().length(4.5).build();

        GHRequest shortHgvRequest = getRequest(START_NODE_GH, END_NODE_GH, TRUCK_PROFILE)
                .setCustomModel(new VehicleDimensionModel(shortHgv));
        GHRequest longHgvRequest = getRequest(START_NODE_GH, END_NODE_GH, TRUCK_PROFILE)
                .setCustomModel(new VehicleDimensionModel(longHgv));

        GHResponse shortHgvResponse = graphHopper.route(shortHgvRequest);
        GHResponse longHgvResponse = graphHopper.route(longHgvRequest);

        assertBestDistance(shortHgvResponse, 324.0);
        assertBestDistance(longHgvResponse, 450.0);
    }

    private static void setVehicleAccess(IndexedNetworkGraphHopper graphHopper, long edgeId, VehicleType vehicleType,
            boolean newValue, boolean reverse) {
        Integer edgeKey = graphHopper.getEdgeMap().get(edgeId);
        EdgeIteratorState edge = graphHopper.getBaseGraph().getEdgeIteratorStateForKey(edgeKey);
        BooleanEncodedValue encodedValue = graphHopper.getEncodingManager()
                .getBooleanEncodedValue(VehicleAccess.key(vehicleType.getName()));
        if (reverse) {
            edge.setReverse(encodedValue, newValue);
        } else {
            edge.set(encodedValue, newValue);
        }
    }

    private static IndexedNetworkGraphHopper createGhNetwork() {
        // Add restrictions on link 1
        addTags(link1);
        return getNetworkGraphHopper(linkList);
    }

    private static GHRequest getRequest(GHPoint from, GHPoint to, String profile) {
        return new GHRequest()
                .setProfile(profile)
                .addPoint(from)
                .addPoint(to);
    }

    private static void assertBestDistance(GHResponse response, double expectedDistance) {
        assertThat(response).isNotNull();
        assertThat(response.getBest()).isNotNull();
        assertThat(response.getBest().getDistance()).isEqualTo(expectedDistance);
    }

    private static void addTags(Link link) {
        link.setTag(MAX_HEIGHT, 3.5, false);
        link.setTag(MAX_WIDTH, 2.0, false);
        link.setTag(MAX_LENGTH, 4.0, false);
        link.setTag(MAX_AXLE_LOAD, 3.0, false);
        link.setTag(HGV_ACCESSIBLE, false, false);

        link.setTag(MAX_HEIGHT, 3.5, true);
        link.setTag(MAX_WIDTH, 2.0, true);
        link.setTag(MAX_LENGTH, 4.0, true);
        link.setTag(MAX_AXLE_LOAD, 3.0, true);
        link.setTag(HGV_ACCESSIBLE, false, true);

        link.setTag(MUNICIPALITY_CODE, 200);
        link.setTag(GEN_ACCESSIBLE, false);
    }


    private static IndexedNetworkGraphHopper getNetworkGraphHopper(List<Link> links) {
        IndexedNetworkGraphHopper graphHopper = new IndexedNetworkGraphHopper(links::iterator);
        Path path = Path.of("graphhopper", "test_network_2");
        graphHopper.setStoreOnFlush(false);
        graphHopper.setElevation(false);
        graphHopper.setVehicleEncodedValuesFactory(new CustomVehicleEncodedValuesFactory());
        graphHopper.setVehicleTagParserFactory(new LinkVehicleTagParsersFactory());
        graphHopper.setEncodedValueFactory(new CustomEncodedValuesFactory());
        graphHopper.setTagParserFactory(new LinkTagParserFactory());
        graphHopper.setProfiles(
                VehicleType.HGV.createProfile(TRUCK_PROFILE),
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
