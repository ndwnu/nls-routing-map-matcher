package nu.ndw.nls.routingmapmatcher.network.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.config.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeedAndAccessAttributeMapperTest {

    private static final String CAR = "car";
    @Mock
    private Profile profile;
    private SpeedAndAccessAttributeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SpeedAndAccessAttributeMapper();
    }

    @Test
    void mapToSpeedAttribute() {
        when(profile.getName()).thenReturn(CAR);
        assertThat(mapper.mapToSpeedAttribute(profile))
                .isEqualTo("car_average_speed");
    }

    @Test
    void mapToAccessAttribute() {
        when(profile.getName()).thenReturn(CAR);
        assertThat(mapper.mapToAccessAttribute(profile))
                .isEqualTo("car_access");
    }
}
