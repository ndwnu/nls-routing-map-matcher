package nu.ndw.nls.routingmapmatcher.viterbi;

import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ViterbiLinestringMapMatcherFactory implements MapMatcherFactory<ViterbiLineStringMapMatcher> {

    @Override
    public ViterbiLineStringMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new ViterbiLineStringMapMatcher(preInitializedNetwork, profileName);
    }
}
