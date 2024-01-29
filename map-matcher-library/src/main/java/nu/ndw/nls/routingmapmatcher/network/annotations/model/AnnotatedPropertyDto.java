package nu.ndw.nls.routingmapmatcher.network.annotations.model;

import java.lang.reflect.Method;
import lombok.Builder;

/**
 *
 * @param propertyName Name of the property
 * @param getterMethod Method for obtaining the property value
 * @param annotationValue The annotation instance as obtained from the configuration
 * @param annotatedSource Which annotation is used for this configuration: field or getter
 * @param <T> The class from which this property is obtained. It is not used in this class, because Method is not a
 *           type safe type. Having it specified here allows you to restrict using the getterMethod in a type safe way.
 * @param <U> Return type of the getter method
 */
@Builder
@SuppressWarnings("java:S2326")
public record AnnotatedPropertyDto<T, U>(String propertyName, Method getterMethod, U annotationValue,
                                      AnnotatedSource annotatedSource) {
}
