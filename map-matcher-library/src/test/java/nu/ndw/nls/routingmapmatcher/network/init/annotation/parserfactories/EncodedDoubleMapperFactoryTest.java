package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedDoubleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedDoubleMapperFactoryTest {

    private final EncodedDoubleMapperFactory annotatedDoubleParserFactory = new EncodedDoubleMapperFactory();

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private EncodedValueDto<?, Double> encodedValueDto;

    @Test
    void getType() {
        assertEquals(Double.class, annotatedDoubleParserFactory.getType());
    }

    @Test
    void create() {
        TagParser tagParser = annotatedDoubleParserFactory.create(lookup, encodedValueDto);
        assertInstanceOf(EncodedDoubleMapper.class, tagParser);
    }
}