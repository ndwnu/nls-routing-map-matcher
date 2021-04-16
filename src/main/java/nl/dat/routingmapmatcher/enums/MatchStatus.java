package nl.dat.routingmapmatcher.enums;

import java.util.Locale;

public enum MatchStatus {

  EXCEPTION,
  INVALID_INPUT,
  NO_PATH,
  MATCH;

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
