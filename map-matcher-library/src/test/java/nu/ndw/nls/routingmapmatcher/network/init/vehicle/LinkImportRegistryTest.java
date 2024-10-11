package nu.ndw.nls.routingmapmatcher.network.init.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DefaultImportRegistry;
import com.graphhopper.routing.ev.ImportUnit;
import java.util.Map;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedMapperFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedValueFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedValueFactory;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.ProfileAccessAndSpeedAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkImportRegistryTest {

    public static final String TEST_ATTRIBUTE = "test_attribute";
    @Mock
    private EncodedValuesByTypeDto<? extends Link> encodedValuesByTypeDto;
    @Mock
    private EncodedValueFactoryRegistry encodedValueFactoryRegistry;
    @Mock
    private EncodedMapperFactoryRegistry classToEncodedMapperFactory;
    @Mock
    private Map<String, LinkVehicleMapper<? extends Link>> linkMappersByVehicleType;
    @Mock
    private DefaultImportRegistry defaultImportRegistry;
    @Mock
    private ProfileAccessAndSpeedAttributes profileAccessAndSpeedAttributes;
    @Mock
    private ImportUnit importUnit;
    @Mock
    private EncodedValueDto encodedValueDto;
    @Mock
    private EncodedValueFactory encodedValueFactory;

    @InjectMocks
    private LinkImportRegistry linkImportRegistry;

    @Test
    void createImportUnit_ok() {
        when(profileAccessAndSpeedAttributes.isSpeedAttribute(TEST_ATTRIBUTE))
                .thenReturn(false);
        when(profileAccessAndSpeedAttributes.isAccessAttribute(TEST_ATTRIBUTE))
                .thenReturn(false);
        when(encodedValuesByTypeDto.getValueTypeByKey(TEST_ATTRIBUTE))
                .thenReturn(Optional.of(String.class));
        ImportUnit result = linkImportRegistry.createImportUnit(TEST_ATTRIBUTE);
        assertThat(result).isNotNull();
    }

    @Test
    void createImportUnit_ok_access_attribute() {
        when(profileAccessAndSpeedAttributes.isSpeedAttribute(TEST_ATTRIBUTE))
                .thenReturn(false);
        when(profileAccessAndSpeedAttributes.isAccessAttribute(TEST_ATTRIBUTE))
                .thenReturn(true);
        ImportUnit result = linkImportRegistry.createImportUnit(TEST_ATTRIBUTE);
        assertThat(result).isNotNull();
    }

    @Test
    void createImportUnit_ok_speed_attribute() {
        when(profileAccessAndSpeedAttributes.isSpeedAttribute(TEST_ATTRIBUTE))
                .thenReturn(true);
        ImportUnit result = linkImportRegistry.createImportUnit(TEST_ATTRIBUTE);
        assertThat(result).isNotNull();
    }

    @Test
    void createImportUnit_ok_default_import_registry() {
        when(profileAccessAndSpeedAttributes.isSpeedAttribute(TEST_ATTRIBUTE))
                .thenReturn(false);
        when(profileAccessAndSpeedAttributes.isAccessAttribute(TEST_ATTRIBUTE))
                .thenReturn(false);
        when(defaultImportRegistry.createImportUnit(TEST_ATTRIBUTE)).thenReturn(importUnit);
        ImportUnit result = linkImportRegistry.createImportUnit(TEST_ATTRIBUTE);
        assertThat(result).isEqualTo(importUnit);
    }
}
