//package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;
//
//import java.lang.reflect.Constructor;
//import lombok.SneakyThrows;
//
//public final class IsochroneTestHelper {
//
//    private static final int ROOT_ID = -1;
//
//    private IsochroneTestHelper() {
//    }
//
//    @SneakyThrows
//    public static IsoLabel createIsoLabel(double distance, long time) {
//        int edgeId = 1;
//        int adjNode = 2;
//        double weight = 0;
//        Constructor<IsoLabel> constructor = IsoLabel.class.getDeclaredConstructor(
//                int.class,
//                int.class,
//                double.class,
//                long.class,
//                double.class);
//        constructor.setAccessible(true);
//        var isoLabel = constructor.newInstance(edgeId, adjNode, weight, time, distance);
//        isoLabel.parent = constructor.newInstance(ROOT_ID, ROOT_ID, weight, 0, 0);
//        return isoLabel;
//    }
//
//    @SneakyThrows
//    public static IsoLabel createIsoLabel(double distance, long time, int edgeId, int adjNode, double weight) {
//        Constructor<IsoLabel> constructor = IsoLabel.class.getDeclaredConstructor(
//                int.class,
//                int.class,
//                double.class,
//                long.class,
//                double.class);
//        constructor.setAccessible(true);
//        return constructor.newInstance(edgeId, adjNode, weight, time, distance);
//    }
//
//}
