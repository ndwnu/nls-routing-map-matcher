package nu.ndw.nls.routingmapmatcher.model;

import java.util.Locale;

public enum MatchStatus {

    EXCEPTION,
    NO_MATCH,
    MATCH;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
