package nl.dat.routingmapmatcher.dataaccess.dao;

import java.io.Closeable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.statement.Query;

import nl.dat.routingmapmatcher.dataaccess.dto.LinkDto;
import nl.dat.routingmapmatcher.dataaccess.mapper.LinkDtoMapper;

public class LinkDao implements Closeable {

  private static final int FETCH_SIZE = 10_000;

  private final Handle handle;
  private final String networkQuery;
  private final CopyOnWriteArrayList<Query> queriesToClose;

  /**
   * @param handle JDBI handle which must be a handle within a transaction
   * @param networkQuery query to read network
   */
  public LinkDao(final Handle handle, final String networkQuery) {
    this.handle = handle;
    this.networkQuery = networkQuery;
    this.queriesToClose = new CopyOnWriteArrayList<>();
  }

  public ResultIterator<LinkDto> getLinksIterator() {
    final Query query = handle.createQuery(networkQuery);
    queriesToClose.add(query);
    return query
        .setFetchSize(FETCH_SIZE) // Only takes effect in PostgreSQL when called in a transaction
        .registerRowMapper(new LinkDtoMapper())
        .mapTo(LinkDto.class)
        .iterator();
  }

  @Override
  public void close() {
    for (final Query query : queriesToClose) {
      query.close();
    }
  }
}
