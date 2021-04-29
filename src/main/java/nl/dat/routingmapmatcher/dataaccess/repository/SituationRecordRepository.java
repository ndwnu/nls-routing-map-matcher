package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.dat.routingmapmatcher.constants.GlobalConstants;
import nl.dat.routingmapmatcher.dataaccess.dao.SituationRecordDao;
import nl.dat.routingmapmatcher.dataaccess.dto.SituationRecordLocationDto;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;
import nl.dat.routingmapmatcher.linestring.ReliabilityCalculationType;

public class SituationRecordRepository implements LineStringLocationRepository {

  private static final Logger logger = LoggerFactory.getLogger(SituationRecordRepository.class);

  private final Jdbi jdbi;

  public SituationRecordRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<LineStringLocation> getLocations() {
    final List<Integer> singlePoints = new ArrayList<>();
    final List<LineStringLocation> situationRecordsOrdered = getSituationRecordOrderedLines(singlePoints);
    final List<LineStringLocation> situationRecordsUnordered = getSituationRecordUnorderedLinears(singlePoints);
    final List<LineStringLocation> situationRecords = new ArrayList<>();
    situationRecords.addAll(situationRecordsOrdered);
    situationRecords.addAll(situationRecordsUnordered);
    return situationRecords;
  }

  public List<LineStringLocation> getSituationRecordOrderedLines(final List<Integer> singlePoints) {
    try (Handle handle = jdbi.open()) {
      final SituationRecordDao situationRecordDao = handle.attach(SituationRecordDao.class);
      final Iterator<SituationRecordLocationDto> iterator = situationRecordDao.getSituationRecordOrderedLocations();
      final List<LineStringLocation> lineStringLocations = new ArrayList<>();
      if (iterator.hasNext()) {
        SituationRecordLocationDto locationDto = iterator.next();
        int situationRecordId = locationDto.getSituationRecordId();
        int locationIndex = locationDto.getLocationIndex();
        List<Point> pointLocations = new ArrayList<>();
        addPointLocations(locationDto, pointLocations);
        while (iterator.hasNext()) {
          locationDto = iterator.next();
          if (locationDto.getSituationRecordId() == situationRecordId) {
            addPointLocations(locationDto, pointLocations);
            locationIndex = locationDto.getLocationIndex();
          } else {
            addLineStringLocation(situationRecordId, locationIndex, pointLocations, lineStringLocations,
                singlePoints);
            situationRecordId = locationDto.getSituationRecordId();
            locationIndex = locationDto.getLocationIndex();
            pointLocations = new ArrayList<>();
            addPointLocations(locationDto, pointLocations);
          }
        }
        addLineStringLocation(situationRecordId, locationIndex, pointLocations, lineStringLocations,
            singlePoints);
      }
      return lineStringLocations;
    }
  }

  public List<LineStringLocation> getSituationRecordUnorderedLinears(final List<Integer> singlePoints) {
    try (Handle handle = jdbi.open()) {
      final SituationRecordDao situationRecordDao = handle.attach(SituationRecordDao.class);
      final Iterator<SituationRecordLocationDto> iterator = situationRecordDao.getSituationRecordUnorderedLinears();
      final List<LineStringLocation> lineStringLocations = new ArrayList<>();
      while (iterator.hasNext()) {
        final List<Point> pointLocations = new ArrayList<>();
        final SituationRecordLocationDto locationDto = iterator.next();
        addPointLocations(locationDto, pointLocations);
        addLineStringLocation(locationDto.getSituationRecordId(), locationDto.getLocationIndex(),
            pointLocations, lineStringLocations, singlePoints);
      }
      logger.info("Note: the following situation record ids contain single point locations and should be "
          + "matched with the PostgreSQL script for situation record points: {}", singlePoints);
      return lineStringLocations;
    }
  }

  @Override
  public void replaceMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final SituationRecordDao situationRecordDao = handle.attach(SituationRecordDao.class);
      situationRecordDao.createSituationRecordLineMatchesTableIfNotExists();
      situationRecordDao.truncateSituationRecordLineMatchesTable();
      situationRecordDao.insertSituationRecordLineMatches(lineStringMatches);
    });
  }

  private void addPointLocations(final SituationRecordLocationDto locationDto, final List<Point> pointLocations) {
    if (locationDto.getLocationType().equals("point")) {
      final Point point = locationDto.getLocationForDisplay();
      if (!isIdenticalToLastPoint(point, pointLocations)) {
        pointLocations.add(point);
      } else {
        logger.debug("Location for display identical to last point: situation record id {} and location index {}",
            locationDto.getSituationRecordId(), locationDto.getLocationIndex());
      }
    } else if (locationDto.getLocationType().equals("linear")) {
      final Point startPoint = locationDto.getStartPoint();
      if (!isIdenticalToLastPoint(startPoint, pointLocations)) {
        pointLocations.add(startPoint);
      } else {
        logger.debug("Start point identical to last point: situation record id {} and location index {}",
            locationDto.getSituationRecordId(), locationDto.getLocationIndex());
      }
      final Point endPoint = locationDto.getEndPoint();
      if (!isIdenticalToLastPoint(endPoint, pointLocations)) {
        pointLocations.add(endPoint);
      } else {
        logger.debug("Identical start and end points: situation record id {} and location index {}",
            locationDto.getSituationRecordId(), locationDto.getLocationIndex());
      }
    }
  }

  private boolean isIdenticalToLastPoint(final Point point, final List<Point> pointLocations) {
    boolean isIdentical;
    if (pointLocations.isEmpty()) {
      isIdentical = false;
    } else {
      final Point lastPoint = pointLocations.get(pointLocations.size() - 1);
      isIdentical = point.getCoordinateSequence().getX(0) == lastPoint.getCoordinateSequence().getX(0) &&
          point.getCoordinateSequence().getY(0) == lastPoint.getCoordinateSequence().getY(0);
    }
    return isIdentical;
  }

  private void addLineStringLocation(final int situationRecordId, final int locationIndex,
      final List<Point> pointLocations, final List<LineStringLocation> lineStringLocations,
      final List<Integer> singlePoints) {
    if (pointLocations.size() > 1) {
      Optional<Integer> locationIndexOptional;
      if (locationIndex == 0) {
        locationIndexOptional = Optional.of(0);
      } else {
        // location index = -1 for linestring consisting of multiple locations
        // (needed because location_index is part of primary key of matches table)
        locationIndexOptional = Optional.of(-1);
      }
      final LineStringLocation lineStringLocation = new LineStringLocation(situationRecordId,
          locationIndexOptional, Optional.empty(), 0.0, createLineStringFromPoints(pointLocations),
          ReliabilityCalculationType.POINT_OBSERVATIONS);
      lineStringLocations.add(lineStringLocation);
    } else {
      // These situation records should be matched with the SQL script for point matching
      singlePoints.add(situationRecordId);
      logger.info("Single point location: situation record id {} and location index {}",
          situationRecordId, locationIndex);
    }
  }

  private LineString createLineStringFromPoints(final List<Point> pointLocations) {
    final List<Coordinate> coordinates = new ArrayList<>();
    for (final Point point : pointLocations) {
      coordinates.add(point.getCoordinate());
    }
    return new LineString(coordinates.toArray(new Coordinate[0]), new PrecisionModel(), GlobalConstants.WGS84_SRID);
  }
}
