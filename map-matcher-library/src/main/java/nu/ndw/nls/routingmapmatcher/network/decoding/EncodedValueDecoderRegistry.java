package nu.ndw.nls.routingmapmatcher.network.decoding;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueDistanceDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueGeometryDecoder;
import org.springframework.stereotype.Component;


/**
 * Wires all {@link EncodedValueDecoder} instances that are found in the spring boot context and allows you to
 * look up an {@link EncodedValueDecoder} for a specific java class type. For convenience also adds the distance and geometry decoders.
 */
@Component
public class EncodedValueDecoderRegistry {

    @Getter
    private final EncodedValueDistanceDecoder encodedValueDistanceDecoder;

    @Getter
    private final EncodedValueGeometryDecoder encodedValueGeometryDecoder;

    @SuppressWarnings("java:S6411")
    private final Map<Class<?>, EncodedValueDecoder<?>> classToEncodedValueDecoder;

    public EncodedValueDecoderRegistry(EncodedValueDistanceDecoder encodedValueDistanceDecoder,
            EncodedValueGeometryDecoder encodedValueGeometryDecoder, List<? extends EncodedValueDecoder<?>> encodedValueDecoder) {
        this.encodedValueDistanceDecoder = encodedValueDistanceDecoder;
        this.encodedValueGeometryDecoder = encodedValueGeometryDecoder;
        this.classToEncodedValueDecoder = encodedValueDecoder.stream().collect(
                Collectors.toMap(EncodedValueDecoder::getType, Function.identity()));
    }

    /**
     * Resolves an {@link EncodedValueDecoder} for a specific java type. Uses unchecked cast, but type safety is
     * guaranteed, because the constructor code guarantees that each class key matches the {@link EncodedValueDecoder}
     * type class.
     *
     * @param aClass type
     * @return {@link EncodedValueDecoder} for a specific java type
     * @param <T> type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<EncodedValueDecoder<T>> lookupEncodedValueDecoder(Class<T> aClass) {
        return Optional.ofNullable((EncodedValueDecoder<T>) this.classToEncodedValueDecoder.get(aClass));
    }

}
