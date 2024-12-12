package nu.ndw.nls.routingmapmatcher.network.decoding.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.DirectionalFieldGenericTypeArgumentMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.EncodedValuesMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.decoding.link.NonEncodedValueLinkPropertyRegistry;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncodedValueObjectConstructorRegistry {

    private final DirectionalFieldGenericTypeArgumentMapper directionalFieldGenericTypeArgumentMapper;

    private final NonEncodedValueLinkPropertyRegistry nonEncodedValueLinkPropertyRegistry;

    private final EncodedValuesMapper encodedValuesMapper;

    @SuppressWarnings("java:S6411")
    private final Map<Class<? extends NetworkEncoded>, Constructor<? extends NetworkEncoded>> linkClassToConstructor = new HashMap<>();

    /**
     * Analyses {@link Link} objects to find the most specific constructor for instantiating a new instance when trying
     * to decode network values back into a {@link Link} object. Constructors need to use parameter names that match the name of the member
     * properties, because the constructor parameter name are matched on member property names in order to be able to find the
     * {@link EncodedValue} annotated configuration required for decoding the network values. The only exception to this rule is that
     * constructor parameters can also contain the non-encoded parameters used in {@link Link} objects. Also see
     * {@link NonEncodedValueLinkPropertyRegistry} for the list of non-encoded properties. Constructor resolving results are cached.
     *
     * @param clazz the class to resolve the constructor for
     * @return the constructor that matches the most encoded value parameter to instantiate the clazz instance
     * @param <T> class type
     */
    public <T extends NetworkEncoded> Constructor<T> resolveConstructor(Class<T> clazz) {
        return (Constructor<T>) linkClassToConstructor.computeIfAbsent(clazz, this::findConstructor);
    }

    private <T extends NetworkEncoded> Constructor<T> findConstructor(Class<T> clazz) {
        // Analyse our annotations
        // We need the property names and types to match for finding a property Link constructor
        EncodedValuesByTypeDto<T> encodedValuesByType = encodedValuesMapper.map(clazz);

        // Match constructors. Find the most specific one available of which all properties match with encoded values
        // We may want to cache this in a map, to increase performance for when we re-use this lookup
        return resolveMostSpecificConstructorContainingWithAllEncodedValueParameters(clazz, encodedValuesByType);
    }

    private <T extends NetworkEncoded> Constructor<T> resolveMostSpecificConstructorContainingWithAllEncodedValueParameters(
            Class<T> clazz, EncodedValuesByTypeDto<T> encodedValuesByType) {

        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> allArgumentsAreKnownAsEncodedValues(clazz, c, encodedValuesByType))
                .sorted(Comparator.comparing(constructor -> constructor.getParameters().length))
                .toList();

        log.debug("Found {} constructors that match all encoded value arguments", constructors.size());

        if (CollectionUtils.isEmpty(constructors)) {
            throw new IllegalArgumentException("Failed to find a suitable constructor! Please verify that the " + clazz.getSimpleName() +
                    " constructors arguments contain the default Link property names: " +
                    nonEncodedValueLinkPropertyRegistry.getNonEncodedProperties() + " and that all " +
                    EncodedValue.class.getSimpleName() + " annotated properties are using the same argument names in the constructor: " +
                    encodedValuesByType.getPropertyNameKeySet().stream().sorted().toList());
        }

        return (Constructor<T>) constructors.getLast();
    }

    private boolean allArgumentsAreKnownAsEncodedValues(Class<? extends NetworkEncoded> linkClass, Constructor<?> constructor,
            EncodedValuesByTypeDto<?> encodedValuesByType) {
        Map<Boolean, List<Parameter>> groupedByEncodedValue = Arrays.stream(constructor.getParameters())
                .collect(Collectors.groupingBy(parameter ->
                        constructorParamExistsAsEncodedValue(linkClass, parameter, encodedValuesByType)));

        List<Parameter> notEncodedValues = groupedByEncodedValue.getOrDefault(false, Collections.emptyList());
        List<Parameter> matchedEncodedValues = groupedByEncodedValue.getOrDefault(true, Collections.emptyList());

        log.debug("Matched {} encoded values, missing {}", matchedEncodedValues.size(), notEncodedValues.stream()
                .map(Parameter::getName).toList());

        return notEncodedValues.isEmpty();
    }

    private boolean constructorParamExistsAsEncodedValue(Class<? extends NetworkEncoded> linkClass, Parameter parameter,
            EncodedValuesByTypeDto<?> encodedValuesByType) {

        Class<?> encodedDataClass;
        if (parameter.getType() == DirectionalDto.class) {
            // If directional, we need to get the real data type from the generic type argument
            encodedDataClass = directionalFieldGenericTypeArgumentMapper.map(linkClass, parameter.getName());
        } else {
            encodedDataClass = parameter.getType();
        }

        // Some values are from Link, they are hard coded and do not use annotations
        if (nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty(parameter.getName(), parameter.getType())) {
            log.debug("Found a core {} parameter, that is not using annotations: {}", Link.class.getSimpleName(), parameter.getName());
            return true;
        }

        // verify that this constructor parameter of a certain type matches an annotated encoded value
        return encodedValuesByType.getByProperty( ClassUtils.primitiveToWrapper(encodedDataClass), parameter.getName()).isPresent();
    }

}
