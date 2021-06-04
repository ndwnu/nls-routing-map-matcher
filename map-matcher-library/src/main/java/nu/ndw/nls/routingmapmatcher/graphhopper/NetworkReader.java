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
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import org.locationtech.jts.geom.Coordinate;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Supplier;

@Slf4j
class NetworkReader implements DataReader {

    private static final int STORAGE_BYTE_COUNT = 1000;
    private static final int COORDINATES_LENGTH_FOR_START_AND_END_ONLY = 2;

    private final GraphHopperStorage ghStorage;
    private final Supplier<Iterator<Link>> linkSupplier;
    private final LongIntMap nodeIdToInternalNodeIdMap;
    private final EncodingManager encodingManager;

    public NetworkReader(final GraphHopperStorage ghStorage,
                         final Supplier<Iterator<Link>> linkSupplier,
                         final LongIntMap nodeIdToInternalNodeIdMap) {
        this.ghStorage = Preconditions.checkNotNull(ghStorage);
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = Preconditions.checkNotNull(nodeIdToInternalNodeIdMap);
        this.encodingManager = Preconditions.checkNotNull(ghStorage.getEncodingManager());
    }

    @Override
    public DataReader setFile(final File file) {
        // Ignore this property, get the link from the database instead
        return this;
    }

    @Override
    public DataReader setElevationProvider(final ElevationProvider elevationProvider) {
        // Ignore this property, don't use elevation
        return this;
    }

    @Override
    public DataReader setSmoothElevation(final boolean smoothElevation) {
        // Ignore this property, don't use elevation
        return this;
    }

    @Override
    public DataReader setWorkerThreads(final int workerThreads) {
        // Ignore this property, always use one thread
        return this;
    }

    @Override
    public DataReader setWayPointMaxDistance(final double wayPointMaxDistance) {
        // Ignore this property, don't simplify geometries
        return this;
    }

    @Override
    public void readGraph() {
        log.info("Start reading links");
        ghStorage.create(STORAGE_BYTE_COUNT);
        final Iterator<Link> links = linkSupplier.get();
        readLinks(links);
        log.info("Finished reading links");
    }

    private void readLinks(final Iterator<Link> links) {
        int count = 0;
        while (links.hasNext()) {
            final Link link = links.next();
            addLink(link);
            count++;
            logCount(count);
        }
    }

    private void addLink(final Link link) {
        final Coordinate[] coordinates = link.getGeometry().getCoordinates();
        if (coordinates.length < COORDINATES_LENGTH_FOR_START_AND_END_ONLY) {
            throw new IllegalStateException("Invalid geometry");
        }
        final int internalFromNodeId = addNodeIfNeeded(link.getFromNodeId(), coordinates[0].y,
                coordinates[0].x);
        final int internalToNodeId = addNodeIfNeeded(link.getToNodeId(), coordinates[coordinates.length - 1].y,
                coordinates[coordinates.length - 1].x);
        final IntsRef wayFlags = determineWayFlags(link);
        final EdgeIteratorState edge = ghStorage.edge(internalFromNodeId, internalToNodeId)
                .setDistance(link.getDistanceInMeters())
                .setFlags(wayFlags);
        if (coordinates.length > COORDINATES_LENGTH_FOR_START_AND_END_ONLY) {
            final PointList geometry = createPointListWithoutStartAndEndPoint(coordinates);
            edge.setWayGeometry(geometry);
        }
    }

    private int addNodeIfNeeded(final long id, final double latitude, final double longitude) {
        int internalNodeId = nodeIdToInternalNodeIdMap.get(id);
        if (internalNodeId < 0) {
            internalNodeId = Math.toIntExact(nodeIdToInternalNodeIdMap.getSize());
            nodeIdToInternalNodeIdMap.put(id, internalNodeId);
            ghStorage.getNodeAccess().setNode(internalNodeId, latitude, longitude);
        }
        return internalNodeId;
    }

    private IntsRef determineWayFlags(final Link link) {
        final EncodingManager.AcceptWay acceptWay = new EncodingManager.AcceptWay();
        final boolean includeWay = encodingManager.acceptWay(link, acceptWay);
        if (!includeWay) {
            return IntsRef.EMPTY;
        }

        final long relationFlags = 0;
        return encodingManager.handleWayTags(link, acceptWay, relationFlags);
    }

    private PointList createPointListWithoutStartAndEndPoint(final Coordinate[] coordinates) {
        final boolean is3d = false;
        final PointList pointList = new PointList(coordinates.length - 2, is3d);
        for (int index = 1; index < coordinates.length - 1; index++) {
            pointList.add(coordinates[index].y, coordinates[index].x);
        }
        return pointList;
    }

    @SuppressWarnings("squid:S109")
    private void logCount(final int count) {
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