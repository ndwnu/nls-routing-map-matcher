package nu.ndw.nls.routingmapmatcher.network.annotations.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import nu.ndw.nls.routingmapmatcher.network.model.Link;

/**
 * It is not possible to create a map that uses a class key to generic type using that class. To overcome this
 * limitation, we use a variant of Joshua Bloch's typesafe heterogeneous container pattern. The add method enforces
 * that the class key matches the {@link EncodedValueDto } R argument, therefore values retrieved from the map
 * by using a class key can cast back to the type safe generic type argument using that class.
 * @param <T> Link extending class
 */
public class EncodedValuesByTypeDto<T extends Link> {

    private final Map<Class<?>, Map<String, EncodedValueDto<T, ?>>> typeToEncodedValues = new HashMap<>();

    private final Map<String, Class<?>> keyToValueClass = new HashMap<>();

    public <R> void add(Class<R> valueTypeClass, EncodedValueDto<T, R> encodedValueDto) {
        typeToEncodedValues.computeIfAbsent(valueTypeClass, aClass -> new HashMap<>());
        typeToEncodedValues.get(valueTypeClass).put(encodedValueDto.key(), encodedValueDto);
        keyToValueClass.put(encodedValueDto.key(), encodedValueDto.valueType());
    }
    @SuppressWarnings("unchecked")
    public <R> EncodedValueDto<T, R> get(Class<R> valueTypeClass, String key) {
        return Optional.ofNullable(typeToEncodedValues.get(valueTypeClass)
                .get(key))
                .map(dto -> (EncodedValueDto<T, R>)dto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find value by class %s and key: %s".formatted(valueTypeClass, key)));
    }

    public Optional<Class<?>> getValueTypeByKey(String key) {
        return Optional.ofNullable(keyToValueClass.get(key));
    }

    public Set<String> keySet() {
        return keyToValueClass.keySet();
    }


}
