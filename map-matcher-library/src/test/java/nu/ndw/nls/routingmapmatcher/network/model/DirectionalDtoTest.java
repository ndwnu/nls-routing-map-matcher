package nu.ndw.nls.routingmapmatcher.network.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DirectionalDtoTest {

    @Test
    void constructor_ok_sameValueBothDirections() {
        DirectionalDto<Boolean> bothDirectionsTrue = new DirectionalDto<>(true);
        assertTrue(bothDirectionsTrue.forward());
        assertTrue(bothDirectionsTrue.reverse());

        DirectionalDto<Boolean> bothDirectionsFalse = new DirectionalDto<>(false);
        assertFalse(bothDirectionsFalse.forward());
        assertFalse(bothDirectionsFalse.reverse());
    }

    @Test
    void map_ok_() {
        DirectionalDto<Integer> speed = new DirectionalDto<>(0, 1);

        assertEquals(new DirectionalDto<>(false, true), speed.map(t -> t > 0));
        assertEquals(new DirectionalDto<>(false, false), speed.map(t -> t < 0));
        assertEquals(new DirectionalDto<>(true, false), speed.map(t -> t == 0));
    }

    @Test
    void isEqualInBothDirections_ok_isEqual() {
        assertTrue(new DirectionalDto<>(1).isEqualForBothDirections());
    }
    @Test
    void isEqualInBothDirections_ok_isNotEqual() {
        assertFalse(new DirectionalDto<>(0, 1).isEqualForBothDirections());
    }

    @Test
    void reduce_ok_applyFunctionOnTwoDirectionalDtos() {
        DirectionalDto<Integer> reduce = new DirectionalDto<>(1, 2)
                .reduce(new DirectionalDto<>(3, 4), Integer::sum);

        assertEquals(DirectionalDto.builder().forward(4)
                .reverse(6)
                .build(), reduce);
    }

    @Test
    void reduce_ok_applyFunctionToGetOneValue() {
        DirectionalDto<Boolean> accessibleInDirection = new DirectionalDto<>(true, false);

        // Example when both need to be accessible
        assertFalse(accessibleInDirection.reduce(this::isAccessibleInBothDirections));

        // Example when either direction should be accessible
        assertTrue(accessibleInDirection.reduce(this::isAccessibleInAnyDirections));
    }

    private boolean isAccessibleInBothDirections(boolean forward, boolean reverse) {
        return forward && reverse;
    }

    private boolean isAccessibleInAnyDirections(boolean forward, boolean reverse) {
        return forward || reverse;
    }
}