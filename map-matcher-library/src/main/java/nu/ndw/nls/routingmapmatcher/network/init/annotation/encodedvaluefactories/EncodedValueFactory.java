package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import com.graphhopper.routing.ev.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.typesafety.Typed;

/**
 * Factory that creates an EncodedValue instance
 * @param <T> The link field type of which the data is encoded.
 */
public interface EncodedValueFactory<T> extends Typed<T> {

    Class<T> getType();
    <U extends Link> EncodedValue encode(EncodedValueDto<U, T> encodedValueDto);

}
