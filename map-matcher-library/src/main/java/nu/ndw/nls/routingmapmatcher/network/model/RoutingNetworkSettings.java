package nu.ndw.nls.routingmapmatcher.network.model;

import com.graphhopper.config.Profile;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(builderMethodName = "genericBuilder")
@Getter
@EqualsAndHashCode
@ToString
public class RoutingNetworkSettings<T extends Link> {

    private final Class<T> linkType;
    private final String networkNameAndVersion;
    private final Supplier<Iterator<T>> linkSupplier;
    private final Instant dataDate;
    private final List<Profile> profiles;
    private final Path graphhopperRootPath;
    @Builder.Default
    private final boolean indexed = false;
    @Builder.Default
    private final boolean expandBounds = false;

    public static <U extends Link> RoutingNetworkSettingsBuilder<U> builder(Class<U> linkType) {
        return RoutingNetworkSettings.<U>genericBuilder().linkType(linkType);
    }

}
