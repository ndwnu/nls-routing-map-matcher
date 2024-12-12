package nu.ndw.nls.routingmapmatcher.network.annotations.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;

/**
 * It is not possible to create a map that uses a class key to generic type using that class. To overcome this
 * limitation, we use a variant of Joshua Bloch's typesafe heterogeneous container pattern. The add method enforces
 * that the class key matches the {@link EncodedValueDto } R argument, therefore values retrieved from the map
 * by using a class key can cast back to the type safe generic type argument using that class.
 * @param <T> Link extending class
 */
public class EncodedValuesByTypeDto<T extends NetworkEncoded> {

    @SuppressWarnings("java:S6411")
    private final Map<Class<?>, Map<String, EncodedValueDto<T, ?>>> typeToKeyToEncodedValues = new HashMap<>();

    @SuppressWarnings("java:S6411")
    private final Map<Class<?>, Map<String, EncodedValueDto<T, ?>>> typeToPropertyToEncodedValues = new HashMap<>();

    private final Map<String, Class<?>> keyToValueClass = new HashMap<>();

    private final Map<String, Class<?>> propertyNameToValueClass = new HashMap<>();

    public <R> void add(Class<R> valueTypeClass, EncodedValueDto<T, R> encodedValueDto) {
        typeToKeyToEncodedValues.computeIfAbsent(valueTypeClass, aClass -> new HashMap<>());
        typeToKeyToEncodedValues.get(valueTypeClass).put(encodedValueDto.key(), encodedValueDto);

        typeToPropertyToEncodedValues.computeIfAbsent(valueTypeClass, aClass -> new HashMap<>());
        typeToPropertyToEncodedValues.get(valueTypeClass).put(encodedValueDto.propertyName(), encodedValueDto);

        keyToValueClass.put(encodedValueDto.key(), encodedValueDto.valueType());
        propertyNameToValueClass.put(encodedValueDto.propertyName(), encodedValueDto.valueType());
    }
    @SuppressWarnings("unchecked")
    public <R> Optional<EncodedValueDto<T, R>> getByKey(Class<R> valueTypeClass, String key) {
        return Optional.ofNullable(typeToKeyToEncodedValues.get(valueTypeClass).get(key))
                .map(dto -> (EncodedValueDto<T, R>)dto);
    }

    @SuppressWarnings("unchecked")
    public <R> Optional<EncodedValueDto<T, R>> getByProperty(Class<R> valueTypeClass, String property) {
        return Optional.ofNullable(typeToPropertyToEncodedValues.get(valueTypeClass))
                .map(propertyNameToEncodedValue -> propertyNameToEncodedValue.get(property))
                .map(dto -> (EncodedValueDto<T, R>) dto);
    }

    public Optional<Class<?>> getValueTypeByKey(String key) {
        return Optional.ofNullable(keyToValueClass.get(key));
    }

    public Set<String> getNetworkEncodedValueNameKeySet() {
        return keyToValueClass.keySet();
    }

    public Set<String> getPropertyNameKeySet() {
        return propertyNameToValueClass.keySet();
    }

}
