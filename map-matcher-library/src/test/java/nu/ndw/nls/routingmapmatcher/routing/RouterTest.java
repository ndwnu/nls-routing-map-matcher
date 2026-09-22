package nu.ndw.nls.routingmapmatcher.routing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.Path;
import com.graphhopper.util.EdgeIteratorState;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.exception.RoutingRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouterTest {

    @Mock
    private Path path;

    @Mock
    private EdgeIteratorState edge;

    @Test
    void ensurePathIsRoutable_throwsRoutingRequestException_whenPathIsFoundButHasNoEdges() {
        when(path.isFound()).thenReturn(true);

        assertThatThrownBy(() -> Router.ensurePathIsRoutable(path, List.of()))
                .isInstanceOf(RoutingRequestException.class)
                .hasMessage("No route found: waypoints resolve to the same node");
    }

    @Test
    void ensurePathIsRoutable_throwsRoutingException_whenPathIsNotFoundAndHasNoEdges() {
        when(path.isFound()).thenReturn(false);

        assertThatThrownBy(() -> Router.ensurePathIsRoutable(path, List.of()))
                .isInstanceOf(RoutingException.class)
                .hasMessage("Unexpected: path was not found and has no edges");
    }

    @Test
    void ensurePathIsRoutable_doesNotThrow_whenPathHasEdges() {
        assertThatCode(() -> Router.ensurePathIsRoutable(path, List.of(edge)))
                .doesNotThrowAnyException();
    }
}
