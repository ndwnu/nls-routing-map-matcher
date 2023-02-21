package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class BearingCalculator {

    private final GeodeticCalculator geodeticCalculator;

    public static final int MAX_BEARING = 360;

    public static final int REVERSE_BEARING = 180;

    public boolean bearingIsInRange(final double convertedBearing, final BearingRange bearingRange) {
        // If no bearing is provided return true
        if (bearingRange == null) {
            return true;
        }
        final double minBearingStandardized = bearingRange.getMinBearing() % MAX_BEARING;
        final double maxBearingStandardized = bearingRange.getMaxBearing() % MAX_BEARING;
        if (minBearingStandardized > maxBearingStandardized) {
            return convertedBearing >= minBearingStandardized || convertedBearing <= maxBearingStandardized;
        } else {
            return convertedBearing >= minBearingStandardized && convertedBearing <= maxBearingStandardized;
        }
    }

    public double calculateBearing(final Coordinate currentCoordinate, final Coordinate nextCoordinate) {
        geodeticCalculator.setStartingGeographicPoint(currentCoordinate.getX(), currentCoordinate.getY());
        geodeticCalculator.setDestinationGeographicPoint(nextCoordinate.getX(), nextCoordinate.getY());
        final double bearing = geodeticCalculator.getAzimuth();
        return (bearing + MAX_BEARING) % MAX_BEARING;
    }
}
