package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

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
    public <T extends Link> Class<?> map(Class<T> annotatedClassType, String fieldName) {

        Field directionalField;

        try {
            directionalField = annotatedClassType.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Direction field by name " + fieldName + " should exist!", e);
        }

        return (Class<?>) ((ParameterizedType) directionalField.getGenericType()).getActualTypeArguments()[0];
    }

}
