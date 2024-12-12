package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.typesafety.Typed;

/**
 * Factory that creates instances of {@link AbstractEncodedMapper}
 * @param <T> The link field type of which the data is retrieved
 */
public interface EncodedMapperFactory<T> extends Typed<T> {

    Class<T> getType();

    <U extends NetworkEncoded> AbstractEncodedMapper<U, T> create(EncodedValueLookup lookup,
            EncodedValueDto<U, T> encodedValueDto);

}
