package nu.ndw.nls.routingmapmatcher.mappers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponse;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponseSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutingLegResponseSequencePairMapperTest {

    @Mock
    private RoutingLegResponse routingLegResponseA;

    @Mock
    private RoutingLegResponse routingLegResponseB;

    @Mock
    private RoutingLegResponse routingLegResponseC;

    private RoutingLegResponseSequencePairMapper routingLegResponseSequencePairMapper =
            new RoutingLegResponseSequencePairMapper();

    @BeforeEach
    void setUp() {
        routingLegResponseSequencePairMapper = new RoutingLegResponseSequencePairMapper();
    }

    @Test
    void map_ok() {
        assertEquals(Optional.empty(), routingLegResponseSequencePairMapper.map(routingLegResponseA));
        assertEquals(Optional.of(RoutingLegResponseSequence.of(routingLegResponseA, routingLegResponseB)),
                routingLegResponseSequencePairMapper.map(routingLegResponseB));
        assertEquals(Optional.of(RoutingLegResponseSequence.of(routingLegResponseB, routingLegResponseC)),
                routingLegResponseSequencePairMapper.map(routingLegResponseC));
    }
}