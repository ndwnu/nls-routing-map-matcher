package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import lombok.RequiredArgsConstructor;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class BearingCalculator {

    private final GeodeticCalculator geodeticCalculator;

    private static final int MAX_BEARING = 360;

    public boolean bearingIsInRange(double convertedBearing, Double inputMinBearing, Double inputMaxBearing) {
        // If no bearing is provided return true
        if (inputMinBearing == null || inputMaxBearing == null) {
            return true;
        }
        double minBearingStandardised = inputMinBearing % MAX_BEARING;
        double maxBearingStandardised = inputMaxBearing % MAX_BEARING;
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
