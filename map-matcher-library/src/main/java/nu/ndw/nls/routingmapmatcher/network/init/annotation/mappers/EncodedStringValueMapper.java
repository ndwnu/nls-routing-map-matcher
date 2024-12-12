package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.StringEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;

public class EncodedStringValueMapper<T extends NetworkEncoded> extends AbstractEncodedMapper<T, String> {

    private final StringEncodedValue stringEncodedValue;

    public EncodedStringValueMapper(EncodedValueLookup lookup, EncodedValueDto<T, String> encodedValueDto) {
        super(encodedValueDto);
        this.stringEncodedValue = lookup.getStringEncodedValue(encodedValueDto.key());
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, String value) {
        stringEncodedValue.setString(reverse, edgeId, edgeIntAccess, value);
    }
}
