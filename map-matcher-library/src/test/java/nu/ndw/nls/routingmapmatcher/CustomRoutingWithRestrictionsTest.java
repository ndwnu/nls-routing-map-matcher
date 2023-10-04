package nu.ndw.nls.routingmapmatcher;


import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.MULTIPLY;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.HGV_ACCESSIBLE;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_AXLE_LOAD;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_HEIGHT;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_LENGTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MAX_WIDTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MUNICIPALITY_CODE;
import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.ev.MaxAxleLoad;
import com.graphhopper.routing.ev.MaxHeight;
import com.graphhopper.routing.ev.MaxLength;
import com.graphhopper.routing.ev.MaxWeight;
import com.graphhopper.routing.ev.MaxWidth;
import com.graphhopper.routing.weighting.custom.CustomProfile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.shapes.GHPoint;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.HgvAccessible;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.LinkCarVehicleEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.LinkEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.MunicipalityCode;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.WayId;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkCarVehicleTagParsersFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkTagParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;

@Slf4j
public class CustomRoutingWithRestrictionsTest {

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setup() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    @Test
    void route_with_no_restrictions_ok() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = new GHRequest()
                .setProfile("custom_vehicle")
                .addPoint(new GHPoint(52.081079, 5.108409))
                .addPoint(new GHPoint(52.0811334, 5.1148870));
        GHResponse res = graphHopper.route(req);
        res.getBest().getPoints().toLineString(false);
        assertThat(res).isNotNull();
        assertThat(res.getBest()).isNotNull();
        assertThat(res.getBest().getDistance()).isEqualTo(324.0);
    }

    private NetworkGraphHopper createGhNetwork() {
        Link link0 = createLineLink(0, 0, 1, 100, 5.108409, 52.081079, 5.1099461, 52.0794303);
        Link link1 = createLineLink(1, 1, 2, 224, 5.1099461, 52.0794303, 5.1148870, 52.0811334);
        Link link2 = createLineLink(2, 1, 3, 100, 5.1099461, 52.0794303, 5.1110934, 52.0782690);
        Link link3 = createLineLink(3, 3, 2, 250, 5.1110934, 52.0782690, 5.1148870, 52.0811334);
        Link link4 = createLineLink(4, 2, 4, 100, 5.1110934, 52.0782690, 5.112295, 52.074527);
        // Add restrictions on this link
        addTags(link1);
        return getNetworkGraphHopper(link0, link1, link2, link3, link4);
    }

    private static void addTags(Link link1) {
        link1.setTag(MAX_HEIGHT, 3.5);
        link1.setTag(MAX_WIDTH, 2.0);
        link1.setTag(MAX_LENGTH, 4.0);
        link1.setTag(MAX_AXLE_LOAD, 3.0);
        link1.setTag(HGV_ACCESSIBLE, false);
        link1.setTag(MUNICIPALITY_CODE, 200);
    }


    @Test
    void route_with_restrictions_will_take_longer_route() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = new GHRequest()
                .setProfile("custom_vehicle")
                .addPoint(new GHPoint(52.081079, 5.108409))
                .addPoint(new GHPoint(52.0811334, 5.1148870));
        // Block quickest route by adding restriction expression in routing request
        CustomModel model = new CustomModel();
        model.addToPriority((If("hgv_accessible == false", MULTIPLY, "0")));
        req.setCustomModel(model);
        GHResponse res = graphHopper.route(req);
        assertThat(res).isNotNull();
        assertThat(res.getBest()).isNotNull();
        assertThat(res.getBest().getDistance()).isEqualTo(450.0);
    }


    @Test
    void route_with_restrictions_reverse_will_take_longer_route() {
        NetworkGraphHopper graphHopper = createGhNetwork();
        GHRequest req = new GHRequest()
                .setProfile("custom_vehicle")
                .addPoint(new GHPoint(52.0811334, 5.1148870))
                .addPoint(new GHPoint(52.081079, 5.108409));

        // Block quickest route by adding restriction expression in routing request
        CustomModel model = new CustomModel();
        model.addToPriority((If("hgv_accessible == false", MULTIPLY, "0")));
        req.setCustomModel(model);
        GHResponse res = graphHopper.route(req);
        assertThat(res).isNotNull();
        assertThat(res.getBest()).isNotNull();
        assertThat(res.getBest().getDistance()).isEqualTo(450.0);
    }

    private static NetworkGraphHopper getNetworkGraphHopper(Link link0, Link link1, Link link2, Link link3,
            Link link4) {
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(
                () -> List.of(link0, link1, link2, link3, link4).iterator());
        Path path = Path.of("graphhopper", "test_network");
        graphHopper.setStoreOnFlush(false);
        graphHopper.setElevation(false);
        graphHopper.setVehicleEncodedValuesFactory(new LinkCarVehicleEncodedValuesFactory());
        graphHopper.setVehicleTagParserFactory(new LinkCarVehicleTagParsersFactory());
        graphHopper.setEncodedValueFactory(new LinkEncodedValuesFactory());
        graphHopper.setTagParserFactory(new LinkTagParserFactory());
        graphHopper.setProfiles(new CustomProfile("custom_vehicle")
                .setCustomModel(new CustomModel())
                .setVehicle("car")
        );
        graphHopper.setEncodedValuesString(
                String.join(",", WayId.KEY, MaxWeight.KEY, MaxWidth.KEY, MaxLength.KEY, MaxAxleLoad.KEY, MaxHeight.KEY,
                        MunicipalityCode.KEY, HgvAccessible.KEY));
        graphHopper.setMinNetworkSize(0);
        graphHopper.setGraphHopperLocation(path.toString());
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);
        return graphHopper;
    }

    private Link createLineLink(long id, long fromNodeId, long toNodeId, int distance, double... coordinates) {
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

    @SneakyThrows
    private LineString createLineStringWktReader(double... coordinates) {
        if (coordinates == null || coordinates.length % 2 != 0) {
            throw new IllegalStateException("Must have coordinates and must come in pairs of two (x, y)");
        }

        StringBuilder lineStringSb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coordinates.length; i += 2) {
            if (i > 0) {
                lineStringSb.append(", ");
            }
            lineStringSb.append(coordinates[i]);
            lineStringSb.append(" ");
            lineStringSb.append(coordinates[i + 1]);
        }

        lineStringSb.append(")");

        log.debug("Loading line string: {}", lineStringSb);
        WKTReader wktReader = new WKTReader(this.geometryFactory);
        return (LineString) wktReader.read(lineStringSb.toString());
    }

}
