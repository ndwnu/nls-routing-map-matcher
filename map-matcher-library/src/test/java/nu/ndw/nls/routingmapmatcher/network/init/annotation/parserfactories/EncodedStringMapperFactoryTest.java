package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedStringValueMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedStringMapperFactoryTest {

    private final EncodedStringMapperFactory annotatedStringParserFactory = new EncodedStringMapperFactory();

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private EncodedValueDto<?, String> encodedValueDto;

    @Test
    void getType() {
        assertEquals(String.class, annotatedStringParserFactory.getType());
    }

    @Test
    void create() {
        TagParser tagParser = annotatedStringParserFactory.create(lookup, encodedValueDto);
        assertInstanceOf(EncodedStringValueMapper.class, tagParser);
    }
}