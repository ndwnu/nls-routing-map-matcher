package nu.ndw.nls.routingmapmatcher.config;

import nu.ndw.nls.routingmapmatcher.domain.RoutingMapMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapMatcherAutoConfiguration.class)
class MapMatcherAutoConfigurationIT {

    @Autowired
    private RoutingMapMatcher routingMapMatcher;

    @Test
    public void testWhenSpringContextIsBootstrappedThenNoExceptions() {
        assertNotNull(routingMapMatcher);
    }

}
