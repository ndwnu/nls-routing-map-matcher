package nu.ndw.nls.routingmapmatcher.network.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class LinkVehicleMapper<T extends Link> {

    private final String vehicleName;
    private final Class<T> linkClass;

    public abstract DirectionalDto<Boolean> getAccessibility(T link);

    public abstract DirectionalDto<Double> getSpeed(T link);

}
