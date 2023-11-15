package nu.ndw.nls.routingmapmatcher.graphhopper.accessibility;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WGS84_GEOMETRY_FACTORY;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C17_MAX_LENGTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C18_MAX_WIDTH;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C19_MAX_HEIGHT;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C20_MAX_AXLE_LOAD;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.C7_HGV_ACCESS_FORBIDDEN;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.MUNICIPALITY_CODE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.AccessibilityRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.VehicleProperties;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingProfile;
import nu.ndw.nls.routingmapmatcher.graphhopper.IndexedNetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomVehicleEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkTagParserFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkVehicleTagParsersFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

class GraphHopperAccessibilityMapIT {

    private static final Coordinate START_NODE = new Coordinate(0.0, 0.0);
    private static final Coordinate NODE_A = new Coordinate(0.0, 0.0);
    private static final Coordinate NODE_B = new Coordinate(10.0, 0.0);
    private static final Coordinate NODE_C = new Coordinate(20.0, 0);
    private static final Coordinate NODE_D = new Coordinate(20.0, 20.0);
    private static final Coordinate NODE_E = new Coordinate(10.0, 20.0);
    private static final Coordinate NODE_F = new Coordinate(0, 20.0);

    private static final Coordinate NODE_A_A = new Coordinate(5.0, 5.0);
    private static final Coordinate NODE_B_B = new Coordinate(10.0, 5.0);
    private static final Coordinate NODE_C_C = new Coordinate(15.0, 5.0);
    private static final Coordinate NODE_D_D = new Coordinate(15.0, 15.0);
    private static final Coordinate NODE_E_E = new Coordinate(10.0, 15.0);
    private static final Coordinate NODE_F_F = new Coordinate(5, 15.0);

    private static final Link link0 = createLineLink(0, 0, 1, 10, NODE_A, NODE_B);
    private static final Link link1 = createLineLink(1, 1, 2, 10, NODE_B, NODE_C);
    private static final Link link2 = createLineLink(2, 2, 3, 20, NODE_C, NODE_D);
    private static final Link link3 = createLineLink(3, 3, 4, 10, NODE_D, NODE_E);
    private static final Link link4 = createLineLink(4, 4, 5, 10, NODE_E, NODE_F);
    private static final Link link5 = createLineLink(5, 5, 0, 20, NODE_F, NODE_A);

    private static final Link link0_0 = createLineLink(6, 6, 7, 5, NODE_A_A, NODE_B_B);
    private static final Link link1_1 = createLineLink(7, 7, 8, 5, NODE_B_B, NODE_C_C);
    private static final Link link2_2 = createLineLink(8, 8, 9, 10, NODE_C_C, NODE_D_D);
    private static final Link link3_3 = createLineLink(9, 9, 10, 5, NODE_D_D, NODE_E_E);
    private static final Link link4_4 = createLineLink(10, 10, 11, 5, NODE_E_E, NODE_F_F);
    private static final Link link5_5 = createLineLink(11, 11, 6, 10, NODE_F_F, NODE_A_A);

    private static final Link link1_1_link1 = createLineLink(12, 7, 1, 5, NODE_B_B, NODE_B);

    private static final Link link3_3_link3 = createLineLink(13, 10, 4, 5, NODE_E_E, NODE_E);
    private static final List<Link> linkList = List.of(link0,
            link1,
            link2,
            link3,
            link4,
            link5,
            link0_0,
            link1_1,
            link2_2,
            link3_3,
            link4_4,
            link5_5,
            link1_1_link1,
            link3_3_link3
    );
    private static final double SEARCH_DISTANCE_IN_METRES = 500.0;
    private IndexedNetworkGraphHopper graphHopper;

    /*
         Test network for accessibility
         If 4-10 and 1-7 links are blocked and the start point is 0,0,
         the links in the inner square are not accessible as well.

          5----4-----3
          |    |     |
          | 11-10-9  |
          | |     |  |
          | 6--7--8  |
          |    |     |
          0----1-----2
    */
    @Test
    void getAccessibleRoadSections_ok_withRestrictions() {
        addRestrictions(link1_1_link1);
        addRestrictions(link3_3_link3);
        graphHopper = createGhNetwork();
        final Set<IsochroneMatch> notAccessible = getIsochroneMatches();
        // restricted access links 1-7 and 4-10  plus inner square links 8 in total in both directions
        assertThat(notAccessible).hasSize(16);
    }


     /*
         Test network for accessibility
         If 4-10 and 1-7 links are blocked and the start point is 0,0,
         the links in the inner square are not accessible as well.

         In this test case links 4-10 and 1-7 are placed in a different municipality
         The algorithm should be able to traverse these links
         and determine the accessibility of the inner square

          5----4-----3
          |    |     |
          | 11-10-9  |
          | |     |  |
          | 6--7--8  |
          |    |     |
          0----1-----2
    */


