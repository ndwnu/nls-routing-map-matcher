package nu.ndw.nls.routingmapmatcher.network.annotations;

import nu.ndw.nls.routingmapmatcher.network.model.Link;

/**
 * Marker interface for classes that use the {@link EncodedValue} annotation for "encoding to" and "decoding from" GraphHopper networks.
 * A lot of code written around parsing and using the {@link EncodedValue} annotation use reflection and have no restrictions on the objects
 * passed as an argument. When encoding data into a GraphHopper network, we always use the {@link Link} class as base object, but when we
 * decode a selection of network values back into a custom DTO there is no class restriction. To avoid a lot of code from having 'Object'
 * arguments, we mark them with the {@link NetworkEncoded} interface, to give a better context of which kind of parameter we actually
 * expect.
 */
public interface NetworkEncoded {

}
