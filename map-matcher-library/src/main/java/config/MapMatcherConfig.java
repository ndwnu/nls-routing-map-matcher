package config;

import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.RoutingMapMatcher;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.ViterbiLinestringMapMatcherFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapMatcherConfig {
    @Bean
    public RoutingMapMatcher routingMapMatcher() {
        return new RoutingMapMatcher(lineStringMapMatcherFactory());
    }

    private LineStringMapMatcherFactory lineStringMapMatcherFactory() {
        return new ViterbiLinestringMapMatcherFactory(
                new NetworkGraphHopperFactory()
        );
    }
}
