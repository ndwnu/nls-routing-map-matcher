package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.json.Statement;
import com.graphhopper.json.Statement.Keyword;
import com.graphhopper.json.Statement.Op;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.VehicleProperties;
import org.junit.jupiter.api.Test;

class VehicleRestrictionsModelTest {

    private static final String EXPECTED_FULL_EXPRESSION = "max_length < 8.230000 || "
            + "max_width < 2.550000 || "
            + "max_height < 2.650000 || "
            + "max_axle_load < 10.000000 || "
            + "max_weight < 26.500000 || "
            + "car_access_forbidden == true || "
            + "hgv_access_forbidden == true || "
            + "bus_access_forbidden == true || "
            + "hgv_and_bus_access_forbidden == true || "
            + "tractor_access_forbidden == true || "
            + "slow_vehicle_access_forbidden == true || "
            + "trailer_access_forbidden == true || "
            + "motorcycle_access_forbidden == true || "
            + "motor_vehicle_access_forbidden == true || "
            + "lcv_and_hgv_access_forbidden == true";
    private static final String EXPECTED_PARTIAL_EXPRESSION = "car_access_forbidden == true || "
            + "hgv_access_forbidden == true || "
            + "bus_access_forbidden == true || "
            + "trailer_access_forbidden == true";

    @Test
    void vehicleRestrictionsModel_ok() {
        VehicleProperties vehicleProperties = VehicleProperties.builder()
                .length(8.23)
                .width(2.55)
                .height(2.65)
                .axleLoad(10.00)
                .weight(26.5)
                .carAccessForbidden(true)
                .hgvAccessForbidden(true)
                .busAccessForbidden(true)
                .hgvAndBusAccessForbidden(true)
                .tractorAccessForbidden(true)
                .slowVehicleAccessForbidden(true)
                .trailerAccessForbidden(true)
                .motorcycleAccessForbidden(true)
                .motorVehicleAccessForbidden(true)
                .lcvAndHgvAccessForbidden(true)
                .build();

        VehicleRestrictionsModel vehicleRestrictionsModel = new VehicleRestrictionsModel(vehicleProperties);
        assertThat(vehicleRestrictionsModel.getPriority()).hasSize(1);
        Statement statement = vehicleRestrictionsModel.getPriority().get(0);
        assertThat(statement.getKeyword()).isEqualTo(Keyword.IF);
        assertThat(statement.getOperation()).isEqualTo(Op.MULTIPLY);
        assertThat(statement.getValue()).isEqualTo("0");
        assertThat(statement.getCondition()).isEqualTo(EXPECTED_FULL_EXPRESSION);
    }

    @Test
    void vehicleRestrictionsModel_ok_withFalseValues() {
        VehicleProperties vehicleProperties = VehicleProperties.builder()
                .carAccessForbidden(true)
                .hgvAccessForbidden(true)
                .busAccessForbidden(true)
                .trailerAccessForbidden(true)
                .build();

        VehicleRestrictionsModel vehicleRestrictionsModel = new VehicleRestrictionsModel(vehicleProperties);
        assertThat(vehicleRestrictionsModel.getPriority()).hasSize(1);
        Statement statement = vehicleRestrictionsModel.getPriority().get(0);
        assertThat(statement.getKeyword()).isEqualTo(Keyword.IF);
        assertThat(statement.getOperation()).isEqualTo(Op.MULTIPLY);
        assertThat(statement.getValue()).isEqualTo("0");
        assertThat(statement.getCondition()).isEqualTo(EXPECTED_PARTIAL_EXPRESSION);
    }

    @Test
    void vehicleRestrictionsModel_ok_withNullProperties() {
        VehicleRestrictionsModel vehicleRestrictionsModel = new VehicleRestrictionsModel(null);
        assertThat(vehicleRestrictionsModel.getPriority()).hasSize(0);
    }
}
