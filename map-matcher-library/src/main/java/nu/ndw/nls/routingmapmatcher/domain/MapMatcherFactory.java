package nu.ndw.nls.routingmapmatcher.domain;


import com.graphhopper.util.CustomModel;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;

/**
 * Factory interface for creating instances of specific types of MapMatchers. The MapMatchers created are used for matching map location
 * data against a pre-initialized network graph, often in routing or navigation contexts.
 *
 * @param <T> the type of the MapMatcher that this factory produces
 */
public interface MapMatcherFactory<T> {

    /**
     * Creates an instance of a MapMatcher for a given pre-initialized network and profile name.
     *
     * @param preInitializedNetwork the pre-initialized {@link NetworkGraphHopper} network graph to be used for map matching operations
     * @param profileName           the name of the profile indicating the routing configuration to be used for the MapMatcher
     * @return an instance of the MapMatcher of type T, which can match location data against the provided network and profile
     */
    T createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName);

    /**
     * Creates an instance of a MapMatcher using a pre-initialized network, a profile name, and a custom model for additional matching
     * configuration.
     *
     * @param preInitializedNetwork the pre-initialized {@link NetworkGraphHopper} network graph to be used for map matching operations
     * @param profileName           the name of the profile defining the routing configuration to be used for the MapMatcher
     * @param customModel           a {@link CustomModel} that provides custom configurations or rules for the MapMatcher
     * @return an instance of the MapMatcher of type T, configured to perform map matching using the provided network, profile, and custom
     * model
     */
    T createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName, CustomModel customModel);
}
