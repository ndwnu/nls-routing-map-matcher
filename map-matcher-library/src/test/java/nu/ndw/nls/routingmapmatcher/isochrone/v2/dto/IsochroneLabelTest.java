package nu.ndw.nls.routingmapmatcher.isochrone.v2.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IsochroneLabelTest {

    private static final int ROOT_EDGE = -1;

    @Test
    void isRoot_true() {
        IsochroneLabel label = createLabel(ROOT_EDGE, null);

        assertThat(label.isRoot()).isTrue();
    }

    @Test
    void isRoot_false() {
        IsochroneLabel label = createLabel(5, null);

        assertThat(label.isRoot()).isFalse();
    }

    @Test
    void getParent() {
        IsochroneLabel parent = createLabel(ROOT_EDGE, null);
        IsochroneLabel label = createLabel(1, parent);

        IsochroneLabel actualParent = label.getParent();
        assertThat(actualParent).isEqualTo(parent);

        IsochroneLabel actualGrandParent = parent.getParent();
        assertThat(actualGrandParent).isNull();
    }

    @Test
    void markAsLeafNode() {
        IsochroneLabel label = createLabel(5, null);
        assertThat(label.isLeafNode()).isFalse();

        label.markAsLeafNode();

        assertThat(label.isLeafNode()).isTrue();
    }

    @Test
    void markAsDeleted() {
        IsochroneLabel label = createLabel(5, null);
        assertThat(label.isDeleted()).isFalse();

        label.markAsDeleted();

        assertThat(label.isDeleted()).isTrue();
    }

    @Test
    void drawPath() {
        IsochroneLabel parent = new IsochroneLabel(1, 1, 4, null, 0L, 0.0, 0.0);
        IsochroneLabel label = new IsochroneLabel(2, 3, 6, parent, 0L, 0.0, 0.0);

        assertThat(label.drawPath()).isEqualTo("1(4) -> 2(6)");
    }

    private IsochroneLabel createLabel(int edge, IsochroneLabel parent) {
        return new IsochroneLabel(0, edge, 0, parent, 0L, 0.0, 0.0);
    }
}
