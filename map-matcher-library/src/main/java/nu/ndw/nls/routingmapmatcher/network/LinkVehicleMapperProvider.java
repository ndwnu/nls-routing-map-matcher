package nu.ndw.nls.routingmapmatcher.network;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.util.Constants;
import org.springframework.stereotype.Component;

@Component
public class LinkVehicleMapperProvider {

    private static final String ORGANISING_ERROR_MSG = "Encountered problems while organising linkvehicles:\n%s";
    private static final String DUPLICATE_VEHICLE_MSG = "Link class '%s' has multiple vehicles with name(s): %s";
    private final Map<Class<? extends Link>, List<LinkVehicleMapper<? extends Link>>> vehiclesByLinkType;

    public LinkVehicleMapperProvider(List<LinkVehicleMapper<? extends Link>> vehicleList) {
        this.vehiclesByLinkType = vehicleList.stream()
                .collect(Collectors.groupingBy(LinkVehicleMapper::getLinkClass, Collectors.toList()));
        this.validate(this.vehiclesByLinkType);
    }

    private void validate(Map<Class<? extends Link>, List<LinkVehicleMapper<? extends Link>>> vehiclesByLinkType) {
        String messages = vehiclesByLinkType.entrySet().stream()
                .map(entry -> this.validate(entry.getKey(), entry.getValue()))
                .flatMap(Optional::stream)
                .sorted()
                .collect(Collectors.joining("\n"));
        if (!messages.isEmpty()) {
            throw new IllegalArgumentException(ORGANISING_ERROR_MSG.formatted(messages));
        }
    }

    private Optional<String> validate(Class<? extends Link> linkClass,
            List<LinkVehicleMapper<? extends Link>> vehicleList) {
        List<String> vehicleNames = vehicleList.stream().map(LinkVehicleMapper::getVehicleName).toList();
        Set<String> duplicatedVehicleNames = vehicleNames.stream()
                .filter(name -> vehicleNames.stream().filter(nameB -> nameB.equals(name)).count() > 1).collect(
                        Collectors.toSet());
        if (duplicatedVehicleNames.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(DUPLICATE_VEHICLE_MSG.formatted(linkClass.getSimpleName(), duplicatedVehicleNames));
        }
    }




    public <T extends Link> Map<String, LinkVehicleMapper<T>> getLinksForType(Class<T> linkType) {
        return Optional.ofNullable(vehiclesByLinkType.get(linkType)).orElse(List.of()).stream()
                .collect(Collectors.toMap(
                        LinkVehicleMapper::getVehicleName,
                        vehicle -> (LinkVehicleMapper<T>) vehicle));
    }

}
