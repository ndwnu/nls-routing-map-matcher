package com.graphhopper.util;

import com.google.common.base.Preconditions;

/**
 * This class  has a calcNormalizedEdgeDistanceNew method which was removed in the current version, but is necessary to
 * calculate the EdgeDistance correctly in LineStringScoreUtil. Using the regular calcNormalizedEdgeDistance without the
 * reduceToSegment logic produces incorrect reliability results in It test.
 *
 * Also, this class has a customized calcDist method to prevent filtering in the map matcher see usage in method
 * preventFilteringWhileMapMatching in the ViterbiLineStringMapMatcher.java class
 */
public class DistanceCalcCustom extends DistancePlaneProjection {

    private double customDistance;
    private int customDistanceCounter;

    public void returnCustomDistanceForNextCalls(final double customDistance, final int numberOfCalls) {
        Preconditions.checkArgument(customDistance >= 0.0);
        Preconditions.checkArgument(numberOfCalls >= 0);

        this.customDistance = customDistance;
        this.customDistanceCounter = numberOfCalls;
    }

    @Override
    public double calcDist(final double fromLat, final double fromLon, final double toLat, final double toLon) {
        if (customDistanceCounter > 0) {
            customDistanceCounter--;
            return customDistance;
        } else {
            return super.calcDist(fromLat, fromLon, toLat, toLon);
        }
    }


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
