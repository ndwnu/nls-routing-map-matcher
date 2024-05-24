package nu.ndw.nls.routingmapmatcher.isochrone;

import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;

public final class IsochroneTestHelper {

    private static final int ROOT_ID = -1;

    private IsochroneTestHelper() {
    }


    public static IsoLabel createIsoLabelWithNonRootParent(double distance, long time) {
        int parentId = 1;
        int parentAdjNode = 2;
        int edgeId = 3;
        int adjNode = 4;
        double weight = 0;
        IsoLabel parent = new IsoLabel(parentId, parentAdjNode, weight, 0, 0, null);
        return new IsoLabel(edgeId, adjNode, weight, time, distance, parent);
    }

    public static IsoLabel createIsoLabel(double distance, long time) {
        int edgeId = 1;
        int adjNode = 2;
        double weight = 0;
        IsoLabel parent = new IsoLabel(ROOT_ID, ROOT_ID, weight, 0, 0, null);
        return new IsoLabel(edgeId, adjNode, weight, time, distance, parent);
    }

    public static IsoLabel createIsoLabel(double distance, long time, int edgeId, int adjNode, double weight) {
        return new IsoLabel(edgeId, adjNode, weight, time, distance, null);
    }

}
