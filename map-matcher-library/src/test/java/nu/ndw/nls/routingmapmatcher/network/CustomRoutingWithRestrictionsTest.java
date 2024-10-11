package nu.ndw.nls.routingmapmatcher.network;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.HGV_ACCESSIBLE_KEY;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestNetwork;
import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.json.Statement;
import com.graphhopper.json.Statement.Op;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.GHPoint;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

@Slf4j
public class CustomRoutingWithRestrictionsTest {

    private static final Coordinate START_NODE = new Coordinate(5.108409, 52.081079);
    private static final Coordinate NODE_A = new Coordinate(5.1099461, 52.0794303);
    private static final Coordinate NODE_B = new Coordinate(5.1110934, 52.0782690);
    private static final Coordinate NODE_C = new Coordinate(5.112295, 52.074527);
    private static final Coordinate END_NODE = new Coordinate(5.1148870, 52.0811334);
    private static final GHPoint START_NODE_GH = new GHPoint(START_NODE.y, START_NODE.x);
    private static final GHPoint END_NODE_GH = new GHPoint(END_NODE.y, END_NODE.x);

    private static final List<TestLink> linkList = List.of(
            createLineLink(0, 0, 1, 100, true, START_NODE, NODE_A),
            createLineLink(1, 1, 2, 224, false, NODE_A, END_NODE),
            createLineLink(2, 1, 3, 100, true, NODE_A, NODE_B),
            createLineLink(3, 3, 2, 250, true, NODE_B, END_NODE),
            createLineLink(4, 2, 4, 100, true, NODE_B, NODE_C)
    );

    private static final CustomModel HGV_MODEL = new CustomModel().addToPriority(
            Statement.If("hgv_accessible == false", Op.MULTIPLY, "0"));

    @Test
    void route_okWithShorterRoute_notRestrictedForCar() {
        NetworkGraphHopper graphHopper = getTestNetwork(linkList);
        GHRequest req = getRequest(START_NODE_GH, END_NODE_GH);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 324.0);
    }

    @Test
    void route_okWithLongerRoute_restrictedForTruck() {
        NetworkGraphHopper graphHopper = getTestNetwork(linkList);
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH).setCustomModel(HGV_MODEL);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 450.0);
    }

    @Test
    void route_okWithLongerRoute_restrictedInReverseForTruck() {
        NetworkGraphHopper graphHopper = getTestNetwork(linkList);
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH).setCustomModel(HGV_MODEL);

        GHResponse res = graphHopper.route(req);

        assertBestDistance(res, 450.0);
    }

    @Test
    void route_okWithShorterRoute_restrictionsLiftedAfterInit() {
        NetworkGraphHopper graphHopper = getTestNetwork(linkList);
        GHRequest req = getRequest(END_NODE_GH, START_NODE_GH).setCustomModel(HGV_MODEL);

        GHResponse responseBefore = graphHopper.route(req);

        // Make faster route hgv accessible again
        liftHgvRestriction(graphHopper);

        GHResponse responseAfter = graphHopper.route(req);

        assertBestDistance(responseBefore, 450.0);
        assertBestDistance(responseAfter, 324.0);
    }

    private static void liftHgvRestriction(NetworkGraphHopper graphHopper) {
        Integer edgeKey = graphHopper.getEdgeMap().get(1L);
        EdgeIteratorState edge = graphHopper.getBaseGraph().getEdgeIteratorStateForKey(edgeKey);
        BooleanEncodedValue encodedValue = graphHopper.getEncodingManager().getBooleanEncodedValue(HGV_ACCESSIBLE_KEY);
        edge.set(encodedValue, true);
    }

    private static GHRequest getRequest(GHPoint from, GHPoint to) {
        return new GHRequest()
                .setProfile(CAR)
                .addPoint(from)
                .addPoint(to);
    }

    private static void assertBestDistance(GHResponse response, double expectedDistance) {
        assertThat(response).isNotNull();
        assertThat(response.getBest()).isNotNull();
        assertThat(response.getBest().getDistance()).isEqualTo(expectedDistance);
    }

    private static TestLink createLineLink(long id, long fromNodeId, long toNodeId, int distance,
            boolean hgvAccessible, Coordinate... coordinates) {
        return TestLink.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(50)
                .reverseSpeedInKilometersPerHour(50)
                .hgvAccessible(hgvAccessible)
                .distanceInMeters(distance)
                .geometry(createLineStringWktReader(coordinates))
                .build();
    }

    private static LineString createLineStringWktReader(Coordinate... coordinates) {
        return new LineString(new CoordinateArraySequence(coordinates), new GeometryFactoryWgs84());
    }

}
