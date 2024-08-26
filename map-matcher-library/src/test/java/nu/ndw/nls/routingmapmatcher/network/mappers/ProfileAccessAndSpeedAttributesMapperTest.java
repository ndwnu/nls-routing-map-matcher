package nu.ndw.nls.routingmapmatcher.network.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.config.Profile;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.network.model.ProfileAccessAndSpeedAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileAccessAndSpeedAttributesMapperTest {

    private static final String CAR_ACCESS = "car_access";
    private static final String CAR_AVERAGE_SPEED = "car_average_speed";
    @Mock
    private SpeedAndAccessAttributeMapper speedAndAccessAttributeMapper;
    @Mock
    private Profile profile;
    @InjectMocks
    private ProfileAccessAndSpeedAttributesMapper mapper;

    @Test
    void map_ok() {
        when(speedAndAccessAttributeMapper.mapToAccessAttribute(profile))
                .thenReturn(CAR_ACCESS);
        when(speedAndAccessAttributeMapper.mapToSpeedAttribute(profile))
                .thenReturn(CAR_AVERAGE_SPEED);
        ProfileAccessAndSpeedAttributes result = mapper.map(List.of(profile));
        ProfileAccessAndSpeedAttributes expected = ProfileAccessAndSpeedAttributes
                .builder()
                .speedAttributes(List.of(CAR_AVERAGE_SPEED))
                .accessAttributes(List.of(CAR_ACCESS))
                .build();
        assertThat(result).isEqualTo(expected);

    }
}
