package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedEncodedMapperFactoryRegistryTest {

    @Mock
    private EncodedMapperFactory<Boolean> booleanEncodedMapperFactory;

    @Mock
    private EncodedMapperFactory<String> stringEncodedMapperFactory;

    @Test
    void lookupEncodedMapperFactory_ok_correctFactoryForEachType() {
        when(booleanEncodedMapperFactory.getType()).thenReturn(Boolean.class);
        when(stringEncodedMapperFactory.getType()).thenReturn(String.class);

        List<? extends EncodedMapperFactory<?>> encodedMapperFactories =
                List.of(booleanEncodedMapperFactory, stringEncodedMapperFactory);

        EncodedMapperFactoryRegistry encodedMapperFactoryRegistry =
                new EncodedMapperFactoryRegistry(encodedMapperFactories);

        assertEquals(Optional.of(booleanEncodedMapperFactory),
                encodedMapperFactoryRegistry.lookupEncodedMapperFactory(Boolean.class));

        assertEquals(Optional.of(stringEncodedMapperFactory),
                encodedMapperFactoryRegistry.lookupEncodedMapperFactory(String.class));

        assertEquals(Optional.empty(),
                encodedMapperFactoryRegistry.lookupEncodedMapperFactory(Integer.class));
    }

    @Test
    void lookupEncodedMapperFactory_ok_emptyOptionalWhenThereIsNoFactoryForType() {
       EncodedMapperFactoryRegistry encodedMapperFactoryRegistry =
                new EncodedMapperFactoryRegistry(Collections.emptyList());

        assertEquals(Optional.empty(),
                encodedMapperFactoryRegistry.lookupEncodedMapperFactory(Integer.class));
    }
}