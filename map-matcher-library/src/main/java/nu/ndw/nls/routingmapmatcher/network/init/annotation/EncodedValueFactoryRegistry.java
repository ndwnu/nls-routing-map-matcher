package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedValueFactory;
import org.springframework.stereotype.Component;

/**
 * Registry for looking up an {@link EncodedValueFactory} by {@link Class} type.
 */
@Component
public class EncodedValueFactoryRegistry {

    private final Map<Class<?>, EncodedValueFactory<?>> classToEncodedValueFactory;

    public EncodedValueFactoryRegistry(List<? extends EncodedValueFactory<?>> annotatedEncoderFactories) {
        this.classToEncodedValueFactory = annotatedEncoderFactories.stream().collect(
                Collectors.toMap(EncodedValueFactory::getType, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    public <R> Optional<EncodedValueFactory<R>> lookupEncodedValueFactory(Class<R> aClass) {
        return Optional.ofNullable((EncodedValueFactory<R>) this.classToEncodedValueFactory.get(aClass));
    }

}
