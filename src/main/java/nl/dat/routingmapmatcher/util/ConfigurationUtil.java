package nl.dat.routingmapmatcher.util;

import nl.dat.routingmapmatcher.exceptions.RoutingMapMatcherException;

public class ConfigurationUtil {

  public String readString(final ConfigurationKey configurationKey) {
    final String environmentVariableValue = System.getenv(configurationKey.getEnvironmentVariableName());
    if (environmentVariableValue == null) {
      throw new RoutingMapMatcherException("Environment variable " + configurationKey.getEnvironmentVariableName() +
          " not defined");
    }
    return environmentVariableValue;
  }

}
