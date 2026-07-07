package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

public class ExploreLimitComposite<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public static final int LIMIT = 1;

    public static final int IN_ACCESSIBLE = 2;

    public static final int ACCESSIBLE = 0;

    @Getter
    private final List<ExploreLimit<LABEL>> exploreLimits;

    @SafeVarargs
    public ExploreLimitComposite(ExploreLimit<LABEL>... exploreLimits) {
        this(List.of(exploreLimits));
    }

    public ExploreLimitComposite(List<ExploreLimit<LABEL>> exploreLimits) {
        super(LIMIT, false);
        this.exploreLimits = exploreLimits;
    }

    @Override
    protected double getValueForLabel(LABEL isochroneLabel, EncodingManager encodingManager) {
        return exploreLimits.stream()
                .allMatch(exploreLimit -> exploreLimit.isInLimit(isochroneLabel, encodingManager))
                ? ACCESSIBLE
                : IN_ACCESSIBLE;
    }

    @Override
    public String debug(LABEL isochroneLabel, EncodingManager encodingManager) {
        return "ExploreLimitComposite{limit=%s, exploreLimits=[%s], reached=%s}".formatted(
                getLimit(),
                exploreLimits.stream()
                        .map(exploreLimit -> exploreLimit.debug(isochroneLabel, encodingManager))
                        .collect(Collectors.joining(", ")),
                !isInLimit(isochroneLabel, encodingManager)
        );
    }
}
