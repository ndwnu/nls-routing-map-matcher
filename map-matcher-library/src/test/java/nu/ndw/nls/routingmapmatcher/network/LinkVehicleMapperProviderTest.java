package nu.ndw.nls.routingmapmatcher.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkVehicleMapperProviderTest {

    private static final String CAR = "car";
    private static final String BIKE = "bike";
    private static final String TRAIN = "train";

    @Test
    void instantiate_ok() {
        List<LinkVehicleMapper<? extends Link>> vehicleList = List.of(
                mockVehicle(TestALink.class, CAR),
                mockVehicle(TestALink.class, BIKE),
                mockVehicle(TestBLink.class, CAR),
                mockVehicle(TestBLink.class, TRAIN),
                mockVehicle(TestCLink.class, CAR));

        assertDoesNotThrow(() -> new LinkVehicleMapperProvider(vehicleList));
    }

    @Test
    void instantiate_exception_duplicateVehicleForLinkType() {
        List<LinkVehicleMapper<? extends Link>> vehicleList = List.of(
                mockVehicle(TestALink.class, CAR),
                mockVehicle(TestALink.class, BIKE),
                mockVehicle(TestBLink.class, CAR),
                mockVehicle(TestBLink.class, CAR),
                mockVehicle(TestCLink.class, CAR),
                mockVehicle(TestCLink.class, BIKE),
                mockVehicle(TestCLink.class, BIKE),
                mockVehicle(TestCLink.class, TRAIN),
                mockVehicle(TestCLink.class, TRAIN));

        String expectedMessage = """
                Encountered problems while organising linkvehicles:
                Link class 'TestBLink' has multiple vehicles with name(s): [car]
                Link class 'TestCLink' has multiple vehicles with name(s): [bike, train]""";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new LinkVehicleMapperProvider(vehicleList));
        assertThat(exception).hasMessage(expectedMessage);
    }

    @Test
    void getLinksForType_ok() {
        LinkVehicleMapper<TestBLink> bLinkCar = mockVehicle(TestBLink.class, CAR);
        LinkVehicleMapper<TestBLink> bLinkTrain = mockVehicle(TestBLink.class, TRAIN);

        List<LinkVehicleMapper<? extends Link>> vehicleList = List.of(
                mockVehicle(TestALink.class, CAR),
                mockVehicle(TestALink.class, BIKE),
                bLinkCar,
                bLinkTrain,
                mockVehicle(TestCLink.class, CAR));
        LinkVehicleMapperProvider linkVehicleMapperProvider = new LinkVehicleMapperProvider(vehicleList);

        Map<String, LinkVehicleMapper<TestBLink>> result = linkVehicleMapperProvider.getLinksForType(TestBLink.class);

        assertThat(result)
                .hasSize(2)
                .containsEntry(CAR, bLinkCar)
                .containsEntry(TRAIN, bLinkTrain);

    }

    private <T extends Link> LinkVehicleMapper<T> mockVehicle(Class<T> linkClass, String vehicleName) {
        LinkVehicleMapper<T> vehicle = mock(LinkVehicleMapper.class);
        when(vehicle.getLinkClass()).thenReturn(linkClass);
        when(vehicle.getVehicleName()).thenReturn(vehicleName);
        return vehicle;
    }

    private static class TestALink extends Link {

        public TestALink() {
            super(0, 1, 2, 5.0, null);
        }
    }

    private static class TestBLink extends Link {

        public TestBLink() {
            super(0, 1, 2, 5.0, null);
        }
    }

    private static class TestCLink extends Link {

        public TestCLink() {
            super(0, 1, 2, 5.0, null);
        }
    }

}