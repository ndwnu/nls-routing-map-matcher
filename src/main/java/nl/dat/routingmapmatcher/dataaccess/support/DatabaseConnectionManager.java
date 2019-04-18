package nl.dat.routingmapmatcher.dataaccess.support;

import javax.annotation.Nullable;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.dat.routingmapmatcher.util.ConfigurationKey;
import nl.dat.routingmapmatcher.util.ConfigurationUtil;

public class DatabaseConnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

  private static final String JDBC_URL_PREFIX = "jdbc:postgresql://";

  @Nullable
  private static DatabaseConnectionManager instance;

  private final Jdbi jdbi;

  private DatabaseConnectionManager(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public static Jdbi configureJdbi(final Jdbi jdbi) {
    return jdbi
        .installPlugin(new SqlObjectPlugin())
        .installPlugin(new PostgresPlugin())
        .registerArgument(new JtsGeometryArgumentFactory());
  }

  public static DatabaseConnectionManager getInstance() {
    if (instance == null) {
      instance = new DatabaseConnectionManager(createJdbiFromEnvironment());
    }
    return instance;
  }

  private static Jdbi createJdbiFromEnvironment() {
    final ConfigurationUtil configurationUtil = new ConfigurationUtil();
    final String jdbcUrl = JDBC_URL_PREFIX + configurationUtil.readString(ConfigurationKey.POSTGRES_END_POINT) +
        "/" + configurationUtil.readString(ConfigurationKey.POSTGRES_DATABASE);
    final String username = configurationUtil.readString(ConfigurationKey.POSTGRES_USERNAME);
    final String password = configurationUtil.readString(ConfigurationKey.POSTGRES_PASSWORD);

    logger.info("Using database url {} and username {}", jdbcUrl, username);

    return configureJdbi(Jdbi.create(jdbcUrl, username, password));
  }

  public Jdbi getJdbi() {
    return jdbi;
  }

}
