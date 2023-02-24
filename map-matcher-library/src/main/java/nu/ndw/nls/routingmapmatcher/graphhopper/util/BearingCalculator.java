package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingFilter;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class BearingCalculator {

    private static final int MAX_BEARING = 360;

    private final GeodeticCalculator geodeticCalculator;

    public boolean bearingIsInRange(final double convertedBearing, final BearingFilter bearingFilter) {
        // If no bearing filter is provided, return true so the match is always kept.
        if (bearingFilter == null) {
            return true;
        }
        final double delta = Math.abs(convertedBearing - bearingFilter.getTarget());
        final double normalizedDelta = Math.min(delta, MAX_BEARING - delta);
        return normalizedDelta <= bearingFilter.getCutoffMargin();
    }

    public double calculateBearing(final Coordinate currentCoordinate, final Coordinate nextCoordinate) {
        geodeticCalculator.setStartingGeographicPoint(currentCoordinate.getX(), currentCoordinate.getY());
        geodeticCalculator.setDestinationGeographicPoint(nextCoordinate.getX(), nextCoordinate.getY());
        final double bearing = geodeticCalculator.getAzimuth();
        return (bearing + MAX_BEARING) % MAX_BEARING;
    }
}
