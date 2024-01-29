package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.util.PMap;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedValueFactory;
import nu.ndw.nls.routingmapmatcher.network.model.Link;


/**
 * A GraphHopper {@link com.graphhopper.routing.ev.EncodedValueFactory} that uses a list of available {@link EncodedValueFactory} to
 * instantiate encoders for annotated values described by {@link EncodedValuesByTypeDto }
 * @param <T> Type safe link
 */
@Slf4j
public class AnnotatedEncodedValueFactory<T extends Link> implements com.graphhopper.routing.ev.EncodedValueFactory {

    private final EncodedValueFactoryRegistry encodedValueFactoryRegistry;
    private final EncodedValuesByTypeDto<T> encodedValuesByTypeDto;

    public AnnotatedEncodedValueFactory(EncodedValueFactoryRegistry encodedValueFactoryRegistry,
            EncodedValuesByTypeDto<T> encodedValuesByTypeDto) {
        this.encodedValueFactoryRegistry = encodedValueFactoryRegistry;
        this.encodedValuesByTypeDto = encodedValuesByTypeDto;
    }

    @Override
    public EncodedValue create(String name, PMap properties) {
        Class<?> type = encodedValuesByTypeDto.getValueTypeByKey(name).orElseThrow(() -> new IllegalStateException(
                "Field requested for which there is no encoded value annotation: " + name));
        return create(type, name);
    }

    private <U> EncodedValue create(Class<U> type, String name) {
        EncodedValueDto<T, U> encodedValueDto = encodedValuesByTypeDto.get(type, name);

        EncodedValueFactory<U> encodedValueFactory =
                encodedValueFactoryRegistry.lookupEncodedValueFactory(type).orElseThrow(
                () -> new IllegalStateException(
                        "No encoded value factory found for name: " + name + " with type: " + type));

        return encodedValueFactory.encode(encodedValueDto);
    }



}
