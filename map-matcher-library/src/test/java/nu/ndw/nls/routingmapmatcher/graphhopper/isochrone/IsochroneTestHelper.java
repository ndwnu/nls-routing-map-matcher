package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import java.lang.reflect.Constructor;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.IsoLabel;

public final class IsochroneTestHelper {
    private IsochroneTestHelper(){}
    @SneakyThrows
    public static IsoLabel createIsoLabel(double distance,long time) {
        int edgeId = 1;
        int adjNode = 2;
        double weight = 0;
        Constructor<IsoLabel> constructor = IsoLabel.class.getDeclaredConstructor(
                int.class,
                int.class,
                double.class,
                long.class,
                double.class);
        constructor.setAccessible(true);
        return constructor.newInstance(edgeId, adjNode, weight, time, distance);
    }

}
