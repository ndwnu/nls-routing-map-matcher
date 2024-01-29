package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.AnnotatedPropertyDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.AnnotatedSource;
import org.springframework.stereotype.Component;


/**
 * Scans a class for an annotation and returns meta information about annotated fields.
 */
@Slf4j
@Component
public class AnnotationMapper {

    /**
     * Resolves the annotation on the annotated class. Annotation can either be resolved from the field itself
     * or the getter method of the property. Getter methods have priority over the field annotations, because they
     * allow overriding parent annotated configurations in child classes.
     *
     * @param annotationClass An annotation with {@link Target} type {@link ElementType#FIELD} and
     *                        {@link ElementType#METHOD} and {@link Retention#value} policy
     *                        {@link RetentionPolicy#RUNTIME}
     * @param annotatedClass Any class that has fields and methods annotated with the annotationClass annotation
     * @throws IllegalStateException when the annotation is found on a field that is not public accessible because it
     *                               has no getter method.
     * @return Lookup map where key is the field name and
     * @param <U> The annotation class type
     */
    public <T, U extends Annotation> Map<String, AnnotatedPropertyDto<T, U>> resolveAnnotation(
            Class<U> annotationClass, Class<T> annotatedClass) {

        Map<String, U> fieldNameToAnnotationMap = createFieldNameToAnnotationMap(annotationClass, annotatedClass);

        Map<String, AnnotatedPropertyDto<T, U>> nameToPropertyMap = new HashMap<>();
        getGetterPropertyDescriptors(annotatedClass).forEach(
                descriptor -> processPropertyDescriptor(annotationClass, fieldNameToAnnotationMap,
                        nameToPropertyMap, descriptor));

        assertNoAnnotationOnFieldWithoutGetterMethod(nameToPropertyMap, fieldNameToAnnotationMap);

        return nameToPropertyMap;
    }

    private <T, U extends Annotation> void processPropertyDescriptor(Class<U> annotationClass,
            Map<String, U> fieldNameToAnnotationMap, Map<String, AnnotatedPropertyDto<T, U>> nameToPropertyMap,
            PropertyDescriptor descriptor) {
        final String propertyName = descriptor.getName();

        final U fieldAnnotation = fieldNameToAnnotationMap.get(propertyName);
        final U getterMethodAnnotation = descriptor.getReadMethod().getAnnotation(annotationClass);

        if (fieldAnnotation == null && getterMethodAnnotation == null) {
            log.trace("No annotation found for property {}", propertyName);
        } else {
            final U annotationValue;
            final AnnotatedSource annotatedSource;

            if (fieldAnnotation != null && getterMethodAnnotation != null) {
                log.trace("Property annotation found on both field: {} and method: {}, prioritizing method "
                          + "annotation, because they can be used for overriding annotations when using "
                          + "inheritance",
                        propertyName, descriptor.getReadMethod().getName());
                annotationValue = getterMethodAnnotation;
                annotatedSource = AnnotatedSource.GETTER_METHOD;
            } else if (fieldAnnotation != null) {
                log.trace("Property annotation found on field: {}", propertyName);
                annotationValue = fieldAnnotation;
                annotatedSource = AnnotatedSource.FIELD;
            } else {
                log.trace("Property annotation found on getter method: {}", descriptor.getReadMethod().getName());

                annotationValue = getterMethodAnnotation;
                annotatedSource = AnnotatedSource.GETTER_METHOD;
            }

            nameToPropertyMap.put(propertyName, AnnotatedPropertyDto.<T, U>builder()
                    .propertyName(propertyName)
                    .annotationValue(annotationValue)
                    .annotatedSource(annotatedSource)
                    .getterMethod(descriptor.getReadMethod())
                    .build());
        }
    }

    private <U, T extends Annotation> void assertNoAnnotationOnFieldWithoutGetterMethod(
            Map<String, AnnotatedPropertyDto<U, T>> nameToAnnotatedPropertyResult,
            Map<String, T> fieldNameToAnnotationMap) {
        if (!nameToAnnotatedPropertyResult.keySet().containsAll(fieldNameToAnnotationMap.keySet())) {
            Set<String> fieldNames = new HashSet<>(fieldNameToAnnotationMap.keySet());
            fieldNames.removeAll(nameToAnnotatedPropertyResult.keySet());
            throw new IllegalStateException("Found field annotations, but field has no getter to access it: Fields: " +
                                            fieldNames);
        }
    }

    private Set<PropertyDescriptor> getGetterPropertyDescriptors(Class<?> annotatedClass) {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(annotatedClass);
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Failed to get properties from annotated class: " +
                                            annotatedClass.getSimpleName(), e);
        }

        return Stream.of(beanInfo.getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
                .collect(Collectors.toSet());
    }

    private <T extends Annotation> Map<String, T> createFieldNameToAnnotationMap(Class<T> annotationClass,
            Class<?> annotatedClass) {
        Map<String, T> map = new HashMap<>();
        getAllFieldsIncludingInherited(annotatedClass)
                .forEach(field -> getOptionalAnnotation(field, annotationClass)
                        .ifPresent(tag -> map.put(field.getName(), tag)));
        return map;
    }

    private <T extends Annotation> Optional<T> getOptionalAnnotation(Field field, Class<T> annotationClass) {
        T[] annotationsByType = field.getAnnotationsByType(annotationClass);
        if (annotationsByType.length == 0) {
            return Optional.empty();
        } else if (annotationsByType.length == 1) {
            return Optional.of(annotationsByType[0]);
        } else {
            throw new IllegalStateException("Field cannot be annotated with more than one Tag annotation");
        }
    }

    private List<Field> getAllFieldsIncludingInherited(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

}
