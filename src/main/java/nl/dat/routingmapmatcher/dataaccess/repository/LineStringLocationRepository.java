package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public interface LineStringLocationRepository {

  List<LineStringLocation> getLocations();

  void replaceMatches(final List<LineStringMatch> lineStringMatches);
}
