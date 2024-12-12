package nu.ndw.nls.routingmapmatcher.network.decoding.link;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;

/**
 * Not all {@link Link} properties are encoded on the graphhopper network, but they are required parameters in the constructors. This class
 * lists which properties they are and supplies default values that can be used for instantiating. Non-encoded properties are:
 * <ul>
 *  <li>fromNodeId with default value 0</li>
 *  <li>toNodeId with default value 0</li>
 *  <li>distanceInMeters with default value 0.0D</li>
 *   <li>geometry with default value null</li>
 * </ul>
 */
@Component
public class NonEncodedValueLinkPropertyRegistry {

    private static final String FROM_NODE_ID = "fromNodeId";
    private static final String TO_NODE_ID = "toNodeId";
    private static final String DISTANCE_IN_METERS = "distanceInMeters";
    private static final String GEOMETRY = "geometry";

    private static final Map<String, DefaultLinkField<?>> DEFAULT_LINK_PROPERTIES = Map.of(
            FROM_NODE_ID, DefaultLinkField.<Long>builder()
                    .name(FROM_NODE_ID)
                    .value(0L)
                    .type(long.class)
                    .build(),
            TO_NODE_ID, DefaultLinkField.<Long>builder()
                    .name(TO_NODE_ID)
                    .value(0L)
                    .type(long.class)
                    .build(),
            DISTANCE_IN_METERS, DefaultLinkField.<Double>builder()
                    .name(TO_NODE_ID)
                    .value(0D)
                    .type(double.class)
                    .build(),
            GEOMETRY, DefaultLinkField.<LineString>builder()
                    .name(GEOMETRY)
                    .value(null)
                    .type(LineString.class)
                    .build()
    );

    /**
     * @return sorted list of java property names from a {@link Link} that are not encoded into the network
     */
    public List<String> getNonEncodedProperties() {
        return DEFAULT_LINK_PROPERTIES.keySet().stream().sorted().toList();
    }

    /**
     * @param propertyName Java property name from a Link object
     * @return true if this is a property that is not encoded into the network
     */
    public boolean isNonEncodedProperty(String propertyName, Class<?> type) {
        if (!DEFAULT_LINK_PROPERTIES.containsKey(propertyName)) {
            return false;
        }

        return DEFAULT_LINK_PROPERTIES.get(propertyName).type().equals(type);
    }

    /**
     * @param propertyName Java property name from a Link object
     * @return a default value that can be used as dummie value to pass to the constructor of a Link object
     */
    public Object getNonEncodedPropertyDefaultValue(String propertyName) {
        DefaultLinkField<?> defaultLinkField = DEFAULT_LINK_PROPERTIES.get(propertyName);
        if (defaultLinkField == null) {
            throw new IllegalArgumentException("Property: '" + propertyName + "' is not a default non-encoded link object property. "
                    + "Use isNonEncodedProperty to check prior calling this method");
        }

        return defaultLinkField.value;
    }

    @Builder
    private record DefaultLinkField<T>(String name, Class<T> type, T value) {
    }
}
