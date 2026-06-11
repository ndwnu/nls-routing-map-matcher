package nu.ndw.nls.routingmapmatcher.isochrone.v2.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IsochroneLabelTest {

    private static final int ROOT_EDGE = -1;

    @Test
    void isRoot_true() {
        IsochroneLabel label = createLabel(ROOT_EDGE);

        assertThat(label.isRoot()).isTrue();
    }

    @Test
    void isRoot_false() {
        IsochroneLabel label = createLabel(5);

        assertThat(label.isRoot()).isFalse();
    }

    @Test
    void markAsLeafNode() {
        IsochroneLabel label = createLabel(5);
        assertThat(label.isLeafNode()).isFalse();

        label.markAsLeafNode();

        assertThat(label.isLeafNode()).isTrue();
    }

    @Test
    void markAsDeleted() {
        IsochroneLabel label = createLabel(5);
        assertThat(label.isDeleted()).isFalse();

        label.markAsDeleted();

        assertThat(label.isDeleted()).isTrue();
    }

    private IsochroneLabel createLabel(int edge) {
        return new IsochroneLabel(0, edge, 0, null, 0L, 0.0, 0.0);
    }
}
