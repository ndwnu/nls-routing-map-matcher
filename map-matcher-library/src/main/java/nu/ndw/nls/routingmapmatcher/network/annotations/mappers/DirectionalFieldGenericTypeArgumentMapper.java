package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * Retrieves the generic type argument from a field of type {@link DirectionalDto}
 */
@Component
public class DirectionalFieldGenericTypeArgumentMapper {

    /**
     * Retrieves the generic type argument from a field of type {@link DirectionalDto}.
     *
     * @param annotatedClassType A link extending class that contains the field that is of type {@link DirectionalDto}
     * @param fieldName          The name of the field that is of type {@link DirectionalDto}
     * @param <T>                Link class that contains the {@link DirectionalDto} field
     * @return {@link Class } which is the generic type argument of {@link DirectionalDto}
     */
    public <T extends NetworkEncoded> Class<?> map(Class<T> annotatedClassType, String fieldName) {
        Field directionalField = getDirectionalField(annotatedClassType, fieldName);

        return (Class<?>) ((ParameterizedType) directionalField.getGenericType()).getActualTypeArguments()[0];
    }

    private static <T extends NetworkEncoded> Field getDirectionalField(Class<T> clazz, String fieldName) {
        return Optional.ofNullable(ReflectionUtils.findField(clazz, fieldName))
                .orElseThrow(() -> new IllegalArgumentException("No such field %s in %s".formatted(fieldName, clazz)));
    }
}
