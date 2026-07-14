package nu.ndw.nls.routingmapmatcher.isochrone.v2.dto;

import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString(exclude = "parent")
public class IsochroneLabel {

    private static final int ROOT_ID = -1;

    @Getter
    private final int node;

    @Getter
    private final int edge;

    @Getter
    private final int edgeKey;

    private final IsochroneLabel parent;

    @Getter
    private final long timeInMilliSeconds;

    @Getter
    private final double distanceInMeters;

    @Getter
    private final double weight;

    /**
     * Marks this label in the context of an isochrone as a path that has ended. Where it has at least one outgoing path that is not
     * traversable.
     */
    @Getter
    private boolean leafNode;

    @Getter
    private boolean deleted;

    public boolean isRoot() {
        return ROOT_ID == edge;
    }

    public <T extends IsochroneLabel> T getParent() {
        if (Objects.isNull(parent)) {
            return null;
        }
        return (T) parent;
    }

    public void markAsLeafNode() {
        this.leafNode = true;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public String drawPath() {
        StringBuilder path = new StringBuilder();
        IsochroneLabel current = this;
        while (current != null) {
            if (!path.isEmpty()) {
                path.insert(0, " -> ");
            }
            path.insert(0, "%s(%s)".formatted(current.getNode(), current.getEdgeKey()));
            current = current.getParent();
        }

        return path.toString();
    }
}
