package nu.ndw.nls.routingmapmatcher.graphhopper;



import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkCarVehicleTagParsersFactory.castToLink;

import com.google.common.base.Preconditions;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.ReaderWay;
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
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
class NetworkReader {

    private static final int COORDINATES_LENGTH_START_END = 2;

    private final Supplier<Iterator<Link>> linkSupplier;
    private final LongIntMap nodeIdToInternalNodeIdMap;
    private final EncodingManager encodingManager;
    private final List<TagParser> vehicleTagParsers;
    private final IntEncodedValue idEncoder;

    private final BaseGraph baseGraph;

    public NetworkReader(BaseGraph baseGraph, EncodingManager encodingManager, Supplier<Iterator<Link>> linkSupplier,
            List<TagParser> vehicleTagParsers,
            LongIntMap nodeIdToInternalNodeIdMap) {
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = Preconditions.checkNotNull(nodeIdToInternalNodeIdMap);
        this.encodingManager = Preconditions.checkNotNull(encodingManager);
        this.vehicleTagParsers = Preconditions.checkNotNull(vehicleTagParsers);
        this.baseGraph = Preconditions.checkNotNull(baseGraph);
        this.idEncoder = encodingManager.getIntEncodedValue(LinkWayIdEncodedValuesFactory.ID_NAME);
    }

    public void readGraph() {
        log.info("Start reading links");
        Iterator<Link> links = linkSupplier.get();
        readLinks(links);
        log.info("Finished reading links");
    }

    private void readLinks(Iterator<Link> links) {
        int count = 0;
        while (links.hasNext()) {
            Link link = links.next();
            addLink(link);
            count++;
            logCount(count);
        }
    }

    private void addLink(Link link) {
        Coordinate[] coordinates = link.getGeometry().getCoordinates();
        if (coordinates.length < COORDINATES_LENGTH_START_END) {
            throw new IllegalStateException("Invalid geometry");
        }
        int internalFromNodeId = addNodeIfNeeded(link.getFromNodeId(), coordinates[0].y, coordinates[0].x);
        int internalToNodeId = addNodeIfNeeded(link.getToNodeId(), coordinates[coordinates.length - 1].y,
                coordinates[coordinates.length - 1].x);

        IntsRef wayFlags = determineWayFlags(link);
        idEncoder.setInt(false, wayFlags, Math.toIntExact(link.getId()));
        EdgeIteratorState edge = baseGraph.edge(internalFromNodeId, internalToNodeId)
                .setDistance(link.getDistanceInMeters())
                .setFlags(wayFlags);
        if (coordinates.length > COORDINATES_LENGTH_START_END) {
            PointList geometry = createPointListWithoutStartAndEndPoint(coordinates);
            edge.setWayGeometry(geometry);
        }
    }

    private int addNodeIfNeeded(long id, double latitude, double longitude) {
        int internalNodeId = nodeIdToInternalNodeIdMap.get(id);
        if (internalNodeId < 0) {
            internalNodeId = Math.toIntExact(nodeIdToInternalNodeIdMap.getSize());
            nodeIdToInternalNodeIdMap.put(id, internalNodeId);
            baseGraph.getNodeAccess().setNode(internalNodeId, latitude, longitude);
        }
        return internalNodeId;
    }

    private WayAccess getAccess(ReaderWay way) {
        Link link = castToLink(way);
        boolean access = link.getSpeedInKilometersPerHour() > 0.0 || link.getReverseSpeedInKilometersPerHour() > 0.0;
        return access ? WayAccess.WAY : WayAccess.CAN_SKIP;
    }

    private IntsRef determineWayFlags(Link link) {
        if (getAccess(link).canSkip()) {
            return IntsRef.EMPTY;
        }
        IntsRef wayFlags = encodingManager.createEdgeFlags();
        vehicleTagParsers
                .forEach(tagParser -> tagParser.handleWayTags(wayFlags,
                        link, IntsRef.EMPTY));

        return wayFlags;
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
