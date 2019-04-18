package nl.dat.routingmapmatcher.graphhopper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import com.vividsolutions.jts.geom.Coordinate;

import nl.dat.routingmapmatcher.dataaccess.dao.NdwLinkDao;
import nl.dat.routingmapmatcher.dataaccess.dto.NdwLinkDto;
import nl.dat.routingmapmatcher.enums.NdwNetworkSubset;
import nl.dat.routingmapmatcher.exceptions.RoutingMapMatcherException;

class NdwNetworkReader implements DataReader {

  private static final Logger logger = LoggerFactory.getLogger(NdwNetworkReader.class);

  private final GraphHopperStorage ghStorage;
  private final Handle handle;
  private final NdwNetworkSubset subset;
  private final List<NdwLinkProperties> linkProperties;
  private final EncodingManager encodingManager;
  private final DistanceCalc distanceCalculator;

  private LongIntMap nodeIdToInternalNodeIdMap;

  public NdwNetworkReader(final GraphHopperStorage ghStorage, final Handle handle,
      final NdwNetworkSubset subset, final List<NdwLinkProperties> linkProperties) {
    this.ghStorage = Preconditions.checkNotNull(ghStorage);
    this.handle = Preconditions.checkNotNull(handle);
    this.subset = Preconditions.checkNotNull(subset);
    this.linkProperties = Preconditions.checkNotNull(linkProperties);
    this.encodingManager = Preconditions.checkNotNull(ghStorage.getEncodingManager());
    this.distanceCalculator = GraphHopperConstants.distanceCalculation;
    this.nodeIdToInternalNodeIdMap = new GHLongIntBTree(200);
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
    logger.info("Start reading NDW links");
    ghStorage.create(1000);

    final NdwLinkDao ndwLinkDao = handle.attach(NdwLinkDao.class);
    readNdwLinks(ndwLinkDao);

    logger.info("Finished reading NDW links");
    finishedReading();
  }

  private void readNdwLinks(final NdwLinkDao ndwLinkDao) {
    int count = 0;
    final Iterator<NdwLinkDto> ndwLinks = ndwLinkDao.getNdwLinksIterator(subset);
    while (ndwLinks.hasNext()) {
      final NdwLinkDto ndwLink = ndwLinks.next();
      ndwLink.setIndex(count);
      addNdwLink(ndwLink);
      count++;
      logCount(count);
    }
  }

  private void addNdwLink(final NdwLinkDto ndwLink) {
    final Coordinate[] coordinates = ndwLink.getGeometry().getCoordinates();
    if (coordinates.length < 2) {
      throw new IllegalStateException("Invalid geometry");
    }
    final int internalFromNodeId = addNodeIfNeeded(ndwLink.getFromNodeId(), coordinates[0].y,
        coordinates[0].x);
    final int internalToNodeId = addNodeIfNeeded(ndwLink.getToNodeId(), coordinates[coordinates.length - 1].y,
        coordinates[coordinates.length - 1].x);
    final long wayFlags = determineWayFlags(ndwLink);
    final EdgeIteratorState edge = ghStorage.edge(internalFromNodeId, internalToNodeId)
        .setDistance(calculateLengthInMeters(coordinates))
        .setFlags(wayFlags);
    if (coordinates.length > 2) {
      final PointList geometry = createPointListWithoutStartAndEndPoint(coordinates);
      edge.setWayGeometry(geometry);
    }
    if (linkProperties.size() != ndwLink.getIndex()) {
      throw new RoutingMapMatcherException("Index of NDW link does not match index in NDW link properties");
    }
    linkProperties.add(new NdwLinkProperties(ndwLink.getForwardId(), ndwLink.getBackwardId()));
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

  private long determineWayFlags(final NdwLinkDto ndwLink) {
    final long includeWay = encodingManager.acceptWay(ndwLink);
    if (includeWay == 0) {
      return 0;
    }

    final long relationFlags = 0;
    return encodingManager.handleWayTags(ndwLink, includeWay, relationFlags);
  }

  private double calculateLengthInMeters(final Coordinate[] coordinates) {
    double lengthInMeters = 0.0;
    for (int index = 1; index < coordinates.length; index++) {
      lengthInMeters += distanceCalculator.calcDist(coordinates[index - 1].y, coordinates[index - 1].x,
          coordinates[index].y, coordinates[index].x);
    }
    return lengthInMeters;
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
    boolean log = count == 1_000;
    log = log || count <= 10_000 && count % 5_000 == 0;
    log = log || count <= 100_000 && count % 50_000 == 0;
    log = log || count % 500_000 == 0;
    if (log) {
      logger.debug("Read {} links", count);
    }
  }

  private void finishedReading() {
    nodeIdToInternalNodeIdMap = null;
  }

  @Override
  public Date getDataDate() {
    // Don't return a date
    return null;
  }

}
