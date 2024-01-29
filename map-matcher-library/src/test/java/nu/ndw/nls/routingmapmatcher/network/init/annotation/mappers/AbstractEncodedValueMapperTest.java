package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.storage.IntsRef;
import java.util.function.Function;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractEncodedValueMapperTest {

    public static final int EDGE_ID = 123;
    public static final int FORWARD_VALUE = 555;
    public static final int REVERSE_VALUE = 666;

    private static class MyLink extends Link {
        protected MyLink(long id, long fromNodeId, long toNodeId, double distanceInMeters,
                LineString geometry) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
        }
    }

    @Getter
    private static class EncodedValueMapper extends AbstractEncodedMapper<MyLink, Integer> {


        private Integer edgeIdForward;
        private Integer edgeIdReverse;
        private EdgeIntAccess edgeIntAccessForward;
        private EdgeIntAccess edgeIntAccessReverse;
        private Integer valueForward;
        private Integer valueReverse;

        public EncodedValueMapper(EncodedValueDto<MyLink, Integer> encodedValueDto) {
            super(encodedValueDto);
        }

        @Override
        protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, Integer value) {
            if (reverse) {
                edgeIdReverse = edgeId;
                edgeIntAccessReverse = edgeIntAccess;
                valueReverse = value;
            } else {
                edgeIdForward = edgeId;
                edgeIntAccessForward = edgeIntAccess;
                valueForward = value;
            }
        }
    }

    @Mock
    private EncodedValueDto<MyLink, Integer> encodedValue;
    @Mock
    private EdgeIntAccess edgeIntAccess;
    @Mock
    private IntsRef relationFlags;
    @Mock
    private Function<MyLink, Integer> forwardValueSupplier;
    @Mock
    private Function<MyLink, Integer> reverseValueSupplier;
    @Mock
    private MyLink myLink;

    @Test
    void handleWayTags_ok_encodesBothDirections() {
        EncodedValueMapper annotatedParser = new EncodedValueMapper(encodedValue);

        when(encodedValue.valueSupplier()).thenReturn(forwardValueSupplier);
        when(forwardValueSupplier.apply(myLink)).thenReturn(FORWARD_VALUE);

        when(encodedValue.isDirectional()).thenReturn(true);
        when(encodedValue.valueReverseSupplier()).thenReturn(reverseValueSupplier);
        when(reverseValueSupplier.apply(myLink)).thenReturn(REVERSE_VALUE);

        annotatedParser.handleWayTags(EDGE_ID, edgeIntAccess, myLink, relationFlags);

        assertForwardSetInvoked(annotatedParser);
        assertReverseSetInvoked(annotatedParser);
    }

    @Test
    void handleWayTags_ok_encodesNoDirectionsBothNull() {
        EncodedValueMapper annotatedParser = new EncodedValueMapper(encodedValue);

        when(encodedValue.valueSupplier()).thenReturn(forwardValueSupplier);
        when(forwardValueSupplier.apply(myLink)).thenReturn(null);

        when(encodedValue.isDirectional()).thenReturn(true);
        when(encodedValue.valueReverseSupplier()).thenReturn(reverseValueSupplier);
        when(reverseValueSupplier.apply(myLink)).thenReturn(null);

        annotatedParser.handleWayTags(EDGE_ID, edgeIntAccess, myLink, relationFlags);

        assertForwardSetNotInvoked(annotatedParser);
        assertReverseSetNotInvoked(annotatedParser);
    }

    @Test
    void handleWayTags_ok_encodesForwardOnlyReverseIsNull() {
        EncodedValueMapper annotatedParser = new EncodedValueMapper(encodedValue);

        when(encodedValue.valueSupplier()).thenReturn(forwardValueSupplier);
        when(forwardValueSupplier.apply(myLink)).thenReturn(FORWARD_VALUE);

        when(encodedValue.isDirectional()).thenReturn(true);
        when(encodedValue.valueReverseSupplier()).thenReturn(reverseValueSupplier);
        when(reverseValueSupplier.apply(myLink)).thenReturn(null);

        annotatedParser.handleWayTags(EDGE_ID, edgeIntAccess, myLink, relationFlags);

        assertForwardSetInvoked(annotatedParser);
        assertReverseSetNotInvoked(annotatedParser);
    }

    @Test
    void handleWayTags_ok_encodesReverseOnlyForwardIsNull() {
        EncodedValueMapper annotatedParser = new EncodedValueMapper(encodedValue);

        when(encodedValue.valueSupplier()).thenReturn(forwardValueSupplier);
        when(forwardValueSupplier.apply(myLink)).thenReturn(null);

        when(encodedValue.isDirectional()).thenReturn(true);
        when(encodedValue.valueReverseSupplier()).thenReturn(reverseValueSupplier);
        when(reverseValueSupplier.apply(myLink)).thenReturn(REVERSE_VALUE);

        annotatedParser.handleWayTags(EDGE_ID, edgeIntAccess, myLink, relationFlags);

        assertForwardSetNotInvoked(annotatedParser);
        assertReverseSetInvoked(annotatedParser);
    }

    @Test
    void handleWayTags_ok_encodesForwardOnlyIsNotDirectional() {
        EncodedValueMapper annotatedParser = new EncodedValueMapper(encodedValue);

        when(encodedValue.valueSupplier()).thenReturn(forwardValueSupplier);
        when(forwardValueSupplier.apply(myLink)).thenReturn(FORWARD_VALUE);

        when(encodedValue.isDirectional()).thenReturn(false);

        annotatedParser.handleWayTags(EDGE_ID, edgeIntAccess, myLink, relationFlags);

        assertForwardSetInvoked(annotatedParser);
        assertReverseSetNotInvoked(annotatedParser);
    }

    private void assertForwardSetInvoked(EncodedValueMapper annotatedParser) {
        assertEquals(EDGE_ID, annotatedParser.getEdgeIdForward());
        assertEquals(edgeIntAccess, annotatedParser.getEdgeIntAccessForward());
        assertEquals(FORWARD_VALUE, annotatedParser.getValueForward());
    }
    private void assertReverseSetInvoked(EncodedValueMapper annotatedParser) {
        assertEquals(EDGE_ID, annotatedParser.getEdgeIdReverse());
        assertEquals(edgeIntAccess, annotatedParser.getEdgeIntAccessReverse());
        assertEquals(REVERSE_VALUE, annotatedParser.getValueReverse());
    }

    private void assertForwardSetNotInvoked(EncodedValueMapper annotatedParser) {
        assertNull(annotatedParser.getEdgeIdForward());
        assertNull(annotatedParser.getEdgeIntAccessForward());
        assertNull(annotatedParser.getValueForward());
    }
    private void assertReverseSetNotInvoked(EncodedValueMapper annotatedParser) {
        assertNull(annotatedParser.getEdgeIdReverse());
        assertNull(annotatedParser.getEdgeIntAccessReverse());
        assertNull(annotatedParser.getValueReverse());
    }
}