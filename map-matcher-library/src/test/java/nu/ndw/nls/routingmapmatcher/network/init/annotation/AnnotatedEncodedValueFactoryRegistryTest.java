package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedValueFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedEncodedValueFactoryRegistryTest {

    @Mock
    private EncodedValueFactory<String> encodedValueFactoryString;

    @Mock
    private EncodedValueFactory<Boolean> encodedValueFactoryBoolean;

    @Test
    void lookupEncodedValueFactory_ok_allTypesResolve() {

        when(encodedValueFactoryString.getType()).thenReturn(String.class);
        when(encodedValueFactoryBoolean.getType()).thenReturn(Boolean.class);

        EncodedValueFactoryRegistry encodedValueFactoryRegistry = new EncodedValueFactoryRegistry(
                List.of(encodedValueFactoryString, encodedValueFactoryBoolean));

        assertEquals(Optional.of(encodedValueFactoryString),
                encodedValueFactoryRegistry.lookupEncodedValueFactory(String.class));


        assertEquals(Optional.of(encodedValueFactoryBoolean),
                encodedValueFactoryRegistry.lookupEncodedValueFactory(Boolean.class));
    }

    @Test
    void lookupEncodedValueFactory_ok_noFactoryForType() {

        EncodedValueFactoryRegistry encodedValueFactoryRegistry = new EncodedValueFactoryRegistry(
                Collections.emptyList());

        assertEquals(Optional.empty(),
                encodedValueFactoryRegistry.lookupEncodedValueFactory(String.class));

    }
}