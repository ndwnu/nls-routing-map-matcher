package nu.ndw.nls.routingmapmatcher.network.annotations.model;

/**
 * Describes the origin of the annotation, which can be useful for analysing problems when a field annotation is
 * overridden with a getter annotation.
 */
public enum AnnotatedSource {
    FIELD,
    GETTER_METHOD;
}
