package nu.ndw.nls.routingmapmatcher.network.annotations.model;

import java.util.function.Function;
import lombok.Builder;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;

/**
 * Meta information about {@link Link } fields that are annotated with {@link EncodedValue}. Describes which fields
 * need to be encoded into a graphhopper network and how the encoding should be performed.
 *
 * @param propertyName property name associated with the encoded key.
 * @param key key used to encode the value into graphhopper
 * @param bits the amount of bits as read from the {@link EncodedValue}. Only applies for graphhopper encoders that
 *             offer specifying the amount of bits, like for the integer encoder.
 * @param valueType the java return type as read from the getter method for non-directional fields or from the field
 *                  {@link DirectionalDto } type argument when using a directional field.
 * @param valueSupplier the forward value supplier that takes T link as argument and produces the R result.
 * @param valueReverseSupplier the reverse value supplier that takes T link as argument and produces the R result. This
 *                             field is null if this {@link EncodedValueDto} is not describing a directional value
 *                             (a value stored in {@link DirectionalDto } )
 * @param <T> The Link extending class of which we describe a field
 * @param <R> The return type of the getter method. If this is a directional field, then the return type is acquired
 *           from the {@link DirectionalDto } field generic type argument and it is assumed that the getter returns
 *           the same type.
 */
@Builder
public record EncodedValueDto<T extends NetworkEncoded, R>(String propertyName, String key, int bits, Class<R> valueType,
                                                           Function<T, R> valueSupplier, Function<T, R> valueReverseSupplier) {

    /**
     * @return true if this instance is describing a directional field
     */
    public boolean isDirectional() {
        return valueReverseSupplier != null;
    }
}
