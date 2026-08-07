package nu.ndw.nls.routingmapmatcher.domain;

import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.Constants;

public class BaseMapMatcherShortestPath extends BaseMapMatcher {

    protected BaseMapMatcherShortestPath(String profileName, NetworkGraphHopper network,
            CustomModel customModel) {
        super(profileName, network, customModel);
    }

    /**
     * Default model is {@link Constants#SHORTEST_CUSTOM_MODEL} instead of no custom model in the base class {@link BaseMapMatcher}
     * @return properties with customModel or default Constants.SHORTEST_CUSTOM_MODEL
     */
    @Override
    protected PMap createPropertyMapWithOptionalCustomModel() {
        CustomModel customModel = getCustomModel();

        if (customModel != null) {
            return new PMap()
                    .putObject(CustomModel.KEY, customModel);
        }

        return new PMap()
                .putObject(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
    }

}
