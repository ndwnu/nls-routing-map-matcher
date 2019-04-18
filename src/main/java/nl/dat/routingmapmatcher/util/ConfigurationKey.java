package nl.dat.routingmapmatcher.util;

public enum ConfigurationKey {

  POSTGRES_END_POINT("POSTGRES_END_POINT"),
  POSTGRES_DATABASE("POSTGRES_DATABASE"),
  POSTGRES_USERNAME("POSTGRES_USERNAME"),
  POSTGRES_PASSWORD("POSTGRES_PASSWORD");

  private final String environmentVariableName;

  private ConfigurationKey(final String environmentVariableName) {
    this.environmentVariableName = environmentVariableName;
  }

  public String getEnvironmentVariableName() {
    return environmentVariableName;
  }

}
