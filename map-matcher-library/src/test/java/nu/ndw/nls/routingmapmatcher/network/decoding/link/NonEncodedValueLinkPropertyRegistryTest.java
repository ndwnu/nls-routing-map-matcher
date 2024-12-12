package nu.ndw.nls.routingmapmatcher.network.decoding.link;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class NonEncodedValueLinkPropertyRegistryTest {

    private static final String FROM_NODE_ID = "fromNodeId";
    private static final String TO_NODE_ID = "toNodeId";
    private static final String DISTANCE_IN_METERS = "distanceInMeters";
    private static final String GEOMETRY = "geometry";

    private final NonEncodedValueLinkPropertyRegistry nonEncodedValueLinkPropertyRegistry = new NonEncodedValueLinkPropertyRegistry();

    @Test
    void getNonEncodedProperties_ok() {
        assertThat(nonEncodedValueLinkPropertyRegistry.getNonEncodedProperties()).isEqualTo(List.of(DISTANCE_IN_METERS, FROM_NODE_ID, GEOMETRY,
                TO_NODE_ID));
    }

    @Test
    void isNonEncodedProperty_ok_true() {
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(DISTANCE_IN_METERS, double.class)).isTrue();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(FROM_NODE_ID, long.class)).isTrue();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(GEOMETRY, LineString.class)).isTrue();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(TO_NODE_ID, long.class)).isTrue();
    }

    @Test
    void isNonEncodedProperty_ok_falseWrongType() {
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(DISTANCE_IN_METERS, Boolean.class)).isFalse();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(FROM_NODE_ID, Boolean.class)).isFalse();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(GEOMETRY, Boolean.class)).isFalse();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(TO_NODE_ID, Boolean.class)).isFalse();
    }

    @Test
    void isNonEncodedProperty_ok_falseWrongProperty() {
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(DISTANCE_IN_METERS + "a", double.class)).isFalse();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(FROM_NODE_ID + "a", long.class)).isFalse();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(GEOMETRY + "a", LineString.class)).isFalse();
        assertThat(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(TO_NODE_ID + "a", long.class)).isFalse();
    }

    @Test
    void getNonEncodedPropertyDefaultValue_ok() {
        assertThat(nonEncodedValueLinkPropertyRegistry.getNonEncodedPropertyDefaultValue(DISTANCE_IN_METERS)).isEqualTo(0.0D);
        assertThat(nonEncodedValueLinkPropertyRegistry.getNonEncodedPropertyDefaultValue(FROM_NODE_ID)).isEqualTo(0L);
        assertThat(nonEncodedValueLinkPropertyRegistry.getNonEncodedPropertyDefaultValue(GEOMETRY)).isNull();
        assertThat(nonEncodedValueLinkPropertyRegistry.getNonEncodedPropertyDefaultValue(TO_NODE_ID)).isEqualTo(0L);
    }

    @Test
    void getNonEncodedPropertyDefaultValue_fail_wrongProperty() {
        assertThatThrownBy(() -> nonEncodedValueLinkPropertyRegistry.getNonEncodedPropertyDefaultValue("not a property"))
                .isInstanceOf(IllegalArgumentException.class)
                .message().isEqualTo("Property: 'not a property' is not a default non-encoded link object property. Use "
                        + "isNonEncodedProperty to check prior calling this method");
    }
}