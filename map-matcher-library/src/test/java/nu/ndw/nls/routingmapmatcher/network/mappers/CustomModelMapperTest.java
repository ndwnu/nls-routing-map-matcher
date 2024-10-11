package nu.ndw.nls.routingmapmatcher.network.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.config.Profile;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomModelMapperTest {

    @Mock
    private SpeedAndAccessAttributeMapper speedAndAccessAttributeMapper;
    @Mock
    private Profile profile;
    @InjectMocks
    private CustomModelMapper customModelMapper;

    @Test
    void mapToCustomModel_ok() {
        when(speedAndAccessAttributeMapper.mapToAccessAttribute(profile))
                .thenReturn("car_access");
        when(speedAndAccessAttributeMapper.mapToSpeedAttribute(profile))
                .thenReturn("car_average_speed");
        CustomModel customModel = customModelMapper.mapToCustomModel(profile);
        List<Statement> speedStatements = customModel.getSpeed();
        List<Statement> accessStatements = customModel.getPriority();
        assertThat(speedStatements).hasSize(1);
        assertThat(accessStatements).hasSize(1);
        assertThat(speedStatements.getFirst().toString())
                .isEqualTo("{\"if\": \"true\", \"limit_to\": car_average_speed}");
        assertThat(accessStatements.getFirst().toString())
                .isEqualTo("{\"if\": \"car_access == false\", \"multiply_by\": 0}");
    }
}
