package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;

@Getter
public class MapMatchingLineRequest extends MapMatchingRequest<LineStringLocation> {

    private final LineMatchingMode lineMatchingMode;

    @Builder
    private MapMatchingLineRequest(final String locationTypeName,
            final Supplier<List<LineStringLocation>> locationSupplier, final LineMatchingMode lineMatchingMode) {
        super(locationTypeName, locationSupplier);
        this.lineMatchingMode = lineMatchingMode != null ? lineMatchingMode : LineMatchingMode.LINE_STRING;
    }
}
