package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm;


public class IsoLabel {

    public IsoLabel(int node, int edge, double weight, long time, double distance, IsoLabel parent) {
        this.node = node;
        this.edge = edge;
        this.weight = weight;
        this.time = time;
        this.distance = distance;
        this.parent = parent;
    }

    public boolean deleted;
    public int node;
    public int edge;
    public double weight;
    public long time;
    public double distance;
    public IsoLabel parent;

    @Override
    public String toString() {
        return "IsoLabel{" +
                "node=" + node +
                ", edge=" + edge +
                ", weight=" + weight +
                ", time=" + time +
                ", distance=" + distance +
                '}';
    }
}
