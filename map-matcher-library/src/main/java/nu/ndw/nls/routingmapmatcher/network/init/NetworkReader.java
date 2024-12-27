package nu.ndw.nls.routingmapmatcher.network.init;

import com.graphhopper.coll.LongLongMap;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public class NetworkReader {

    private static final int COORDINATES_LENGTH_START_END = 2;
    private static final double BOUND_EXPAND = .000001;

    private final Supplier<Iterator<? extends Link>> linkSupplier;
    private final LongLongMap nodeIdToInternalNodeIdMap;
    private final EdgeIntAccess edgeIntAccess;
    private final List<TagParser> wayTagParsers;

    protected final BaseGraph baseGraph;
    private final Map<Long, Integer> edgeMap;
    private final boolean expandBounds;

    public NetworkReader(BaseGraph baseGraph, Supplier<Iterator<? extends Link>> linkSupplier,
            List<TagParser> wayTagParsers, LongLongMap nodeIdToInternalNodeIdMap, Map<Long, Integer> edgeMap,
            boolean expandBounds) {
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = Objects.requireNonNull(nodeIdToInternalNodeIdMap);
        this.wayTagParsers = Objects.requireNonNull(wayTagParsers);
        this.baseGraph = Objects.requireNonNull(baseGraph);
        this.edgeIntAccess = baseGraph.createEdgeIntAccess();
        this.edgeMap = edgeMap;
        this.expandBounds = expandBounds;
    }

    public void readGraph() {
        log.info("Start reading links");
        Iterator<? extends Link> links = linkSupplier.get();
        readLinks(links);
        log.info("Finished reading links");
    }

    private void readLinks(Iterator<? extends Link> links) {
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

    private void addLink(Link link) {
        Coordinate[] coordinates = link.getGeometry().getCoordinates();
        if (coordinates.length < COORDINATES_LENGTH_START_END) {
            throw new IllegalStateException("Invalid geometry");
        }
        if (link.getFromNodeId() == link.getToNodeId()) {
            log.debug("GraphHopper >= 8.0 does not support loop edges, skipping link ID " + link.getId());
            return;
        }
        if(expandBounds) {
            Arrays.stream(coordinates).forEach(coord -> {
                baseGraph.getBounds().update(coord.y + BOUND_EXPAND, coord.x + BOUND_EXPAND);
                baseGraph.getBounds().update(coord.y - BOUND_EXPAND, coord.x - BOUND_EXPAND);
            });
        }
        int internalFromNodeId = addNodeIfNeeded(link.getFromNodeId(), coordinates[0].y, coordinates[0].x);
        int internalToNodeId = addNodeIfNeeded(link.getToNodeId(), coordinates[coordinates.length - 1].y,
                coordinates[coordinates.length - 1].x);

        EdgeIteratorState edge = baseGraph.edge(internalFromNodeId, internalToNodeId)
                .setDistance(link.getDistanceInMeters());
        wayTagParsers.forEach(tagParser -> tagParser.handleWayTags(edge.getEdge(), edgeIntAccess, link, IntsRef.EMPTY));
        if (coordinates.length > COORDINATES_LENGTH_START_END) {
            PointList geometry = createPointListWithoutStartAndEndPoint(coordinates);
            edge.setWayGeometry(geometry);
        }

        edgeMap.put(link.getId(), edge.getEdgeKey());
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
