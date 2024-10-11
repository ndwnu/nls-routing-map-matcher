package nu.ndw.nls.routingmapmatcher.network.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileAccessAndSpeedAttributesTest {

    private static final String CAR_ACCESS = "car_access";
    private static final String CAR_AVERAGE_SPEED = "car_average_speed";
    private static final String CAR = "car";
    private ProfileAccessAndSpeedAttributes profileAccessAndSpeedAttributes;

    @BeforeEach
    void setUp() {
        profileAccessAndSpeedAttributes = ProfileAccessAndSpeedAttributes
                .builder()
                .accessAttributes(List.of(CAR_ACCESS))
                .speedAttributes(List.of(CAR_AVERAGE_SPEED))
                .build();
    }

    @Test
    void getAll_ok() {
        List<String> result = profileAccessAndSpeedAttributes.getAll();
        assertThat(result).contains(CAR_ACCESS, CAR_AVERAGE_SPEED);
    }

    @Test
    void isSpeedAttribute_ok() {
        assertThat(profileAccessAndSpeedAttributes.isSpeedAttribute(CAR_AVERAGE_SPEED)).isTrue();
    }

    @Test
    void isAccessAttribute_ok() {
        assertThat(profileAccessAndSpeedAttributes.isAccessAttribute(CAR_ACCESS)).isTrue();
    }

    @Test
    void getVehicleName_ok() {
        assertThat(profileAccessAndSpeedAttributes.getVehicleName(CAR_ACCESS))
                .isEqualTo(CAR);
        assertThat(profileAccessAndSpeedAttributes.getVehicleName(CAR_AVERAGE_SPEED))
                .isEqualTo(CAR);

    }

    @Test
    void getAccessAttributes_ok() {
        List<String> result = profileAccessAndSpeedAttributes.accessAttributes();
        assertThat(result).contains(CAR_ACCESS);
    }

    @Test
    void getSpeedAttributes_ok() {
        List<String> result = profileAccessAndSpeedAttributes.speedAttributes();
        assertThat(result).contains(CAR_AVERAGE_SPEED);
    }
}
