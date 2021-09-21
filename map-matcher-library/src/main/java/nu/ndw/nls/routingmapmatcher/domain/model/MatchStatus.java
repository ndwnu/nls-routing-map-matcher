package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.Locale;

public enum MatchStatus {

    EXCEPTION,
    INVALID_INPUT,
    NO_MATCH,
    MATCH;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