    @Test
    void getAccessibleRoadSections_ok_withRestrictions_and_accessRoadsOutsideMunicipality() {
        addRestrictions(link1_1_link1);
        addRestrictions(link3_3_link3);
        link1_1_link1.setTag(MUNICIPALITY_CODE, 2);
        link3_3_link3.setTag(MUNICIPALITY_CODE, 2);
        graphHopper = createGhNetwork();
        final Set<IsochroneMatch> notAccessible = getIsochroneMatches();
        // restricted access links 1-7 and 4-10 are outside the municipality. The inner square links are within
        // 6 in total in both directions
        assertThat(notAccessible).hasSize(12);
    }

    private static AccessibilityRequest getAccessibilityRequest() {
        return AccessibilityRequest
                .builder()
                .vehicleProperties(VehicleProperties
                        .builder()
                        .hgvAccessForbidden(true)
                        .axleLoad(10.00)
                        .weight(26.5)
                        .height(2.65)
                        .length(8.23)
                        .width(2.55)
                        .build())
                .municipalityId(1)
                .searchDistanceInMetres(SEARCH_DISTANCE_IN_METRES)
                .startPoint(WGS84_GEOMETRY_FACTORY.createPoint(START_NODE))
                .build();
    }

    @NotNull
    private Set<IsochroneMatch> getIsochroneMatches() {
        GraphHopperAccessibilityMap graphHopperAccessibilityMap = new GraphHopperAccessibilityMap(graphHopper);
        final AccessibilityRequest accessibilityRequestAll = getAccessibilityRequestAll();
        final AccessibilityRequest restrictedAccessRequest = getAccessibilityRequest();
        Set<IsochroneMatch> allAccessible = graphHopperAccessibilityMap.getAccessibleRoadSections(
                accessibilityRequestAll);
        Set<IsochroneMatch> restrictedAccess = graphHopperAccessibilityMap.getAccessibleRoadSections(
                restrictedAccessRequest);
        return Sets.difference(allAccessible, restrictedAccess);
    }

    private static AccessibilityRequest getAccessibilityRequestAll() {
        return AccessibilityRequest
                .builder()
                .municipalityId(1)
                .startPoint(WGS84_GEOMETRY_FACTORY.createPoint(START_NODE))
                .searchDistanceInMetres(SEARCH_DISTANCE_IN_METRES)
                .build();
    }


    private static IndexedNetworkGraphHopper createGhNetwork() {
        return getNetworkGraphHopper();
    }

    private static void addRestrictions(Link link) {
        link.setTag(C19_MAX_HEIGHT, 3.5, false);
        link.setTag(C18_MAX_WIDTH, 2.0, false);
        link.setTag(C17_MAX_LENGTH, 4.0, false);
        link.setTag(C20_MAX_AXLE_LOAD, 3.0, false);
        link.setTag(C7_HGV_ACCESS_FORBIDDEN, true, false);

        link.setTag(C19_MAX_HEIGHT, 3.5, true);
        link.setTag(C18_MAX_WIDTH, 2.0, true);
        link.setTag(C17_MAX_LENGTH, 4.0, true);
        link.setTag(C20_MAX_AXLE_LOAD, 3.0, true);
        link.setTag(C7_HGV_ACCESS_FORBIDDEN, true, true);
    }

    private static IndexedNetworkGraphHopper getNetworkGraphHopper() {
        IndexedNetworkGraphHopper graphHopper = new IndexedNetworkGraphHopper(
                linkList::iterator);
        Path path = Path.of("graphhopper", "test_network_2");
        graphHopper.setStoreOnFlush(false);
        graphHopper.setElevation(false);
        graphHopper.setVehicleEncodedValuesFactory(new CustomVehicleEncodedValuesFactory());
        graphHopper.setVehicleTagParserFactory(new LinkVehicleTagParsersFactory());
        graphHopper.setEncodedValueFactory(new CustomEncodedValuesFactory());
        graphHopper.setTagParserFactory(new LinkTagParserFactory());
        graphHopper.setProfiles(
                VehicleType.CAR.createProfile(RoutingProfile.MOTOR_VEHICLE_CUSTOM.getLabel())
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
        Link link = Link.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(50)
                .reverseSpeedInKilometersPerHour(50)
                .distanceInMeters(distance)
                .geometry(createLineString(coordinates))
                .build();
        link.setTag(MUNICIPALITY_CODE, 1);
        return link;
    }

    private static LineString createLineString(Coordinate... coordinates) {
        return new LineString(new CoordinateArraySequence(coordinates),
                new GeometryFactory(new PrecisionModel()));
    }
}
