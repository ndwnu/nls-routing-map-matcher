package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.google.common.base.Preconditions;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
class NetworkReader implements DataReader {

    private static final int STORAGE_BYTE_COUNT = 1000;
    private static final int COORDINATES_LENGTH_FOR_START_AND_END_ONLY = 2;

    private final GraphHopperStorage ghStorage;
    private final Supplier<Iterator<Link>> linkSupplier;
    private final LongIntMap nodeIdToInternalNodeIdMap;
    private final EncodingManager encodingManager;

    public NetworkReader(GraphHopperStorage ghStorage, Supplier<Iterator<Link>> linkSupplier,
            LongIntMap nodeIdToInternalNodeIdMap) {
        this.ghStorage = Preconditions.checkNotNull(ghStorage);
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = Preconditions.checkNotNull(nodeIdToInternalNodeIdMap);
        this.encodingManager = Preconditions.checkNotNull(ghStorage.getEncodingManager());
    }

    @Override
    public DataReader setFile(File file) {
        // Ignore this property, get the link from the database instead
        return this;
    }

    @Override
    public DataReader setElevationProvider(ElevationProvider elevationProvider) {
        // Ignore this property, don't use elevation
        return this;
    }

    @Override
    public DataReader setSmoothElevation(boolean smoothElevation) {
        // Ignore this property, don't use elevation
        return this;
    }

    @Override
    public DataReader setWorkerThreads(int workerThreads) {
        // Ignore this property, always use one thread
        return this;
    }

    @Override
    public DataReader setWayPointMaxDistance(double wayPointMaxDistance) {
        // Ignore this property, don't simplify geometries
        return this;
    }

    @Override
    public void readGraph() {
        log.info("Start reading links");
        ghStorage.create(STORAGE_BYTE_COUNT);
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
        if (coordinates.length < COORDINATES_LENGTH_FOR_START_AND_END_ONLY) {
            throw new IllegalStateException("Invalid geometry");
        }
        int internalFromNodeId = addNodeIfNeeded(link.getFromNodeId(), coordinates[0].y, coordinates[0].x);
        int internalToNodeId = addNodeIfNeeded(link.getToNodeId(), coordinates[coordinates.length - 1].y,
                coordinates[coordinates.length - 1].x);
        IntsRef wayFlags = determineWayFlags(link);
        EdgeIteratorState edge = ghStorage.edge(internalFromNodeId, internalToNodeId)
                .setDistance(link.getDistanceInMeters())
                .setFlags(wayFlags);
        if (coordinates.length > COORDINATES_LENGTH_FOR_START_AND_END_ONLY) {
            PointList geometry = createPointListWithoutStartAndEndPoint(coordinates);
            edge.setWayGeometry(geometry);
        }
    }

    private int addNodeIfNeeded(long id, double latitude, double longitude) {
        int internalNodeId = nodeIdToInternalNodeIdMap.get(id);
        if (internalNodeId < 0) {
            internalNodeId = Math.toIntExact(nodeIdToInternalNodeIdMap.getSize());
            nodeIdToInternalNodeIdMap.put(id, internalNodeId);
            ghStorage.getNodeAccess().setNode(internalNodeId, latitude, longitude);
        }
        return internalNodeId;
    }

    private IntsRef determineWayFlags(Link link) {
        EncodingManager.AcceptWay acceptWay = new EncodingManager.AcceptWay();
        boolean includeWay = encodingManager.acceptWay(link, acceptWay);
        if (!includeWay) {
            return IntsRef.EMPTY;
        }

        long relationFlags = 0;
        return encodingManager.handleWayTags(link, acceptWay, relationFlags);
    }

    private PointList createPointListWithoutStartAndEndPoint(Coordinate[] coordinates) {
        boolean is3d = false;
        PointList pointList = new PointList(coordinates.length - 2, is3d);
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

    @Override
    public Date getDataDate() {
        // Don't return a date
        return null;
    }
}
