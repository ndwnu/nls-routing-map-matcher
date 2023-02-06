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

    public boolean bearingIsInRange(double convertedBearing, BearingRange bearingRange) {
        // If no bearing is provided return true
        if (bearingRange == null) {
            return true;
        }
        double minBearingStandardised = bearingRange.getMinBearing() % MAX_BEARING;
        double maxBearingStandardised = bearingRange.getMaxBearing() % MAX_BEARING;
        if (minBearingStandardised > maxBearingStandardised) {
            return convertedBearing >= minBearingStandardised || convertedBearing <= maxBearingStandardised;

        } else {
            return convertedBearing >= minBearingStandardised && convertedBearing <= maxBearingStandardised;
        }
    }

    public double calculateBearing(Coordinate currentCoordinate, Coordinate nextCoordinate) {
        geodeticCalculator.setStartingGeographicPoint(currentCoordinate.getX(),
                currentCoordinate.getY());
        geodeticCalculator.setDestinationGeographicPoint(nextCoordinate.getX(),
                nextCoordinate.getY());
        final double bearing = geodeticCalculator.getAzimuth();
        return (bearing + MAX_BEARING) % MAX_BEARING;
    }
}
