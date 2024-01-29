package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedBooleanMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedBooleanMapperFactoryTest {

    private final EncodedBooleanMapperFactory annotatedBooleanParserFactory = new EncodedBooleanMapperFactory();

    @Mock
    private EncodedValueLookup lookup;
    @Mock
    private EncodedValueDto<?, Boolean> encodedValueDto;

    @Test
    void getType() {
        assertEquals(Boolean.class, annotatedBooleanParserFactory.getType());
    }

    @Test
    void create() {
        TagParser tagParser = annotatedBooleanParserFactory.create(lookup, encodedValueDto);
        assertInstanceOf(EncodedBooleanMapper.class, tagParser);
    }
}