package nu.ndw.nls.routingmapmatcher.util;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class BearingCalculator {

    private static final int MAX_BEARING = 360;

    private final GeodeticCalculator geodeticCalculator;

    public boolean bearingIsInRange(double actualBearing, BearingFilter bearingFilter) {
        // If no bearing filter is provided, return true so the match is always kept.
        if (bearingFilter == null) {
            return true;
        }
        return bearingDelta(actualBearing, bearingFilter.target()) <= bearingFilter.cutoffMargin();
    }

    public double bearingDelta(double actualBearing, double targetBearing) {
        double delta = Math.abs(actualBearing - targetBearing);
        return Math.min(delta, MAX_BEARING - delta);
    }

    public double calculateBearing(Coordinate currentCoordinate, Coordinate nextCoordinate) {
        geodeticCalculator.setStartingGeographicPoint(currentCoordinate.getX(), currentCoordinate.getY());
        geodeticCalculator.setDestinationGeographicPoint(nextCoordinate.getX(), nextCoordinate.getY());
        double bearing = geodeticCalculator.getAzimuth();
        return (bearing + MAX_BEARING) % MAX_BEARING;
    }
}