package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.util.CustomModel;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final double WEIGHTING_SHORTEST_DISTANCE_INFLUENCE = 10_000D;
    public static final CustomModel SHORTEST_CUSTOM_MODEL = new CustomModel()
            .setDistanceInfluence(WEIGHTING_SHORTEST_DISTANCE_INFLUENCE);
}
