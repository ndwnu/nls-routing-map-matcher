package nu.ndw.nls.routingmapmatcher.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import nu.ndw.nls.routingmapmatcher.domain.RoutingMapMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapMatcherConfiguration.class)
class MapMatcherConfigurationIT {

    @Autowired
    private RoutingMapMatcher routingMapMatcher;

    @Test
    public void testWhenSpringContextIsBootstrappedThenNoExceptions() {
        assertNotNull(routingMapMatcher);
    }

}
