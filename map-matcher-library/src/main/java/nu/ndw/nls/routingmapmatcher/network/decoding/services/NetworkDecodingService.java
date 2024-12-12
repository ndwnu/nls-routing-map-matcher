package nu.ndw.nls.routingmapmatcher.network.decoding.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.DirectionalFieldGenericTypeArgumentMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.EncodedValuesMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.decoding.EncodedValueDecoderRegistry;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueDistanceDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueGeometryDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.link.NonEncodedValueLinkPropertyRegistry;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.apache.commons.lang3.ClassUtils;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkDecodingService {

    private static final String DISTANCE_IN_METERS = "distanceInMeters";

    private static final String GEOMETRY = "geometry";

    private final EncodedValuesMapper encodedValuesMapper;

    private final EncodedValueObjectConstructorRegistry encodedValueObjectConstructorRegistry;

    private final NonEncodedValueLinkPropertyRegistry nonEncodedValueLinkPropertyRegistry;

    private final DirectionalFieldGenericTypeArgumentMapper directionalFieldGenericTypeArgumentMapper;

    private final EncodedValueDecoderRegistry encodedValueDecoderRegistry;

    @SuppressWarnings("java:S6411")
    private final Map<Class<?>, LinkConstructor<?>> linkDtoConstructors = new HashMap<>();

    /**
     * Configuration for a {@link Link} related object required to construct it and to get parameters from a network
     *
     * @param <T>
     */
    @Getter
    @Builder
    @RequiredArgsConstructor
    private static class LinkConstructor<T> {
        private final Constructor<T> constructor;

        private final List<ConstructorArgumentSupplier> constructorArgumentSuppliers;
    }

    private interface ConstructorArgumentSupplier {
        Object decode(NetworkGraphHopper networkGraphHopper, Long roadSectionId);
    }

    @Builder
    private static class GeometryValueConstructorArgumentSupplier implements ConstructorArgumentSupplier {

        private final EncodedValueGeometryDecoder encodedValueGeometryDecoder;

        @Override
        public Object decode(NetworkGraphHopper networkGraphHopper, Long roadSectionId) {
            return encodedValueGeometryDecoder.decode(networkGraphHopper, roadSectionId);
        }
    }

    @Builder
    private static class DistanceValueConstructorArgumentSupplier implements ConstructorArgumentSupplier {

        private final EncodedValueDistanceDecoder encodedValueDistanceDecoder;

        @Override
        public Object decode(NetworkGraphHopper networkGraphHopper, Long roadSectionId) {
            return encodedValueDistanceDecoder.decode(networkGraphHopper, roadSectionId);
        }
    }

    /**
     * Class for supplying default values for {@link Link} related constructor parameters that are not encoded from the network
     */
    @Builder
    private static class DefaultValueConstructorArgumentSupplier implements ConstructorArgumentSupplier {
        private final Object defaultValue;
        public DefaultValueConstructorArgumentSupplier(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object decode(NetworkGraphHopper networkGraphHopper, Long roadSectionId) {
            return defaultValue;
        }
    }

    /**
     * Class for supplying decoded values from a network for {@link Link} related constructor parameters
     */
    @Builder
    @RequiredArgsConstructor
    private static class DecodedValueConstructorArgumentSupplier implements ConstructorArgumentSupplier {

        private final EncodedValueDecoder<?> encodedValueDecoder;
        private final String encodedValueKey;
        private final boolean isDirectional;

        @Override
        public Object decode(NetworkGraphHopper networkGraphHopper, Long roadSectionId) {
            Object forwardValue = encodedValueDecoder.decode(networkGraphHopper, roadSectionId, encodedValueKey, false);

            if (isDirectional) {
                // Also retrieve the reverse value
                Object reverseValue = encodedValueDecoder.decode(networkGraphHopper, roadSectionId, encodedValueKey, true);
                return new DirectionalDto<>(forwardValue, reverseValue);
            } else {
                return forwardValue;
            }
        }
    }

    /**
     * Decodes a road section back into a {@link NetworkEncoded} bean, which can be a simple java DTO that can contain one or more network
     * encoded properties or the entire {@link Link} object. In both cases, a constructor should be available of which all the
     * parameters names match the encoded properties names. The most specific constructor is used to instantiate the object.
     * When decoding a {@link Link} object, the required fromNodeId and toNodeId properties are not decoded, because they are not encoded
     * in the network, a default value of 0 is used to initialize them.
     * When decoding into a custom DTO, the properties need to be annotated with {@link EncodedValue} of which only the
     * {@link EncodedValue#key()} is used to know which network property matches which java property.
     * Properties distanceInMeters (double) and geometry (LineString) are core parameters that are not encoded into the network as
     * additional properties using the {@link EncodedValue} annotation and therefor also do not need to be annotated with
     * {@link EncodedValue}. When you want to decode these properties, they need to match the name and type in your DTOs constructor
     * arguments.
     *
     * @param networkGraphHopper The graphhopper network
     * @param roadSectionId The road section id
     * @param clazz The class to decode back into
     * @return Instance of {@link T} with all the network decoded properties and dummy values for all non network encoded
     *    {@link Link} parameters: fromNodeId, toNodeId
     * @param <T> Link class type
     */
    public <T extends NetworkEncoded> T decodeByRoadSectionId(NetworkGraphHopper networkGraphHopper, long roadSectionId,
            Class<T> clazz) {
        // Because we store the link constructor inside a map, we cannot get it back type safe and need to cast.
        LinkConstructor<T> linkConstructor = (LinkConstructor<T>)linkDtoConstructors.computeIfAbsent(clazz,
                aClass -> resolveLinkObjectConstructorArgumentSuppliers(clazz));

        // Instantiate a new Link object
        return createInstance(linkConstructor, networkGraphHopper, roadSectionId);
    }

    /**
     * Given a {@link Link} clazz, resolves the most accurate constructor to create the Link object with. Then analyses how to obtain the
     * values for all the constructor arguments and returns the outcome as {@link LinkConstructor}
     *
     * @param clazz Link class
     * @return {@link LinkConstructor} with constructor and constructor parameter resolvers
     * @param <T> Link class type
     */
    private <T extends NetworkEncoded> LinkConstructor<T> resolveLinkObjectConstructorArgumentSuppliers(Class<T> clazz) {
        // Match constructors. Find the most specific one available of which all properties match with encoded values
        // We may want to cache this in a map, to increase performance for when we re-use this lookup
        Constructor<T> constructor = encodedValueObjectConstructorRegistry.resolveConstructor(clazz);

        // Analyse our annotations
        // We need the property names and types to match for finding a property Link constructor
        EncodedValuesByTypeDto<T> encodedValuesByType = encodedValuesMapper.map(clazz);

        List<ConstructorArgumentSupplier> constructorArgumentSuppliers = Arrays.stream(constructor.getParameters())
                .map(parameter -> resolveParameterValue(clazz, encodedValuesByType, parameter))
                .toList();

        return LinkConstructor.<T>builder()
                .constructor(constructor)
                .constructorArgumentSuppliers(constructorArgumentSuppliers)
                .build();
    }

    /**
     * Obtains all the parameters required for constructing the {@link Link} object and uses reflection to construct the instance
     *
     * @param linkConstructor contains information about the constructor and how to get the parameters
     * @param networkGraphHopper the graphhopper network instance
     * @param roadSectionId a road section id to decode
     * @return Link instance containing the network decoded values and default dummy values for all other non-encoded values.
     * @param <T> Link type
     */
    private <T extends NetworkEncoded> T createInstance(LinkConstructor<T> linkConstructor, NetworkGraphHopper networkGraphHopper,
            long roadSectionId) {

        Object[] constructorArguments = linkConstructor.getConstructorArgumentSuppliers()
                .stream()
                .map(constructorArgumentSupplier -> constructorArgumentSupplier.decode(networkGraphHopper,
                        roadSectionId))
                .toArray();

        try {
            return linkConstructor.getConstructor().newInstance(constructorArguments);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to instantiate object", e);
        }
    }


    /**
     * This method figures out how to obtain a value for a link constructor parameter. It will return a {@link ConstructorArgumentSupplier}
     * that either knows how to decode the parameter from the network or returns a default value for {@link Link} parameters that are
     * not encoded on the network, but are still required in the constructor as parameter.
     *
     * @param parameter Constructor parameter
     * @param linkClass Link Class, required for type argument resolving from DirectionalDto arguments if the parameter is an encoded value
     * @param encodedValuesByType Registry for looking up encoded values, required if the parameter is an encoded value
     * @return ConstructorArgumentSupplier capable of returning the constructor parameter value
     * @param <T> The link object type
     */
    @SuppressWarnings("java:S1142")
    private <T extends NetworkEncoded> ConstructorArgumentSupplier resolveParameterValue(Class<T> linkClass,
            EncodedValuesByTypeDto<T> encodedValuesByType, Parameter parameter) {

        if (parameter.getName().equals(DISTANCE_IN_METERS) && parameter.getType() == double.class) {
            // Distance is a special property with a dedicated retrieval method
            return DistanceValueConstructorArgumentSupplier.builder()
                    .encodedValueDistanceDecoder(encodedValueDecoderRegistry.getEncodedValueDistanceDecoder())
                    .build();
        } else if (parameter.getName().equals(GEOMETRY) && parameter.getType() == LineString.class) {
            // Geometry is a special property with a dedicated retrieval method
            return GeometryValueConstructorArgumentSupplier.builder()
                    .encodedValueGeometryDecoder(encodedValueDecoderRegistry.getEncodedValueGeometryDecoder())
                    .build();
        } else if (nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(parameter.getName(), parameter.getType())) {
            // ConstructorArgumentSupplier will always return a hard coded default value for constructor parameters that are not encoded
            // into the network
            return DefaultValueConstructorArgumentSupplier.builder()
                    .defaultValue(nonEncodedValueLinkPropertyRegistry.getNonEncodedPropertyDefaultValue(parameter.getName()))
                    .build();
        }

        return createConstructorArgumentDecoder(parameter, linkClass, encodedValuesByType);
    }

    /**
     * This method figures out how to decode a value for a link constructor parameter
     *
     * @param parameter Constructor parameter
     * @param linkClass Link Class, required for type argument resolving from DirectionalDto arguments
     * @param encodedValuesByType Registry for looking up encoded values
     * @return ConstructorArgumentSupplier capable of decoding graphhopper network properties by supplying the network and
     *      road section id
     * @param <T> The link object type
     */
    private <T extends NetworkEncoded> ConstructorArgumentSupplier createConstructorArgumentDecoder(Parameter parameter, Class<T> linkClass,
            EncodedValuesByTypeDto<T> encodedValuesByType) {
        Class<?> encodedDataClass;
        if (parameter.getType() == DirectionalDto.class) {
            // If directional, we need to get the real data type from the generic type argument
            encodedDataClass = directionalFieldGenericTypeArgumentMapper.map(linkClass, parameter.getName());
        } else {
            encodedDataClass = parameter.getType();
        }

        Class<?> boxedEncodedDataClass = ClassUtils.primitiveToWrapper(encodedDataClass);

        // Lookup the decoder to use based on our type
        EncodedValueDecoder<?> encodedValueDecoder = encodedValueDecoderRegistry.lookupEncodedValueDecoder(boxedEncodedDataClass)
                .orElseThrow(() -> new IllegalArgumentException("Failed to find encoded value decoder for java type: " +
                        boxedEncodedDataClass));

        // Verify if this is a directional value
        EncodedValueDto<?, ?> encodedValueDto = encodedValuesByType.getByProperty(boxedEncodedDataClass, parameter.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No encoded value for type: " + boxedEncodedDataClass + " and name: " + parameter.getName() +
                                " found!"));

        return DecodedValueConstructorArgumentSupplier.builder()
                .encodedValueDecoder(encodedValueDecoder)
                .encodedValueKey(encodedValueDto.key())
                .isDirectional(encodedValueDto.isDirectional())
                .build();
    }

}
