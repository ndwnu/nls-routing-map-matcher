package nu.ndw.nls.routingmapmatcher.geometry.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nu.ndw.nls.routingmapmatcher.geometry.model.ClosestPointResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class ClosestPointServiceIT {
    @Autowired
    private ClosestPointService closestPointService;

    @Test
    void closestPoint_ok() {
        ClosestPointResult result = closestPointService.closestPoint(
                List.of(new Coordinate(52.366349, 4.877447), new Coordinate(52.364595, 4.878614)),
                new Coordinate(52.364848, 4.878124));
        assertThat(result.distance()).isEqualTo(29.74587257816117);
        assertThat(result.bearing()).isEqualTo(303.55706380749484);
        assertThat(result.point()).isEqualTo(new Coordinate(52.364997096061515, 4.878346470864429));

    }
}
