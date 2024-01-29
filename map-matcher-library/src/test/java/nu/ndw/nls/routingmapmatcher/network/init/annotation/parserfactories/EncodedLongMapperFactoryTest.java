package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedLongMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedLongMapperFactoryTest {

    private final EncodedLongMapperFactory annotatedLongParserFactory = new EncodedLongMapperFactory();

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private EncodedValueDto<?, Long> encodedValueDto;

    @Test
    void getType() {
        assertEquals(Long.class, annotatedLongParserFactory.getType());
    }

    @Test
    void create() {
        TagParser tagParser = annotatedLongParserFactory.create(lookup, encodedValueDto);
        assertInstanceOf(EncodedLongMapper.class, tagParser);
    }


}