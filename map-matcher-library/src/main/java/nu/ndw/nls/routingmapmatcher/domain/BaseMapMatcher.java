package nu.ndw.nls.routingmapmatcher.domain;

import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import java.util.Objects;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;

/**
 * Base class for implementing map-matching functionality. This class provides core mechanisms and configurations necessary for extending
 * map-matching functionality, such as handling profiles, custom models, and network access.
 */
public class BaseMapMatcher {

    private final Profile profile;
    private final CustomModel customModel;
    private final NetworkGraphHopper network;

    protected BaseMapMatcher(String profileName, NetworkGraphHopper network, CustomModel customModel) {
        this.network = Objects.requireNonNull(network);
        this.profile = Objects.requireNonNull(network.getProfile(profileName));
        this.customModel = customModel;
    }

    protected final CustomModel getCustomModel() {
        return customModel;
    }

    protected final Profile getProfile() {
        return profile;
    }

    protected final NetworkGraphHopper getNetwork() {
        return network;
    }
}
