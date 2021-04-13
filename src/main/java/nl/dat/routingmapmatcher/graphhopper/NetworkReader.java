package nl.dat.routingmapmatcher.graphhopper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.jdbi.v3.core.result.ResultIterator;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

import nl.dat.routingmapmatcher.dataaccess.dao.LinkDao;
import nl.dat.routingmapmatcher.dataaccess.dto.LinkDto;

class NetworkReader implements DataReader {

  private static final Logger logger = LoggerFactory.getLogger(NetworkReader.class);

  private final GraphHopperStorage ghStorage;
  private final LinkDao linkDao;
  private final LongIntMap nodeIdToInternalNodeIdMap;
  private final EncodingManager encodingManager;

  public NetworkReader(final GraphHopperStorage ghStorage, final LinkDao linkDao,
      final LongIntMap nodeIdToInternalNodeIdMap) {
    this.ghStorage = Preconditions.checkNotNull(ghStorage);
    this.linkDao = Preconditions.checkNotNull(linkDao);
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
  public void readGraph() throws IOException {
    logger.info("Start reading links");
    ghStorage.create(1000);

    try (final ResultIterator<LinkDto> links = linkDao.getLinksIterator()) {
      readLinks(links);
    }

    logger.info("Finished reading links");
  }

  private void readLinks(final Iterator<LinkDto> links) {
    int count = 0;
    while (links.hasNext()) {
      final LinkDto link = links.next();
      addLink(link);
      count++;
      logCount(count);
    }
  }

  private void addLink(final LinkDto link) {
    final Coordinate[] coordinates = link.getGeometry().getCoordinates();
    if (coordinates.length < 2) {
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
    if (coordinates.length > 2) {
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

  private IntsRef determineWayFlags(final LinkDto link) {
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

  private void logCount(final int count) {
    boolean log = count <= 10 && count % 5 == 0;
    log = log || count <= 100 && count % 50 == 0;
    log = log || count <= 1_000 && count % 500 == 0;
    log = log || count <= 10_000 && count % 5_000 == 0;
    log = log || count <= 100_000 && count % 50_000 == 0;
    log = log || count % 500_000 == 0;
    if (log) {
      logger.debug("Read {} links", count);
    }
  }

  @Override
  public Date getDataDate() {
    // Don't return a date
    return null;
  }
}
