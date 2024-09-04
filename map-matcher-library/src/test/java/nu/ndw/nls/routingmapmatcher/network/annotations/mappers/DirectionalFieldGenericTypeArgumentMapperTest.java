package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.Getter;
import lombok.Setter;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class DirectionalFieldGenericTypeArgumentMapperTest {

    private final DirectionalFieldGenericTypeArgumentMapper directionalFieldGenericTypeArgumentMapper =
            new DirectionalFieldGenericTypeArgumentMapper();

    @Getter
    @Setter
    private static class TestDto extends Link {

        private DirectionalDto<String> stringField;

        private DirectionalDto<Integer> integerField;

        protected TestDto(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry, null);
        }
    }

    @Test
    void map() {
        assertEquals(String.class, directionalFieldGenericTypeArgumentMapper.map(TestDto.class, "stringField"));
        assertEquals(Integer.class, directionalFieldGenericTypeArgumentMapper.map(TestDto.class, "integerField"));
    }
}
