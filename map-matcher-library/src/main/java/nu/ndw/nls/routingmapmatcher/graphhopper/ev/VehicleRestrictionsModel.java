package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.json.Statement;
import com.graphhopper.json.Statement.Op;
import com.graphhopper.util.CustomModel;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.VehicleProperties;

public class VehicleRestrictionsModel extends CustomModel {

    // The left parameter in the expression template is the maximum allowed value, the right parameter is the actual
    // value of the vehicle. If the expression evaluates to 'true' (e.g. if the actual value exceeds the maximum),
    // the 'priority' of the edge is multiplied by 0, rendering it inaccessible.
    private static final String SMALLER_THAN_EXPRESSION_TEMPLATE = "%s < %f";
    private static final String EQUALS_EXPRESSION_TEMPLATE = "%s == %b";

    public VehicleRestrictionsModel(VehicleProperties vehicleProperties) {
        if (vehicleProperties != null) {
            String fullExpression = Stream.of(EncodedTag.values())
                    .map(tag -> getPartialExpression(vehicleProperties, tag))
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining(" || "));
            if (!fullExpression.isEmpty()) {
                super.addToPriority(Statement.If(fullExpression, Op.MULTIPLY, "0"));
            }
        }
    }

    private Optional<String> getPartialExpression(VehicleProperties vehicleProperties, EncodedTag encodedTag) {
        return Optional.ofNullable(encodedTag.getValueFunction().apply(vehicleProperties))
                .map(value ->
                        switch (encodedTag.getOperator()) {
                            case SMALLER_THAN -> String.format(Locale.US,
                                    SMALLER_THAN_EXPRESSION_TEMPLATE, encodedTag.getKey(), value);
                            case EQUALS -> String.format(Locale.US,
                                    EQUALS_EXPRESSION_TEMPLATE, encodedTag.getKey(), value);
                        });
    }
}
