package nu.ndw.nls.routingmapmatcher.network.model;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
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

    /**
     * Constructor that can be used when both forward and reverse have the same value
     * @param value for both directions
     */
    public DirectionalDto(T value) {
        this(value, value);
    }

    /**
     * Maps the internal type from one to another value type using a function that knows how to convert the value
     * @param function function capable of mapping the original value into a different type
     * @return new DirectionalDto containing the new values
     * @param <U> target type
     */
    public <U> DirectionalDto<U> map(Function<? super T, ? extends U> function) {
        return DirectionalDto.<U>builder()
                .forward(function.apply(forward))
                .reverse(function.apply(reverse))
                .build();
    }

    /**
     * Reduces this {@link DirectionalDto} with another {@link DirectionalDto} by applying the accumulator on each
     * individual direction
     *
     * @param dto another {@link DirectionalDto}
     * @param accumulator accumulator operation to perform on each two individual directions
     * @return new {@link DirectionalDto} with the result of the accumulation
     */
    public DirectionalDto<T> reduce(DirectionalDto<T> dto, BinaryOperator<T> accumulator) {
        return new DirectionalDto<>(accumulator.apply(this.forward, dto.forward),
                                    accumulator.apply(this.reverse, dto.reverse));
    }

    /**
     * Reduces this {@link DirectionalDto}  by applying the accumulator on both values
     *
     * @param accumulator accumulator operation to perform on each two individual directions
     * @return new {@link DirectionalDto} with the result of the accumulation
     */
    public T reduce(BinaryOperator<T> accumulator) {
        return accumulator.apply(forward, reverse);
    }

    /**
     * Performs equals on forward and reverse
     * @return true if forward and reverse are equal
     */
    public boolean isEqualForBothDirections() {
        return Objects.equals(forward, reverse);
    }
}
