package nu.ndw.nls.routingmapmatcher.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.exceptions.ConnectionNotFoundException;
import com.graphhopper.util.exceptions.PointOutOfBoundsException;
import java.util.List;
import java.util.Map;
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
        List<EdgeIteratorState> edges = List.of();

        assertThatThrownBy(() -> Router.ensurePathIsRoutable(path, edges))
                .isInstanceOf(RoutingRequestException.class)
                .hasMessage("No route found: waypoints resolve to the same node");
    }

    @Test
    void ensurePathIsRoutable_throwsRoutingException_whenPathIsNotFoundAndHasNoEdges() {
        when(path.isFound()).thenReturn(false);
        List<EdgeIteratorState> edges = List.of();

        assertThatThrownBy(() -> Router.ensurePathIsRoutable(path, edges))
                .isInstanceOf(RoutingException.class)
                .hasMessage("Unexpected: path was not found and has no edges");
    }

    @Test
    void ensurePathIsRoutable_doesNotThrow_whenPathHasEdges() {
        assertThatCode(() -> Router.ensurePathIsRoutable(path, List.of(edge)))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureResponseHasNoErrors_doesNotThrow_whenResponseHasNoErrors() {
        GHResponse ghResponse = new GHResponse();

        assertThatCode(() -> Router.ensureResponseHasNoErrors(ghResponse))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureResponseHasNoErrors_throwsRoutingRequestException_whenAllErrorsAreWhitelisted() {
        GHResponse ghResponse = new GHResponse();
        ghResponse.addError(new PointOutOfBoundsException("out of bounds", 0));
        ghResponse.addError(new ConnectionNotFoundException("no connection", Map.of()));

        assertThatThrownBy(() -> Router.ensureResponseHasNoErrors(ghResponse))
                .isInstanceOf(RoutingRequestException.class)
                .hasMessageContaining("out of bounds")
                .hasMessageContaining("no connection");
    }

    @Test
    void ensureResponseHasNoErrors_throwsRoutingExceptionWithCause_whenAnErrorIsNotWhitelisted() {
        RuntimeException unexpectedError = new RuntimeException("something GraphHopper-internal went wrong");
        GHResponse ghResponse = new GHResponse();
        ghResponse.addError(unexpectedError);

        assertThatThrownBy(() -> Router.ensureResponseHasNoErrors(ghResponse))
                .isInstanceOf(RoutingException.class)
                .hasMessageContaining("something GraphHopper-internal went wrong")
                .hasCause(unexpectedError);
    }

    @Test
    void ensureResponseHasNoErrors_throwsRoutingExceptionWithFirstErrorAsCauseAndRestSuppressed_whenMixedWithWhitelistedErrors() {
        RuntimeException unexpectedError = new RuntimeException("something GraphHopper-internal went wrong");
        PointOutOfBoundsException secondError = new PointOutOfBoundsException("out of bounds", 0);
        GHResponse ghResponse = new GHResponse();
        ghResponse.addError(unexpectedError);
        ghResponse.addError(secondError);

        assertThatThrownBy(() -> Router.ensureResponseHasNoErrors(ghResponse))
                .isInstanceOf(RoutingException.class)
                .hasCause(unexpectedError)
                .satisfies(thrown -> assertThat(thrown.getSuppressed()).containsExactly(secondError));
    }
}
