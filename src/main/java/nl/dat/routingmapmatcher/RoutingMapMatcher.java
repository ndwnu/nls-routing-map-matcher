package nl.dat.routingmapmatcher;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import nl.dat.routingmapmatcher.dataaccess.repository.LmsRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.MstShapefileRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.NetworkRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.NwbRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.SituationRecordRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.StartToEndRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.WazeRepository;
import nl.dat.routingmapmatcher.dataaccess.support.DatabaseConnectionManager;
import nl.dat.routingmapmatcher.enums.NdwNetworkSubset;
import nl.dat.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMapMatcher;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;
import nl.dat.routingmapmatcher.linestring.viterbi.ViterbiLineStringMapMatcher;
import nl.dat.routingmapmatcher.starttoend.StartToEndLocation;
import nl.dat.routingmapmatcher.starttoend.StartToEndMapMatcher;
import nl.dat.routingmapmatcher.starttoend.StartToEndMatch;

public class RoutingMapMatcher {

  private static final Logger logger = LoggerFactory.getLogger(RoutingMapMatcher.class);

  public static void main(final String[] args) {
    try {
      new RoutingMapMatcher().run();
    } catch (final Exception e) {
      logger.error("An exception occurred", e);
    }
  }

  public void run() {
    matchNoFcdStartToEndMeasurementLocations(readNdwNetwork(NdwNetworkSubset.OSM_NO_SMALL_LINKS));
    matchNwb(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
    matchWazeJams(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
    matchWazeIrregularities(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
    matchMstLinesShapefile(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
    matchSituationRecordLines(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
    matchLmsLinks(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
  }

  private NetworkGraphHopper readNdwNetwork(final NdwNetworkSubset subset) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();
    logger.info("Start reading NDW network with subset {}", subset);
    final NetworkRepository ndwNetworkRepository = new NetworkRepository(jdbi);
    return ndwNetworkRepository.getNetwork(subset.getNetworkQuery());
  }

  private void matchNoFcdStartToEndMeasurementLocations(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final StartToEndRepository startToEndRepository = new StartToEndRepository(jdbi);
    final List<StartToEndLocation> startToEndLocations = startToEndRepository.getNoFcdStartToEndMeasurementLocations();

    logger.info("Start map matching for start to end measurement locations (excluding fcd), count = {}",
        startToEndLocations.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final StartToEndMapMatcher startToEndMapMatcher = new StartToEndMapMatcher(ndwNetwork);
    final List<StartToEndMatch> startToEndMatches = new ArrayList<>(startToEndLocations.size());
    for (final StartToEndLocation startToEndLocation : startToEndLocations) {
      startToEndMatches.add(startToEndMapMatcher.match(startToEndLocation));
    }

    logger.info("Write results to database, matching took {} for {} locations", stopwatch, startToEndLocations.size());
    startToEndRepository.replaceMatches(startToEndMatches);

    logger.info("Done");
  }

  private void matchNwb(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final NwbRepository nwbRepository = new NwbRepository(jdbi);
    final List<LineStringLocation> nwbLocations = nwbRepository.getNwbLocations();
    logger.info("Start map matching for NWB national highways, count = {}", nwbLocations.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> nwbMatches = new ArrayList<>(nwbLocations.size());
    for (final LineStringLocation nwbNationalHighwayLocation : nwbLocations) {
      nwbMatches.add(lineStringMapMatcher.match(nwbNationalHighwayLocation));
    }

    logger.info("Write results to database, matching took {} for {} locations", stopwatch, nwbLocations.size());
    nwbRepository.replaceNwbMatches(nwbMatches);

    logger.info("Done");
  }

  private void matchWazeJams(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final WazeRepository wazeRepository = new WazeRepository(jdbi);
    final List<LineStringLocation> wazeJams = wazeRepository.getWazeJams();
    logger.info("Start map matching for waze jams, count = {}", wazeJams.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> wazeJamMatches = new ArrayList<>(wazeJams.size());
    for (final LineStringLocation wazeJam : wazeJams) {
      wazeJamMatches.add(lineStringMapMatcher.match(wazeJam));
    }

    logger.info("Write results to database, matching took {} for {} locations", stopwatch, wazeJams.size());
    wazeRepository.replaceWazeJamsMatches(wazeJamMatches);

    logger.info("Done");
  }

  private void matchWazeIrregularities(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final WazeRepository wazeRepository = new WazeRepository(jdbi);
    final List<LineStringLocation> wazeIrregularities = wazeRepository.getWazeIrregularities();
    logger.info("Start map matching for waze irregularities, count = {}", wazeIrregularities.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> wazeIrregularitiesMatches = new ArrayList<>(wazeIrregularities.size());
    for (final LineStringLocation wazeIrregularity : wazeIrregularities) {
      wazeIrregularitiesMatches.add(lineStringMapMatcher.match(wazeIrregularity));
    }

    logger.info("Write results to database, matching took {} for {} locations", stopwatch, wazeIrregularities.size());
    wazeRepository.replaceWazeIrregularitiesMatches(wazeIrregularitiesMatches);

    logger.info("Done");
  }

  private void matchMstLinesShapefile(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final MstShapefileRepository mstShapefileRepository = new MstShapefileRepository(jdbi);
    final List<LineStringLocation> mstLines = mstShapefileRepository.getMstLinesShapefile();
    logger.info("Start map matching for MST lines, count = {}", mstLines.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> mstLinesMatches = new ArrayList<>(mstLines.size());
    int counter = 1;
    for (final LineStringLocation mstLine : mstLines) {
      mstLinesMatches.add(lineStringMapMatcher.match(mstLine));
      if (counter % 100 == 0) {
        logger.info("Matched {} MST lines of {} total", counter, mstLines.size());
      }
      counter++;
    }
    logger.info("Write results to database, matching took {} for {} locations", stopwatch, mstLines.size());
    mstShapefileRepository.replaceMstLinesShapefileMatches(mstLinesMatches);

    logger.info("Done");
  }

  private void matchSituationRecordLines(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final SituationRecordRepository repository = new SituationRecordRepository(jdbi);
    final List<Integer> singlePoints = new ArrayList<>();
    final List<LineStringLocation> situationRecordsOrdered  = repository.getSituationRecordOrderedLines(singlePoints);
    final List<LineStringLocation> situationRecordsUnordered  = repository.getSituationRecordUnorderedLinears(singlePoints);
    final List<LineStringLocation> situationRecords = new ArrayList<>();
    situationRecords.addAll(situationRecordsOrdered);
    situationRecords.addAll(situationRecordsUnordered);
    logger.info("Start map matching for Situation Record lines, count = {}", situationRecords.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> situationRecordLineMatches = new ArrayList<>(situationRecords.size());
    int counter = 1;
    for (final LineStringLocation situationRecord : situationRecords) {
      situationRecordLineMatches.add(lineStringMapMatcher.match(situationRecord));
      if (counter % 100 == 0) {
        logger.info("Matched {} Situation Record lines of {} total", counter, situationRecords.size());
      }
      counter++;
    }
    logger.info("Write results to database, matching took {} for {} locations", stopwatch, situationRecords.size());
    repository.replaceSituationRecordLineMatches(situationRecordLineMatches);

    logger.info("Done");
  }

  private void matchLmsLinks(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    final LmsRepository repository = new LmsRepository(jdbi);
    final List<LineStringLocation> lmsLinks = repository.getLmsLinks();
    logger.info("Start map matching for LMS links, count = {}", lmsLinks.size());
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> lmsLinkMatches = new ArrayList<>(lmsLinks.size());
    int counter = 1;
    for (final LineStringLocation lmsLink : lmsLinks) {
      lmsLinkMatches.add(lineStringMapMatcher.match(lmsLink));
      if (counter % 100 == 0) {
        logger.info("Matched {} LMS links of {} total", counter, lmsLinks.size());
      }
      counter++;
    }
    logger.info("Write results to database, matching took {} for {} locations", stopwatch, lmsLinks.size());
    repository.replaceLmsLinkMatches(lmsLinkMatches);

    logger.info("Done");
  }
}
