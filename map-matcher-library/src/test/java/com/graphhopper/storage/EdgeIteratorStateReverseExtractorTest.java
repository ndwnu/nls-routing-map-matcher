package com.graphhopper.storage;

import static com.graphhopper.storage.EdgeIteratorStateReverseExtractor.hasReversed;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.MatchUtil.getQueryResults;
import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

class EdgeIteratorStateReverseExtractorTest {

    private static final double LONG_1 = 5.358247;
    private static final double LAT_1 = 52.161257;
    private static final double LONG_2 = 5.379687;
    private static final double LAT_2 = 52.158304;
    private static final double LONG_3 = 5.379667;
    private static final double LAT_3 = 52.158280;
    private static final double LONG_4 = 5.3783483;
    private static final double LAT_4 = 52.1590774;
    private static final long FROM_NODE_ID = 1;
    private static final long TO_NODE_ID = 2;
    private static final Coordinate coordinateA1 = new Coordinate(LONG_1, LAT_1);
    private static final Coordinate coordinateA2 = new Coordinate(LONG_2, LAT_2);
    private static final Coordinate coordinateA3 = new Coordinate(LONG_3, LAT_3);
    private static final Coordinate coordinateA4 = new Coordinate(LONG_4, LAT_4);
    private static final int TO_NODE_ID_2 = 3;
    private static final int ID_1 = 1;
    private static final int ID_2 = 2;
    private static final int ALL_NODES = 3;
    private List<QueryResult> queryResults;
    private NetworkGraphHopper network;
    private LineString lineString_1;

    @BeforeEach
    void setup() {
        var geometryFactory = new GeometryFactory(new PrecisionModel(),
                GlobalConstants.WGS84_SRID);
        lineString_1 = geometryFactory
                .createLineString(new Coordinate[]{coordinateA1, coordinateA2, coordinateA3});
        var link = Link.builder()
                .id(ID_1)
                .fromNodeId(FROM_NODE_ID)
                .toNodeId(TO_NODE_ID)
                .speedInKilometersPerHour(50)
                .reverseSpeedInKilometersPerHour(50)
                .distanceInMeters(1500)
                .geometry(lineString_1)
                .build();
        LineString lineString_2 = geometryFactory
                .createLineString(new Coordinate[]{coordinateA3, coordinateA4});
        var link2 = Link.builder()
                .id(ID_2)
                .fromNodeId(TO_NODE_ID)
                .toNodeId(TO_NODE_ID_2)
                .speedInKilometersPerHour(50)
                .reverseSpeedInKilometersPerHour(50)
                .distanceInMeters(500)
                .geometry(lineString_2)
                .build();
        Supplier<Iterator<Link>> linkSupplier = () -> List.of(link, link2).iterator();
        var routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test-1")
                .linkSupplier(linkSupplier)
                .build();
        var networkGraphHopperFactory = new NetworkGraphHopperFactory();
        network = networkGraphHopperFactory.createNetwork(routingNetwork);
        var locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        var inputPoint = geometryFactory.createPoint(new Coordinate(5.3789037, 52.1588714));
        queryResults = getQueryResults(inputPoint, 100, locationIndexTree, EdgeFilter.ALL_EDGES);
    }

    @Test
    void hasReversed_when_reversed_isTrue_geometry_should_be_reversed() {
        var reversedQueryResult = queryResults.get(1);
        var reversedEdge = reversedQueryResult.getClosestEdge();
        var flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        var flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        var flags = reversedEdge.getFlags();
        int matchedLinkId = flagEncoder.getId(flags);
        assertThat(matchedLinkId).isEqualTo(ID_1);
        assertThat(hasReversed(reversedQueryResult)).isTrue();
        assertThat(lineString_1.reverse())
                .isEqualTo(reversedEdge.fetchWayGeometry(ALL_NODES).toLineString(false));
    }
}
