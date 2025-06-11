package nu.ndw.nls.routingmapmatcher.starttoend;

import com.graphhopper.util.CustomModel;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.confidence.LineStringReliabilityCalculator;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartToEndMapMatcherFactory implements MapMatcherFactory<StartToEndMapMatcher> {

    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final PointListUtil pointListUtil;
    private final LineStringReliabilityCalculator lineStringReliabilityCalculator;

    @Override
    public StartToEndMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new StartToEndMapMatcher(preInitializedNetwork, profileName, fractionAndDistanceCalculator, pointListUtil,
                lineStringReliabilityCalculator, null);
    }

    @Override
    public StartToEndMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName, CustomModel customModel) {
        return new StartToEndMapMatcher(preInitializedNetwork, profileName, fractionAndDistanceCalculator, pointListUtil,
                lineStringReliabilityCalculator, customModel);
    }
}
