package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistancePlaneProjection;

public class GraphHopperConstants {

    public static final AngleCalc angleCalculation = new AngleCalc();
    public static final DistanceCalc distanceCalculation = new DistancePlaneProjection();

    private GraphHopperConstants() {
        // Prevent instantiation
    }

}
