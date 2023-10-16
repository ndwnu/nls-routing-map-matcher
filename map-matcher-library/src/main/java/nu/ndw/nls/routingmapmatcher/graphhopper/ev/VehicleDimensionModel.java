package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.json.Statement;
import com.graphhopper.json.Statement.Op;
import com.graphhopper.util.CustomModel;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VehicleDimensionModel extends CustomModel {

    // The left parameter in the expression template is the maximum allowed value, the right parameter is the actual
    // value of the vehicle. If the expression evaluates to 'true' (e.g. if the actual value exceeds the maximum),
    // the 'priority' of the edge is multiplied by 0, rendering it inaccessible.
    private static final String EXPRESSION_TEMPLATE = "%s < %f";

    public VehicleDimensionModel(VehicleDimensions vehicleDimensions) {
        String fullExpression = Stream.of(EncodedTag.values())
                .map(tag -> getPartialExpression(vehicleDimensions, tag))
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" | "));
        if(!fullExpression.isEmpty()) {
            super.addToPriority(Statement.If(fullExpression, Op.MULTIPLY, "0"));
        }
    }

    private Optional<String> getPartialExpression(VehicleDimensions vehicleDimensions, EncodedTag encodedTag) {

        return Optional.ofNullable(encodedTag.getValueFunction().apply(vehicleDimensions))
                .map(dim -> String.format(Locale.US, EXPRESSION_TEMPLATE, encodedTag.getKey(), dim));

    }

}
