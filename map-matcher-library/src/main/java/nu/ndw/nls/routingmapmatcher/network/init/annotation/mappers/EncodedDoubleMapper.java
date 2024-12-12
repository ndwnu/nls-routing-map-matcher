package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;

@Slf4j
public class EncodedDoubleMapper<T extends NetworkEncoded> extends AbstractEncodedMapper<T, Double> {

    private final DecimalEncodedValue doubleEnc;

    public EncodedDoubleMapper(EncodedValueLookup lookup, EncodedValueDto<T, Double> encodedValueDto) {
        super(encodedValueDto);
        this.doubleEnc = lookup.getDecimalEncodedValue(encodedValueDto.key());
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, Double value) {
        doubleEnc.setDecimal(reverse, edgeId, edgeIntAccess, value);
    }
}
