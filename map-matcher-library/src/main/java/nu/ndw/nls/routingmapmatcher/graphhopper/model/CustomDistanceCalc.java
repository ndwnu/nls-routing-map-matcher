package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.google.common.base.Preconditions;
import com.graphhopper.util.DistancePlaneProjection;

public class CustomDistanceCalc extends DistancePlaneProjection {

    private double customDistance = 0.0;
    private int customDistanceCounter = 0;

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

}
