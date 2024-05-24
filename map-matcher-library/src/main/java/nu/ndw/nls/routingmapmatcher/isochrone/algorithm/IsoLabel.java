package nu.ndw.nls.routingmapmatcher.isochrone.algorithm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IsoLabel {

    private static final int ROOT_ID = -1;

    private boolean deleted;
    private int node;
    private int edge;
    private double weight;
    private long time;
    private double distance;
    private IsoLabel parent;

    public boolean isRoot() {
        return ROOT_ID == edge;
    }

    public boolean parentIsRoot() {
        return ROOT_ID == parent.edge;
    }

    public IsoLabel(int node, int edge, double weight, long time, double distance, IsoLabel parent) {
        this.node = node;
        this.edge = edge;
        this.weight = weight;
        this.time = time;
        this.distance = distance;
        this.parent = parent;
    }

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
