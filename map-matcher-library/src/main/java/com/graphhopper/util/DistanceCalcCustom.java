package com.graphhopper.util;

public class DistanceCalcCustom extends  DistancePlaneProjection{
    public double calcNormalizedEdgeDistanceNew(double r_lat_deg, double r_lon_deg, double a_lat_deg, double a_lon_deg, double b_lat_deg, double b_lon_deg, boolean reduceToSegment) {
        double shrinkFactor = this.calcShrinkFactor(a_lat_deg, b_lat_deg);
        double a_lon = a_lon_deg * shrinkFactor;
        double b_lon = b_lon_deg * shrinkFactor;
        double r_lon = r_lon_deg * shrinkFactor;
        double delta_lon = b_lon - a_lon;
        double delta_lat = b_lat_deg - a_lat_deg;
        if (delta_lat == 0.0) {
            return this.calcNormalizedDist(a_lat_deg, r_lon_deg, r_lat_deg, r_lon_deg);
        } else if (delta_lon == 0.0) {
            return this.calcNormalizedDist(r_lat_deg, a_lon_deg, r_lat_deg, r_lon_deg);
        } else {
            double norm = delta_lon * delta_lon + delta_lat * delta_lat;
            double factor = ((r_lon - a_lon) * delta_lon + (r_lat_deg - a_lat_deg) * delta_lat) / norm;
            if (reduceToSegment) {
                if (factor > 1.0) {
                    factor = 1.0;
                } else if (factor < 0.0) {
                    factor = 0.0;
                }
            }

            double c_lon = a_lon + factor * delta_lon;
            double c_lat = a_lat_deg + factor * delta_lat;
            return this.calcNormalizedDist(c_lat, c_lon / shrinkFactor, r_lat_deg, r_lon_deg);
        }
    }
}
