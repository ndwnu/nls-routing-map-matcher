package nu.ndw.nls.routingmapmatcher.graphhopper;


import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.WAY_ID;

import com.google.common.base.Preconditions;
import com.graphhopper.coll.LongLongMap;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public class NetworkReader {

    private static final int COORDINATES_LENGTH_START_END = 2;

    private final Supplier<Iterator<Link>> linkSupplier;
    private final LongLongMap nodeIdToInternalNodeIdMap;
    private final EdgeIntAccess edgeIntAccess;
    private final List<TagParser> vehicleTagParsers;
    private final IntEncodedValue idEncoder;

    protected final BaseGraph baseGraph;

    public NetworkReader(BaseGraph baseGraph, EncodingManager encodingManager, Supplier<Iterator<Link>> linkSupplier,
            List<TagParser> vehicleTagParsers, LongLongMap nodeIdToInternalNodeIdMap) {
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = Preconditions.checkNotNull(nodeIdToInternalNodeIdMap);
        this.vehicleTagParsers = Preconditions.checkNotNull(vehicleTagParsers);
        this.baseGraph = Preconditions.checkNotNull(baseGraph);
        this.edgeIntAccess = baseGraph.createEdgeIntAccess();
        this.idEncoder = encodingManager.getIntEncodedValue(WAY_ID.getKey());
    }

    public void readGraph() {
        log.info("Start reading links");
        Iterator<Link> links = linkSupplier.get();
        readLinks(links);
        log.info("Finished reading links");
    }

    public static Link castToLink(ReaderWay way) {
        Link link = (Link) way;
        if (link == null) {
            throw new IllegalStateException("Only Link Dto's are supported by this parser");
        }
        return link;
    }

    public static WayAccess getAccess(ReaderWay way) {
        Link link = castToLink(way);
        boolean access = link.getSpeedInKilometersPerHour() > 0.0 || link.getReverseSpeedInKilometersPerHour() > 0.0;
        return access ? WayAccess.WAY : WayAccess.CAN_SKIP;
    }

    private void readLinks(Iterator<Link> links) {
        int count = 0;
        while (links.hasNext()) {
            Link link = links.next();
            try {
                addLink(link);
                count++;
                logCount(count);
            } catch (Exception exception) {
                log.error("Error adding link {}", link);
                throw new IllegalStateException(exception);
            }
        }
    }

    protected OptionalInt addLink(Link link) {
        Coordinate[] coordinates = link.getGeometry().getCoordinates();
        if (coordinates.length < COORDINATES_LENGTH_START_END) {
            throw new IllegalStateException("Invalid geometry");
        }
        if (link.getFromNodeId() == link.getToNodeId()) {
            log.debug("GraphHopper >= 8.0 does not support loop edges, skipping link ID " + link.getId());
            return OptionalInt.empty();
        }
        int internalFromNodeId = addNodeIfNeeded(link.getFromNodeId(), coordinates[0].y, coordinates[0].x);
        int internalToNodeId = addNodeIfNeeded(link.getToNodeId(), coordinates[coordinates.length - 1].y,
                coordinates[coordinates.length - 1].x);

        EdgeIteratorState edge = baseGraph.edge(internalFromNodeId, internalToNodeId)
                .setDistance(link.getDistanceInMeters());
        setWayFlags(link, edge.getEdge());
        if (coordinates.length > COORDINATES_LENGTH_START_END) {
            PointList geometry = createPointListWithoutStartAndEndPoint(coordinates);
            edge.setWayGeometry(geometry);
        }
        return OptionalInt.of(edge.getEdgeKey());
    }

    private int addNodeIfNeeded(long id, double latitude, double longitude) {
        int internalNodeId = Math.toIntExact(nodeIdToInternalNodeIdMap.get(id));
        if (internalNodeId < 0) {
            internalNodeId = Math.toIntExact(nodeIdToInternalNodeIdMap.getSize());
            nodeIdToInternalNodeIdMap.put(id, internalNodeId);
            baseGraph.getNodeAccess().setNode(internalNodeId, latitude, longitude);
        }
        return internalNodeId;
    }

    private void setWayFlags(Link link, int edgeId) {
        if (getAccess(link).canSkip()) {
            log.warn("link {} is inaccessible and will be ignored in the network", link);
            return;
        }
        vehicleTagParsers.forEach(tagParser -> tagParser.handleWayTags(edgeId, edgeIntAccess, link, IntsRef.EMPTY));
        idEncoder.setInt(false, edgeId, edgeIntAccess, Math.toIntExact(link.getId()));
    }

    private PointList createPointListWithoutStartAndEndPoint(Coordinate[] coordinates) {
        boolean is3d = false;
        PointList pointList = new PointList(coordinates.length - COORDINATES_LENGTH_START_END, is3d);
        for (int index = 1; index < coordinates.length - 1; index++) {
            pointList.add(coordinates[index].y, coordinates[index].x);
        }
        return pointList;
    }

    @SuppressWarnings("squid:S109")
    private void logCount(int count) {
        boolean shouldLog = count <= 10 && count % 5 == 0;
        shouldLog = shouldLog || (count <= 100 && count % 50 == 0);
        shouldLog = shouldLog || (count <= 1_000 && count % 500 == 0);
        shouldLog = shouldLog || (count <= 10_000 && count % 5_000 == 0);
        shouldLog = shouldLog || (count <= 100_000 && count % 50_000 == 0);
        shouldLog = shouldLog || count % 500_000 == 0;
        if (shouldLog) {
            log.debug("Read {} links", count);
        }
    }
}
