package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphhopper.json.Statement;
import com.graphhopper.json.Statement.Keyword;
import com.graphhopper.json.Statement.Op;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.VehicleProperties;
import org.junit.jupiter.api.Test;

class VehicleRestrictionsModelTest {

    private static final String EXPECTED_FULL_EXPRESSION = "max_axle_load < 10.000000 || "
            + "max_height < 2.650000 || "
            + "max_length < 8.230000 || "
            + "max_weight < 26.500000 || "
            + "max_width < 2.550000 || "
            + "hgv_access_forbidden == true || "
            + "car_access_forbidden == true || "
            + "auto_bus_access_forbidden == true || "
            + "trailer_access_forbidden == true || "
            + "hgv_and_auto_bus_access_forbidden == true || "
            + "motor_bike_access_forbidden == true || "
            + "motor_vehicle_access_forbidden == true";
    private static final String EXPECTED_PARTIAL_EXPRESSION = "hgv_access_forbidden == true || "
            + "car_access_forbidden == true || "
            + "auto_bus_access_forbidden == true || "
            + "trailer_access_forbidden == true";

    @Test
    void VehicleRestrictionsModel_will_parse_ok() {
        VehicleProperties vehicleProperties = VehicleProperties
                .builder()
                .autoBusAccessForbidden(true)
                .carAccessForbidden(true)
                .trailerAccessForbidden(true)
                .hgvAccessForbidden(true)
                .motorVehicleAccessForbidden(true)
                .hgvAndAutoBusAccessForbidden(true)
                .motorBikeAccessForbidden(true)
                .axleLoad(10.00)
                .weight(26.5)
                .height(2.65)
                .length(8.23)
                .width(2.55)
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
    void VehicleRestrictionsModel_with_null_values_will_parse_ok() {
        VehicleProperties vehicleProperties = VehicleProperties
                .builder()
                .autoBusAccessForbidden(true)
                .carAccessForbidden(true)
                .trailerAccessForbidden(true)
                .hgvAccessForbidden(true)
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
    void VehicleRestrictionsModel_with_null_properties_will_parse_ok() {
        VehicleRestrictionsModel vehicleRestrictionsModel = new VehicleRestrictionsModel(null);
        assertThat(vehicleRestrictionsModel.getPriority()).hasSize(0);
    }
}
