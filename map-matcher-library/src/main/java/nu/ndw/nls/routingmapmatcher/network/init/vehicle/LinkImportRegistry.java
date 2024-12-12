package nu.ndw.nls.routingmapmatcher.network.init.vehicle;

import com.graphhopper.routing.ev.DefaultImportRegistry;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.ImportRegistry;
import com.graphhopper.routing.ev.ImportUnit;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.util.PMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedMapperFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedValueFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedValueFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers.LinkAccessParser;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers.LinkAverageSpeedParser;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.ProfileAccessAndSpeedAttributes;

@RequiredArgsConstructor
public class LinkImportRegistry<T extends Link> implements ImportRegistry {

    private static final int DEFAULT_SPEED_BITS = 5;

    private final EncodedValuesByTypeDto<T> encodedValuesByTypeDto;
    private final EncodedValueFactoryRegistry encodedValueFactoryRegistry;
    private final EncodedMapperFactoryRegistry classToEncodedMapperFactory;
    private final Map<String, LinkVehicleMapper<T>> linkMappersByVehicleType;
    private final DefaultImportRegistry defaultImportRegistry;
    private final ProfileAccessAndSpeedAttributes profileAccessAndSpeedAttributes;

    @Override
    public ImportUnit createImportUnit(String attributeName) {
        return getAttributeDataType(attributeName)
                .map(dataType -> ImportUnit.create(attributeName,
                        props -> createEncodedValueForAttributeType(dataType, attributeName),
                        createTagParserFunction(attributeName, dataType)))
                // The gh framework always encodes some extra internal attributes on the network
                // here the fallback delegates to the gh import registry
                .orElseGet(() -> defaultImportRegistry.createImportUnit(attributeName));
    }

    private Optional<Class<?>> getAttributeDataType(String attributeName) {
        if (profileAccessAndSpeedAttributes.isSpeedAttribute(attributeName)) {
            return Optional.of(Double.class);
        } else if (profileAccessAndSpeedAttributes.isAccessAttribute(attributeName)) {
            return Optional.of(Boolean.class);
        } else {
            return encodedValuesByTypeDto.getValueTypeByKey(attributeName);
        }
    }

    private BiFunction<EncodedValueLookup, PMap, TagParser> createTagParserFunction(String attributeName,
            Class<?> type) {
        if (profileAccessAndSpeedAttributes.isSpeedAttribute(attributeName)) {
            return (lookup, props) -> createSpeedTagParserForAttributeType(lookup, attributeName);
        }
        if (profileAccessAndSpeedAttributes.isAccessAttribute(attributeName)) {
            return (lookup, props) -> createAccessibleTagParserForAttributeType(lookup, attributeName);
        }
        return (lookup, props) -> createTagParserForAttributeType(lookup, type, attributeName);
    }

    private TagParser createSpeedTagParserForAttributeType(EncodedValueLookup lookup, String attributeName) {
        String vehicleName = profileAccessAndSpeedAttributes.getVehicleName(attributeName);
        return new LinkAverageSpeedParser<>(lookup, vehicleName, getMapperForAttribute(attributeName));
    }

    private TagParser createAccessibleTagParserForAttributeType(EncodedValueLookup lookup,
            String attributeName) {
        String vehicleName = profileAccessAndSpeedAttributes.getVehicleName(attributeName);
        return new LinkAccessParser<>(lookup, vehicleName, getMapperForAttribute(attributeName));
    }

    private <U> EncodedValue createEncodedValueForAttributeType(Class<U> type, String name) {
        String vehicleName = profileAccessAndSpeedAttributes.getVehicleName(name);
        if (profileAccessAndSpeedAttributes.isSpeedAttribute(name)) {
            return VehicleSpeed.create(vehicleName, DEFAULT_SPEED_BITS, DEFAULT_SPEED_BITS, true);
        } else if (profileAccessAndSpeedAttributes.isAccessAttribute(name)) {
            return VehicleAccess.create(vehicleName);
        }

        EncodedValueDto<T, U> encodedValueDto = encodedValuesByTypeDto.getByKey(type, name)
                .orElseThrow(() -> new IllegalStateException("Failed to find encoded value by type '%s' and name '%s'"
                        .formatted(type.getSimpleName(), name)));

        EncodedValueFactory<U> encodedValueFactory =
                encodedValueFactoryRegistry.lookupEncodedValueFactory(type)
                        .orElseThrow(() -> new IllegalStateException(
                                "No encoded value factory found for name: %s with type: %s".formatted(name, type)));

        return encodedValueFactory.encode(encodedValueDto);
    }

    private <U> AbstractEncodedMapper<T, U> createTagParserForAttributeType(EncodedValueLookup lookup,
            Class<U> valueType,
            String name) {
        EncodedMapperFactory<U> encodedMapperFactory =
                classToEncodedMapperFactory.lookupEncodedMapperFactory(valueType)
                        .orElseThrow(() -> new IllegalStateException(
                                "No tag parser found for name: %s with type: %s".formatted(name, valueType)));

        return encodedMapperFactory.create(lookup, encodedValuesByTypeDto.getByKey(valueType, name)
                .orElseThrow(() -> new IllegalStateException("Failed to find encoded value by type '%s' and name '%s'"
                        .formatted(valueType.getSimpleName(), name))));
    }

    private LinkVehicleMapper<T> getMapperForAttribute(String attributeName) {
        String vehicleName = profileAccessAndSpeedAttributes.getVehicleName(attributeName);
        if (linkMappersByVehicleType.containsKey(vehicleName)) {
            return linkMappersByVehicleType.get(vehicleName);
        } else {
            throw new IllegalArgumentException("No link mapper found for name: %s".formatted(vehicleName));
        }
    }
}
