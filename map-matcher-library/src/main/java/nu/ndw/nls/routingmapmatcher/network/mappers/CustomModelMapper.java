package nu.ndw.nls.routingmapmatcher.network.mappers;

import com.graphhopper.config.Profile;
import com.graphhopper.json.Statement;
import com.graphhopper.json.Statement.Op;
import com.graphhopper.util.CustomModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomModelMapper {

    private final SpeedAndAccessAttributeMapper speedAndAccessAttributeMapper;

    public CustomModel mapToCustomModel(Profile profile) {
        String accessibleAttribute = speedAndAccessAttributeMapper.mapToAccessAttribute(profile);
        String speedAttribute = speedAndAccessAttributeMapper.mapToSpeedAttribute(profile);
        Statement priorityStatement = Statement.If(accessibleAttribute + " == false", Op.MULTIPLY, "0");
        Statement speedStatement = Statement.If("true", Op.LIMIT, speedAttribute);
        return new CustomModel()
                .addToPriority(priorityStatement)
                .addToSpeed(speedStatement);
    }
}
