package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.google.common.base.Preconditions;
import com.graphhopper.util.DistancePlaneProjection;

public class CustomDistanceCalc extends DistancePlaneProjection {

    private double customDistance;
    private int customDistanceCounter;

    public void returnCustomDistanceForNextCalls(double customDistance, int numberOfCalls) {
        Preconditions.checkArgument(customDistance >= 0.0);
        Preconditions.checkArgument(numberOfCalls >= 0);

        this.customDistance = customDistance;
        this.customDistanceCounter = numberOfCalls;
    }

    @Override
    public double calcDist(double fromLat, double fromLon, double toLat, double toLon) {
        if (customDistanceCounter > 0) {
            customDistanceCounter--;
            return customDistance;
        } else {
            return super.calcDist(fromLat, fromLon, toLat, toLon);
        }
    }
}
