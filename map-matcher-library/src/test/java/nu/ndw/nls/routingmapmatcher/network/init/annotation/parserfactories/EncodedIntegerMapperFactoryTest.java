package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedIntegerMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedIntegerMapperFactoryTest {

    private final EncodedIntegerMapperFactory annotatedIntegerParserFactory = new EncodedIntegerMapperFactory();

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private EncodedValueDto<?, Integer> encodedValueDto;

    @Test
    void getType() {
        assertEquals(Integer.class, annotatedIntegerParserFactory.getType());
    }

    @Test
    void create() {
        TagParser tagParser = annotatedIntegerParserFactory.create(lookup, encodedValueDto);
        assertInstanceOf(EncodedIntegerMapper.class, tagParser);
    }
}