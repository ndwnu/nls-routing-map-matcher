package nu.ndw.nls.routingmapmatcher.isochrone.v2.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IsochroneLabelTest {

    private static final int ROOT_EDGE = -1;

    @Test
    void isRoot_true() {
        IsochroneLabel label = createLabel(0, ROOT_EDGE, null);

        assertThat(label.isRoot()).isTrue();
    }

    @Test
    void isRoot_false() {
        IsochroneLabel label = createLabel(0, 5, null);

        assertThat(label.isRoot()).isFalse();
    }

    @Test
    void markAsLeafNode() {
        IsochroneLabel label = createLabel(0, 5, null);
        assertThat(label.isLeafNode()).isFalse();

        label.markAsLeafNode();

        assertThat(label.isLeafNode()).isTrue();
    }

    @Test
    void markAsDeleted() {
        IsochroneLabel label = createLabel(0, 5, null);
        assertThat(label.isDeleted()).isFalse();

        label.markAsDeleted();

        assertThat(label.isDeleted()).isTrue();
    }

    private IsochroneLabel createLabel(int node, int edge, IsochroneLabel parent) {
        return new IsochroneLabel(node, edge, 0, 0.0, 0L, 0.0, parent);
    }
}
