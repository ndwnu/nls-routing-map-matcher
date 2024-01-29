package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                    geometry);
            this.booleanField = booleanField;
            this.directionalInteger = directionalInteger;
        }


        @EncodedValue(key = "booleanField")
        private final Boolean booleanField;

        @Override
        @EncodedValue(key = "way_id", bits = 31)
        public long getId() {
            return super.getId();
        }

        @EncodedValue(key = "directionalInteger", bits=12)
        private final DirectionalDto<Integer> directionalInteger;
    }

    @Test
    void map() {
        EncodedValuesByTypeDto<MyLink> map = encodedValuesMapper.map(MyLink.class);

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

        EncodedValueDto<MyLink, Long> id = map.get(Long.class, "way_id");
        assertEquals("way_id", id.key());
        assertEquals(Long.class, id.valueType());
        assertEquals(31, id.bits());
        assertFalse(id.isDirectional());
        assertNotNull(id.valueSupplier());
        assertNull(id.valueReverseSupplier());
        assertEquals(1L, id.valueSupplier().apply(myLinkInstance));



        EncodedValueDto<MyLink, Boolean> booleanField = map.get(Boolean.class, "booleanField");
        assertEquals("booleanField", booleanField.key());
        assertEquals( Boolean.class, booleanField.valueType());
        assertEquals(0, booleanField.bits());
        assertFalse(booleanField.isDirectional());
        assertNotNull(booleanField.valueSupplier());
        assertNull(booleanField.valueReverseSupplier());
        assertTrue(booleanField.valueSupplier().apply(myLinkInstance));


        EncodedValueDto<MyLink, Integer> directionalIntegerField = map.get(Integer.class, "directionalInteger");
        assertEquals("directionalInteger", directionalIntegerField.key());
        assertEquals( Integer.class, directionalIntegerField.valueType());
        assertEquals(12, directionalIntegerField.bits());
        assertTrue(directionalIntegerField.isDirectional());
        assertNotNull(directionalIntegerField.valueSupplier());
        assertNotNull(directionalIntegerField.valueReverseSupplier());
        assertEquals(10, directionalIntegerField.valueSupplier().apply(myLinkInstance));
        assertEquals(20, directionalIntegerField.valueReverseSupplier().apply(myLinkInstance));

    }
}