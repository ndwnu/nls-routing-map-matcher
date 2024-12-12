package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class EncodedValuesMapperTest {

    private final EncodedValuesMapper encodedValuesMapper = new EncodedValuesMapper(new AnnotationMapper(),
            new DirectionalFieldGenericTypeArgumentMapper());

    @Getter
    public static class MyLink extends Link {

        @Builder(builderMethodName = "myLinkBuilder")
        protected MyLink(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry,
                Boolean booleanField, DirectionalDto<Integer> directionalInteger) {
            super(id, fromNodeId, toNodeId, distanceInMeters,
                    geometry, null);
            this.booleanField = booleanField;
            this.directionalInteger = directionalInteger;
        }

        @EncodedValue(key = "boolean_field")
        private final Boolean booleanField;

        @Override
        @EncodedValue(key = "way_id", bits = 31)
        public long getId() {
            return super.getId();
        }

        @EncodedValue(key = "directional_integer", bits=12)
        private final DirectionalDto<Integer> directionalInteger;
    }

    @Test
    void map_ok() {
        EncodedValuesByTypeDto<MyLink> resultEncodedValues = encodedValuesMapper.map(MyLink.class);

        MyLink myLinkInstance = MyLink.myLinkBuilder()
                .id(1)
                .fromNodeId(2)
                .toNodeId(3)
                .distanceInMeters(5)
                .geometry(null)
                .booleanField(true)
                .directionalInteger(DirectionalDto.<Integer>builder()
                        .forward(10)
                        .reverse(20)
                        .build())
                .build();

        Optional<EncodedValueDto<MyLink, Long>> optionalId = resultEncodedValues.getByKey(Long.class, "way_id");
        assertThat(optionalId)
                .isPresent()
                .isEqualTo(resultEncodedValues.getByProperty(Long.class, "id"));

        EncodedValueDto<MyLink, Long> id = optionalId.get();

        assertEquals("way_id", id.key());
        assertEquals(Long.class, id.valueType());
        assertEquals(31, id.bits());
        assertFalse(id.isDirectional());
        assertNotNull(id.valueSupplier());
        assertNull(id.valueReverseSupplier());
        assertEquals(1L, id.valueSupplier().apply(myLinkInstance));

        Optional<EncodedValueDto<MyLink, Boolean>> optionalBoolean = resultEncodedValues.getByKey(Boolean.class, "boolean_field");
        assertThat(optionalBoolean)
                .isPresent()
                .isEqualTo(resultEncodedValues.getByProperty(Boolean.class, "booleanField"));

        EncodedValueDto<MyLink, Boolean> booleanField = optionalBoolean.get();
        assertEquals("boolean_field", booleanField.key());
        assertEquals( Boolean.class, booleanField.valueType());
        assertEquals(0, booleanField.bits());
        assertFalse(booleanField.isDirectional());
        assertNotNull(booleanField.valueSupplier());
        assertNull(booleanField.valueReverseSupplier());
        assertTrue(booleanField.valueSupplier().apply(myLinkInstance));


        Optional<EncodedValueDto<MyLink, Integer>> optionalDirectionInteger =
                resultEncodedValues.getByKey(Integer.class, "directional_integer");

        assertThat(optionalDirectionInteger)
                    .isPresent()
                    .isEqualTo(resultEncodedValues.getByProperty(Integer.class, "directionalInteger"));

        EncodedValueDto<MyLink, Integer> directionalIntegerField = optionalDirectionInteger.get();

        assertEquals("directional_integer", directionalIntegerField.key());
        assertEquals( Integer.class, directionalIntegerField.valueType());
        assertEquals(12, directionalIntegerField.bits());
        assertTrue(directionalIntegerField.isDirectional());
        assertNotNull(directionalIntegerField.valueSupplier());
        assertNotNull(directionalIntegerField.valueReverseSupplier());
        assertEquals(10, directionalIntegerField.valueSupplier().apply(myLinkInstance));
        assertEquals(20, directionalIntegerField.valueReverseSupplier().apply(myLinkInstance));
    }
}
