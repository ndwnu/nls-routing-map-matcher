package nu.ndw.nls.routingmapmatcher.network;

import static java.util.Objects.requireNonNull;
import static nu.ndw.nls.routingmapmatcher.util.GraphHopperNetworkPathUtils.formatNormalizedPath;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.ev.DefaultImportRegistry;
import com.graphhopper.routing.ev.ImportRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.exception.GraphHopperNotImportedException;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.EncodedValuesMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedMapperFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedValueFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.LinkImportRegistry;
import nu.ndw.nls.routingmapmatcher.network.mappers.CustomModelMapper;
import nu.ndw.nls.routingmapmatcher.network.mappers.ProfileAccessAndSpeedAttributesMapper;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.ProfileAccessAndSpeedAttributes;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GraphHopperNetworkService {

    private static final String DELIMITER = ",";
    private static final String NO_LINK_SUPPLIER_MSG = "Link supplier must be provided when creating new network";
    private static final String NO_PATH_MSG_READ = "GraphHopper root path must be specified when reading from disk";
    private static final String NO_NAME_MSG_READ = "Network name and version must be specified when reading from disk";
    private static final String NO_PATH_MSG_WRITE = "GraphHopper root path must be specified when writing to disk";
    private static final String NO_NAME_MSG_WRITE = "Network name and version must be specified when writing to disk";
    private static final String NO_NETWORK_MSG = "GraphHopper network %s is not imported on disk";

    private final LinkVehicleMapperProvider linkVehicleMapperProvider;
    private final EncodedValueFactoryRegistry encodedValueFactoryRegistry;
    private final EncodedValuesMapper encodedValuesMapper;
    private final EncodedMapperFactoryRegistry encodedMapperFactoryRegistry;
    private final CustomModelMapper customModelMapper;
    private final ProfileAccessAndSpeedAttributesMapper profileAccessAndSpeedAttributesMapper;


    /**
     * Creates an in memory network
     *
     * @param networkSettings routing network with link supplier
     * @return a network loaded from links in memory
     */
    public <T extends Link> NetworkGraphHopper inMemory(RoutingNetworkSettings<T> networkSettings) {
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(networkSettings);

        Map<String, LinkVehicleMapper<T>> providers = linkVehicleMapperProvider.getLinksForType(
                networkSettings.getLinkType());
        validateVehicles(networkSettings.getProfiles(), providers, networkSettings.getLinkType());
        configureGraphHopper(networkSettings.getLinkType(), networkSettings.getProfiles(), graphHopper);

        graphHopper.setStoreOnFlush(false);
        // Required even though we explicitly set store on flush to false
        graphHopper.setGraphHopperLocation(Path.of("/tmp").toString());

        graphHopper.importOrLoad();

        return graphHopper;
    }


    /***
     * In order to separate reads and writes to/form disk cache this method only loads existing networks from disk.
     * Use this method to load a previously stored network.
     * @param networkSettings routing network without link supplier
     * @return a network loaded from disk
     * @throws GraphHopperNotImportedException if there is no network on disk it will throw this exception
     */
    public <T extends Link> NetworkGraphHopper loadFromDisk(RoutingNetworkSettings<T> networkSettings)
            throws GraphHopperNotImportedException {
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(networkSettings);
        Path path = requireNonNull(networkSettings.getGraphhopperRootPath(), NO_PATH_MSG_READ);
        String nameAndVersion = requireNonNull(networkSettings.getNetworkNameAndVersion(), NO_NAME_MSG_READ);
        graphHopper.setGraphHopperLocation(formatNormalizedPath(path, nameAndVersion).toString());

        configureGraphHopper(networkSettings.getLinkType(), networkSettings.getProfiles(), graphHopper);

        graphHopper.setAllowWrites(false);
        if (!graphHopper.load()) {
            throw new GraphHopperNotImportedException(
                    NO_NETWORK_MSG.formatted(networkSettings.getNetworkNameAndVersion()));
        }
        return graphHopper;
    }

    /***
     * In order to separate reads and writes to/form disk cache this method only stores a new network on disk.
     * This method is idempotent it will remove an existing network from disk and reimport it.
     * Use this method to load a previously stored network.
     * @param networkSettings Routing network with link supplier
     */
    public <T extends Link> void storeOnDisk(RoutingNetworkSettings<T> networkSettings) {
        requireNonNull(networkSettings.getLinkSupplier(), NO_LINK_SUPPLIER_MSG);
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(networkSettings);
        Path path = requireNonNull(networkSettings.getGraphhopperRootPath(), NO_PATH_MSG_WRITE);
        String nameAndVersion = requireNonNull(networkSettings.getNetworkNameAndVersion(), NO_NAME_MSG_WRITE);
        graphHopper.setGraphHopperLocation(formatNormalizedPath(path, nameAndVersion).toString());
        Map<String, LinkVehicleMapper<T>> providers = linkVehicleMapperProvider.getLinksForType(
                networkSettings.getLinkType());
        validateVehicles(networkSettings.getProfiles(), providers, networkSettings.getLinkType());
        configureGraphHopper(networkSettings.getLinkType(), networkSettings.getProfiles(), graphHopper);
        graphHopper.clean();
        graphHopper.setStoreOnFlush(true);
        graphHopper.importAndClose();
    }

    /**
     * Configures graphhopper by scanning the annotations from the linkClass and configuration encoders and parsers
     * accordingly.
     *
     * @param linkClass The annotated link class
     * @param profiles  The profiles for which we need to encode vehicle information
     * @param <T>       Link class
     */
    private <T extends Link> void configureGraphHopper(Class<T> linkClass, List<Profile> profiles,
            NetworkGraphHopper networkGraphHopper) {
        networkGraphHopper.setElevation(false);

        EncodedValuesByTypeDto<T> encodedValuesByTypeDto = encodedValuesMapper.map(linkClass);
        Map<String, LinkVehicleMapper<T>> providers = linkVehicleMapperProvider.getLinksForType(
                linkClass);
        ProfileAccessAndSpeedAttributes profileAccessAndSpeedAttributes = profileAccessAndSpeedAttributesMapper.map(
                profiles);
        String encodedValuesString = getEncodedValuesString(profileAccessAndSpeedAttributes.getAll(),
                encodedValuesByTypeDto);
        ImportRegistry importRegistry = new LinkImportRegistry<>(encodedValuesByTypeDto,
                encodedValueFactoryRegistry, encodedMapperFactoryRegistry,
                providers,
                new DefaultImportRegistry(),
                profileAccessAndSpeedAttributes);
        addSpeedAndAccessRestrictionsToProfiles(profiles);
        networkGraphHopper.setImportRegistry(importRegistry);
        networkGraphHopper.setEncodedValuesString(encodedValuesString);
        networkGraphHopper.setProfiles(profiles);
        networkGraphHopper.setMinNetworkSize(0);
    }

    private <T extends Link> String getEncodedValuesString(List<String> accessAndSpeedAttributes,
            EncodedValuesByTypeDto<T> encodedValuesByTypeDto) {
        return Stream.concat(
                        encodedValuesByTypeDto.getNetworkEncodedValueNameKeySet().stream(),
                        accessAndSpeedAttributes.stream()
                )
                .collect(Collectors.joining(DELIMITER));
    }


    private <T extends Link> void validateVehicles(List<Profile> profiles, Map<String, LinkVehicleMapper<T>> providers,
            Class<T> linkClass) {
        Set<String> missingVehicles = profiles.stream()
                .map(Profile::getName)
                .filter(vehicle -> !providers.containsKey(vehicle))
                .collect(Collectors.toSet());
        if (!missingVehicles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing LinkVehicle implementations for Link type [%s] and vehicle type(s) [%s]"
                            .formatted(linkClass.getSimpleName(), String.join(", ", missingVehicles)));
        }
    }

    private void addSpeedAndAccessRestrictionsToProfiles(List<Profile> profiles) {
        profiles.forEach(profile -> profile.setCustomModel(customModelMapper.mapToCustomModel(profile)));
    }

}
