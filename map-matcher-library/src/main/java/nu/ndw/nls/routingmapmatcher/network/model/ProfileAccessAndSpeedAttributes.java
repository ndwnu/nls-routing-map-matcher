package nu.ndw.nls.routingmapmatcher.network.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/**
 * In gh 9xx we need to add average speed and access attributes per profile we need to generate speed and access
 * attribute names. This class is responsible for this logic.
 */
@Builder
public record ProfileAccessAndSpeedAttributes(List<String> accessAttributes,
                                              List<String> speedAttributes) {

    public List<String> getAll() {
        List<String> result = new ArrayList<>(accessAttributes);
        result.addAll(speedAttributes);
        return result;
    }

    public boolean isSpeedAttribute(String attributeName) {
        return speedAttributes.contains(attributeName);
    }

    public boolean isAccessAttribute(String attributeName) {
        return accessAttributes.contains(attributeName);
    }

    public String getVehicleName(String attributeName) {
        return attributeName
                .replace("_average_speed", "")
                .replace("_access", "");
    }


}
