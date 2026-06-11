package nu.ndw.nls.routingmapmatcher.isochrone.v2.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class IsochroneLabel {

    private static final int ROOT_ID = -1;

    private final int node;

    private final int edge;

    private final int edgeKey;

    private final IsochroneLabel parent;

    private final long time;

    private final double distance;

    private final double weight;

    /**
     * Marks this label in the context of an isochrone as a path that has ended. Where it has
     * at least one outgoing path that is not traversable.
     */
    private boolean leafNode;

    private boolean deleted;

    public boolean isRoot() {
        return ROOT_ID == edge;
    }

    public void markAsLeafNode() {
        this.leafNode = true;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }
}
