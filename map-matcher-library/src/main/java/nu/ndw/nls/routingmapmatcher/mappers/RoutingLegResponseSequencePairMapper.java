package nu.ndw.nls.routingmapmatcher.mappers;

import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponse;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponseSequence;

/**
 * Stateful stream mapper that stores the previous routing leg of a stream and is capable of returning pairs of
 * consecutive {@link RoutingLegResponse} as {@link RoutingLegResponseSequence}
 */
public class RoutingLegResponseSequencePairMapper {

    private RoutingLegResponse previous;

    public Optional<RoutingLegResponseSequence> map(RoutingLegResponse current) {
        if (previous == null) {
            previous = current;
            return Optional.empty();
        }

        Optional<RoutingLegResponseSequence> result = Optional.of(
                RoutingLegResponseSequence.of(previous, current));

        previous = current;

        return result;
    }

}
