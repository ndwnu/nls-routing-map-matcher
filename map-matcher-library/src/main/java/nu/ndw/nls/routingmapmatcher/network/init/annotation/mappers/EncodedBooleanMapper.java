package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;

public class EncodedBooleanMapper<T extends NetworkEncoded> extends AbstractEncodedMapper<T, Boolean> {

    private final BooleanEncodedValue booleanEnc;

    public EncodedBooleanMapper(EncodedValueLookup lookup, EncodedValueDto<T, Boolean> encodedValueDto) {
        super(encodedValueDto);
        this.booleanEnc = lookup.getBooleanEncodedValue(encodedValueDto.key());
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, Boolean value) {
        this.booleanEnc.setBool(reverse, edgeId, edgeIntAccess, value);
    }

}
