package nl.dat.routingmapmatcher;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import nl.dat.routingmapmatcher.dataaccess.repository.CbmSiterecordRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.FcdRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.LineStringLocationRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.LmsRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.MstShapefileRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.NetworkRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.NwbRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.SituationRecordRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.StartToEndRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.WazeIrregularitiesRepository;
import nl.dat.routingmapmatcher.dataaccess.repository.WazeJamsRepository;
import nl.dat.routingmapmatcher.dataaccess.support.DatabaseConnectionManager;
import nl.dat.routingmapmatcher.enums.MatchStatus;
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
    matchCbmSiterecords(readNdwNetwork(NdwNetworkSubset.FCD));
    matchFcd(readNdwNetwork(NdwNetworkSubset.OSM_FULL_NETWORK));
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

  private void matchCbmSiterecords(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new CbmSiterecordRepository(jdbi), "CBM siterecords");
  }

  private void matchFcd(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new FcdRepository(jdbi), "FCD level 1 links");
  }

  private void matchNwb(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new NwbRepository(jdbi), "NWB national highways");
  }

  private void matchWazeJams(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new WazeJamsRepository(jdbi), "Waze jams");
  }

  private void matchWazeIrregularities(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new WazeIrregularitiesRepository(jdbi), "Waze irregularities");
  }

  private void matchMstLinesShapefile(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new MstShapefileRepository(jdbi), "MST lines");
  }

  private void matchSituationRecordLines(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new SituationRecordRepository(jdbi), "Situation Record lines");
  }

  private void matchLmsLinks(final NetworkGraphHopper ndwNetwork) {
    final Jdbi jdbi = DatabaseConnectionManager.getInstance().getJdbi();

    matchLocations(ndwNetwork, new LmsRepository(jdbi), "LMS links");
  }

  private void matchLocations(final NetworkGraphHopper ndwNetwork, final LineStringLocationRepository repository,
      final String locationsName) {
    final List<LineStringLocation> locations = repository.getLocations();
    final int numLocations = locations.size();

    logger.info("Start map matching for " + locationsName + ", count = {}", numLocations);
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final LineStringMapMatcher lineStringMapMatcher = new ViterbiLineStringMapMatcher(ndwNetwork);
    final List<LineStringMatch> matches = new ArrayList<>(numLocations);
    int matched = 0;
    for (int i = 0; i < numLocations; i++) {
      final LineStringMatch match = lineStringMapMatcher.match(locations.get(i));
      matches.add(match);
      if (MatchStatus.MATCH.equals(match.getStatus())) {
        matched++;
      }
      if ((i + 1) % 100 == 0) {
        logger.info("Processed {} " + locationsName + " of {} total", i + 1, numLocations);
      }
    }

    logger.info("Writing results to database. Processing took {}", stopwatch);
    repository.replaceMatches(matches);

    logger.info("Done. Processed {} locations, {} successfully matched ({}%)", numLocations, matched,
        matched * 10000 / numLocations / 100.0);
  }
}
