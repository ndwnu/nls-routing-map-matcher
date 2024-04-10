package nu.ndw.nls.routingmapmatcher.geometry.model;

import org.locationtech.jts.geom.Coordinate;

public record ProjectionResult(double distance, double bearing, Coordinate point) {

}
