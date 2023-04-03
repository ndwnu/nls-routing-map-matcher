package com.graphhopper.util;

/**
 * This class  has a calcNormalizedEdgeDistanceNew method which was removed in the current version, but is necessary to
 * calculate the EdgeDistance correctly in LineStringScoreUtil. Using the regular calcNormalizedEdgeDistance without the
 * reduceToSegment logic produces incorrect reliability results in It test.
 */
public class DistanceCalcCustom extends DistancePlaneProjection {


    public double calcNormalizedEdgeDistanceNew(double rLatDeg, double rLonDeg, double aLatDeg, double aLonDeg,
            double bLatDeg, double bLonDeg, boolean reduceToSegment) {
        double shrinkFactor = this.calcShrinkFactor(aLatDeg, bLatDeg);
        double aLon = aLonDeg * shrinkFactor;
        double bLon = bLonDeg * shrinkFactor;
        double rLon = rLonDeg * shrinkFactor;
        double deltaLon = bLon - aLon;
        double deltaLat = bLatDeg - aLatDeg;
        if (deltaLat == 0.0) {
            return this.calcNormalizedDist(aLatDeg, rLonDeg, rLatDeg, rLonDeg);
        } else if (deltaLon == 0.0) {
            return this.calcNormalizedDist(rLatDeg, aLonDeg, rLatDeg, rLonDeg);
        } else {
            double norm = deltaLon * deltaLon + deltaLat * deltaLat;
            double factor = ((rLon - aLon) * deltaLon + (rLatDeg - aLatDeg) * deltaLat) / norm;
            if (reduceToSegment) {
                if (factor > 1.0) {
                    factor = 1.0;
                } else if (factor < 0.0) {
                    factor = 0.0;
                }
            }

            double c_lon = aLon + factor * deltaLon;
            double c_lat = aLatDeg + factor * deltaLat;
            return this.calcNormalizedDist(c_lat, c_lon / shrinkFactor, rLatDeg, rLonDeg);
        }
    }
}
