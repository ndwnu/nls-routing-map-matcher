package nu.ndw.nls.routingmapmatcher.network.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import nu.ndw.nls.routingmapmatcher.network.model.Link;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface EncodedValue {

    /**
     * The tag key under which this value is stored into the graph hopper network
     * @return key value
     */
    String key();

    /**
     * The bits attribute is used during the encoding process, to configure how much bits are used to store a {@link Link} annotated value.
     * It is ignored when used on non link DTO's when used during the decoding process.
     * When not 0, this is used as the amount of bits for numeric values int and long or else a sensible default is used based on the
     * property java type. For Strings this value contains the maximum expected amount of unique different values that will be used in this
     * network.
     *
     * @return bits
     */
    int bits() default 0;



}
