package nu.ndw.nls.routingmapmatcher.network.model;

import lombok.Builder;

/**
 * The {@link DirectionalDto} class is a wrapper class that can be used for properties that have a forward and reverse
 * direction. The {@link DirectionalDto#forward} and {@link DirectionalDto#reverse} values are encoded on the link as separate
 * values post fixed with respectively :forward and :reverse.
 *
 * @param forward value will be encoded with postfix :forward
 * @param reverse value will be encoded with postfix :reverse
 * @param <T> value that has an encoder
 */
@Builder
public record DirectionalDto<T>(T forward, T reverse) {
}
