package nu.ndw.nls.routingmapmatcher.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.exceptions.ConnectionNotFoundException;
import com.graphhopper.util.exceptions.PointOutOfBoundsException;
import com.graphhopper.util.shapes.GHPoint;
import java.util.List;
import java.util.Map;
import nu.ndw.nls.routingmapmatcher.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.exception.RoutingRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class RouterTest {

    @Mock
    private Path path;

    @Mock
    private EdgeIteratorState edge;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        routerLogger().addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        routerLogger().detachAppender(logAppender);
    }

    private static Logger routerLogger() {
        return (Logger) LoggerFactory.getLogger(Router.class);
    }

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

        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().equals("Unexpected: path was not found and has no edges"));
    }

    @Test
    void ensurePathIsRoutable_doesNotThrow_whenPathHasEdges() {
        assertThatCode(() -> Router.ensurePathIsRoutable(path, List.of(edge)))
                .doesNotThrowAnyException();

        assertThat(logAppender.list).isEmpty();
    }

    private static final GHRequest GH_REQUEST = new GHRequest(
            new GHPoint(52.177687, 5.430496),
            new GHPoint(52.175901, 5.428436));

    @Test
    void ensureResponseHasNoErrors_doesNotThrow_whenResponseHasNoErrors() {
        GHResponse ghResponse = new GHResponse();

        assertThatCode(() -> Router.ensureResponseHasNoErrors(ghResponse, GH_REQUEST))
                .doesNotThrowAnyException();

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void ensureResponseHasNoErrors_throwsRoutingRequestException_whenAllErrorsAreWhitelisted() {
        GHResponse ghResponse = new GHResponse();
        ghResponse.addError(new PointOutOfBoundsException("out of bounds", 0));
        ghResponse.addError(new ConnectionNotFoundException("no connection", Map.of()));

        assertThatThrownBy(() -> Router.ensureResponseHasNoErrors(ghResponse, GH_REQUEST))
                .isInstanceOf(RoutingRequestException.class)
                .hasMessageContaining("out of bounds")
                .hasMessageContaining("no connection");

        // A known, already-correctly-classified NO_ROUTE cause is not noteworthy - no WARN expected.
        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.WARN);
    }

    @Test
    void ensureResponseHasNoErrors_throwsRoutingExceptionWithCause_whenAnErrorIsNotWhitelisted() {
        RuntimeException unexpectedError = new RuntimeException("something GraphHopper-internal went wrong");
        GHResponse ghResponse = new GHResponse();
        ghResponse.addError(unexpectedError);

        assertThatThrownBy(() -> Router.ensureResponseHasNoErrors(ghResponse, GH_REQUEST))
                .isInstanceOf(RoutingException.class)
                .hasMessageContaining("something GraphHopper-internal went wrong")
                .hasCause(unexpectedError);

        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains(RuntimeException.class.getName())
                        && event.getFormattedMessage().contains("5.430496,52.177687;5.428436,52.175901"));
    }

    @Test
    void ensureResponseHasNoErrors_throwsRoutingExceptionWithFirstErrorAsCause_whenMixedWithWhitelistedErrors() {
        RuntimeException unexpectedError = new RuntimeException("something GraphHopper-internal went wrong");
        GHResponse ghResponse = new GHResponse();
        ghResponse.addError(unexpectedError);
        ghResponse.addError(new PointOutOfBoundsException("out of bounds", 0));

        assertThatThrownBy(() -> Router.ensureResponseHasNoErrors(ghResponse, GH_REQUEST))
                .isInstanceOf(RoutingException.class)
                .hasCause(unexpectedError);
    }
}
