package nu.ndw.nls.routingmapmatcher.domain;

import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import java.util.Objects;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.Constants;

/**
 * Abstract base class for implementing map-matching functionality. This class provides core mechanisms and configurations necessary for
 * extending map-matching functionality, such as handling profiles, custom models, and network access.
 */
public abstract class AbstractMapMatcher {

    private final Profile profile;
    private final CustomModel customModel;
    private final NetworkGraphHopper network;

    public AbstractMapMatcher(String profileName, NetworkGraphHopper network, CustomModel customModel) {
        this.network = Objects.requireNonNull(network);
        this.profile = Objects.requireNonNull(network.getProfile(profileName));
        this.customModel = customModel;
    }

    protected PMap createCustomModelMergedWithShortestCustomModelHintsIfPresent() {
        if (customModel != null) {
            return new PMap()
                    .putObject(CustomModel.KEY, CustomModel.merge(Constants.SHORTEST_CUSTOM_MODEL, customModel));
        }
        return new PMap()
                .putObject(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
    }

    protected PMap createCustomModelHintsIfPresent() {
        if (customModel != null) {
            return new PMap()
                    .putObject(CustomModel.KEY, customModel);
        }
        return new PMap();
    }

    protected CustomModel getCustomModel() {
        return customModel;
    }

    protected Profile getProfile() {
        return profile;
    }

    protected NetworkGraphHopper getNetwork() {
        return network;
    }
}
