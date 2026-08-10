package nu.ndw.nls.routingmapmatcher.mappers;

import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.BaseMapMatcher;
import nu.ndw.nls.routingmapmatcher.util.Constants;
import org.springframework.stereotype.Component;

@Component
public class PMapMapper {

    public PMap createPropertyMapWithOptionalCustomModel(CustomModel customModel) {
        if (customModel != null) {
            return new PMap()
                    .putObject(CustomModel.KEY, customModel);
        }
        return new PMap();
    }

    /**
     * Default model is {@link Constants#SHORTEST_CUSTOM_MODEL} instead of no custom model in the base class {@link BaseMapMatcher}
     * @return properties with customModel or default Constants.SHORTEST_CUSTOM_MODEL
     */
    public PMap mapCustomModelOrDefaultToShortestWeighting(CustomModel customModel) {
        if (customModel != null) {
            return new PMap()
                    .putObject(CustomModel.KEY, customModel);
        }

        return new PMap()
                .putObject(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
    }


}
