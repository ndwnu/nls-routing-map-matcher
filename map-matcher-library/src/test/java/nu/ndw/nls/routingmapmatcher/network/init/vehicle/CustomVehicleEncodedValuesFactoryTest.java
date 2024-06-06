package nu.ndw.nls.routingmapmatcher.network.init.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.util.PMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomVehicleEncodedValuesFactoryTest {

    @Mock
    PMap properties;

    @InjectMocks
    CustomVehicleEncodedValuesFactory factory;

    @Test
    void createVehicleEncodedValues_ok() {

        VehicleEncodedValues result = factory.createVehicleEncodedValues("vehicle_name", properties);
        
        assertEquals("vehicle_name", result.getName());
        assertEquals("vehicle_name_access", result.getAccessEnc().getName());
        assertEquals("vehicle_name_average_speed", result.getAverageSpeedEnc().getName());
        assertEquals("vehicle_name_turn_restriction", result.getTurnRestrictionEnc().getName());

    }

}
