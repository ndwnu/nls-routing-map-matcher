package nu.ndw.nls.routingmapmatcher.network.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface EncodedValue {

    /**
     * The tag key under which this value is stored
     * @return
     */
    String key();

    /**
     * When not 0, this is used as the amount of bits. Otherwise a sensible default is used based on the property
     * java type
     * @return
     */
    int bits() default 0;



}
