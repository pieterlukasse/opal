package org.obiba.opal.core.service.impl;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.obiba.magma.DatasourceFactory;
import org.obiba.magma.datasource.hibernate.support.HibernateDatasourceFactory;
import org.obiba.magma.support.EntitiesPredicate;
import org.obiba.opal.core.domain.HasUniqueProperties;
import org.obiba.opal.core.domain.database.Database;
import org.obiba.opal.core.domain.database.MongoDbSettings;
import org.obiba.opal.core.domain.database.SqlSettings;
import org.obiba.opal.core.runtime.jdbc.DataSourceFactory;
import org.obiba.opal.core.runtime.jdbc.DatabaseSessionFactoryProvider;
import org.obiba.opal.core.runtime.jdbc.SessionFactoryFactory;
import org.obiba.opal.core.service.IdentifiersTableService;
import org.obiba.opal.core.service.OrientDbService;
import org.obiba.opal.core.service.database.CannotDeleteDatabaseWithDataException;
import org.obiba.opal.core.service.database.DatabaseRegistry;
import org.obiba.opal.core.service.database.IdentifiersDatabaseNotFoundException;
import org.obiba.opal.core.service.database.MultipleIdentifiersDatabaseException;
import org.obiba.opal.core.service.database.NoSuchDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atomikos.jdbc.AbstractDataSourceBean;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

@Component
public class DefaultDatabaseRegistry implements DatabaseRegistry {

  private static final Logger log = LoggerFactory.getLogger(DefaultDatabaseRegistry.class);

  @Autowired
  private SessionFactoryFactory sessionFactoryFactory;

  @Autowired
  private DataSourceFactory dataSourceFactory;

  @Autowired
  private OrientDbService orientDbService;

  @Autowired
  private IdentifiersTableService identifiersTableService;

  private final LoadingCache<String, DataSource> dataSourceCache = CacheBuilder.newBuilder()
      .removalListener(new DataSourceRemovalListener()) //
      .build(new DataSourceCacheLoader());

  private final LoadingCache<String, SessionFactory> sessionFactoryCache = CacheBuilder.newBuilder()
      .removalListener(new SessionFactoryRemovalListener()) //
      .build(new SessionFactoryCacheLoader());

  private final SetMultimap<String, String> registrations = Multimaps
      .synchronizedSetMultimap(HashMultimap.<String, String>create());

  @Override
  @PostConstruct
  public void start() {
    orientDbService.createUniqueIndex(Database.class);
  }

  @Override
  @PreDestroy
  public void stop() {
    sessionFactoryCache.invalidateAll();
    dataSourceCache.invalidateAll();
  }

  @Override
  public Iterable<Database> list() {
    return orientDbService
        .list(Database.class, "select from " + Database.class.getSimpleName() + " where usedForIdentifiers = ?", false);
  }

  @Override
  public Iterable<Database> list(@Nullable Database.Usage usage) {
    if(usage == null) {
      return list();
    }
    return orientDbService.list(Database.class,
        "select from " + Database.class.getSimpleName() + " where usedForIdentifiers = ? and usage = ?", false, usage);
  }

  @Override
  public Iterable<Database> listSqlDatabases() {
    return orientDbService.list(Database.class,
        "select from " + Database.class.getSimpleName() + " where usedForIdentifiers = ? and sqlSettings is not null",
        false);
  }

  @Override
  public Iterable<Database> listMongoDatabases() {
    return orientDbService.list(Database.class, "select from " + Database.class.getSimpleName() +
        " where usedForIdentifiers = ? and mongoDbSettings is not null", false);
  }

  @Override
  public boolean hasDatabases(@Nullable Database.Usage usage) {
    return !Iterables.isEmpty(list(usage));
  }

  @NotNull
  @Override
  public Database getDatabase(@NotNull String name) throws NoSuchDatabaseException {
    return orientDbService.findUnique(Database.Builder.create().name(name).build());
  }

  @Override
  public DataSource getDataSource(@NotNull String name, @Nullable String usedByDatasource) {
    register(name, usedByDatasource);
    return dataSourceCache.getUnchecked(name);
  }

  @Override
  public SessionFactory getSessionFactory(@NotNull String name, @Nullable String usedByDatasource) {
    register(name, usedByDatasource);
    return sessionFactoryCache.getUnchecked(name);
  }

  @Override
  public void save(@NotNull Database database)
      throws ConstraintViolationException, MultipleIdentifiersDatabaseException {

    //TODO valid unique url
    validUniqueIdentifiersDatabase(database);

    if(database.isDefaultStorage()) {
      Database previousDefaultStorageDatabase = getDefaultStorageDatabase();
      if(previousDefaultStorageDatabase == null || previousDefaultStorageDatabase.equals(database)) {
        orientDbService.save(database, database);
      } else {
        previousDefaultStorageDatabase.setDefaultStorage(false);
        orientDbService.save(ImmutableMap
            .<HasUniqueProperties, HasUniqueProperties>of(database, database, previousDefaultStorageDatabase,
                previousDefaultStorageDatabase));
      }
    } else {
      orientDbService.save(database, database);
    }
  }

  private void validUniqueIdentifiersDatabase(Database database) throws MultipleIdentifiersDatabaseException {
    if(database.isUsedForIdentifiers()) {
      try {
        Database identifiersDatabase = getIdentifiersDatabase();
        if(!Objects.equal(identifiersDatabase.getName(), database.getName())) {
          throw new MultipleIdentifiersDatabaseException(identifiersDatabase.getName(), database.getName());
        }
      } catch(IdentifiersDatabaseNotFoundException ignored) {
      }
    }
  }

  @Nullable
  @Override
  public Database getDefaultStorageDatabase() {
    return orientDbService.uniqueResult(Database.class,
        "select from " + Database.class.getSimpleName() + " where usedForIdentifiers = ? and defaultStorage = ?", false,
        true);
  }

  @Override
  public void delete(@NotNull Database database) throws CannotDeleteDatabaseWithDataException {
    if(database.isUsedForIdentifiers()) {
      if(identifiersTableService.hasEntities(new EntitiesPredicate.NonViewEntitiesPredicate())) {
        throw new IllegalArgumentException("Cannot delete identifiers database with entities");
      }
      unregister(database.getName(), identifiersTableService.getDatasourceName());
    } else if(!database.isEditable()) {
      throw new IllegalArgumentException("Cannot delete non editable database");
    }
    orientDbService.delete(database);
    destroyDataSource(database.getName());
  }

  private void destroyDataSource(String name) {
    sessionFactoryCache.invalidate(name);
    dataSourceCache.invalidate(name);
  }

  private void register(String databaseName, @Nullable String usedByDatasource) {
    if(Strings.isNullOrEmpty(usedByDatasource)) return;
    Database database = getDatabase(databaseName);
    if(database.isEditable()) {
      database.setEditable(false);
      orientDbService.save(database, database);
    }
    registrations.put(databaseName, usedByDatasource);
  }

  @Override
  public void unregister(@NotNull String databaseName, String usedByDatasource) {
    Database database = getDatabase(databaseName);
    database.setEditable(true);
    orientDbService.save(database, database);
    // close SessionFactory or JDBC dataSource
    destroyDataSource(databaseName);
    registrations.remove(databaseName, usedByDatasource);
  }

  @Override
  public boolean hasIdentifiersDatabase() {
    return orientDbService
        .uniqueResult(Database.class, "select from " + Database.class.getSimpleName() + " where usedForIdentifiers = ?",
            true) != null;
  }

  @NotNull
  @Override
  public Database getIdentifiersDatabase() throws IdentifiersDatabaseNotFoundException {
    Database database = orientDbService
        .uniqueResult(Database.class, "select from " + Database.class.getSimpleName() + " where usedForIdentifiers = ?",
            true);
    if(database == null) throw new IdentifiersDatabaseNotFoundException();
    return database;
  }

  @NotNull
  @Override
  public DatasourceFactory createDataSourceFactory(@NotNull String datasourceName, @NotNull Database database) {
    String databaseName = database.getName();
    Preconditions.checkArgument(database.getUsage() == Database.Usage.STORAGE,
        "Cannot create DatasourceFactory for non storage database " + databaseName + ": " + database.getUsage());
    register(database.getName(), datasourceName);

    SqlSettings sqlSettings = database.getSqlSettings();
    if(sqlSettings != null) {
      if(sqlSettings.getSqlSchema() != SqlSettings.SqlSchema.HIBERNATE) {
        throw new IllegalArgumentException(
            "Cannot create datasource for non Hibernate storage database " + databaseName + ": " +
                sqlSettings.getSqlSchema());
      }
      return new HibernateDatasourceFactory(datasourceName,
          new DatabaseSessionFactoryProvider(datasourceName, this, database.getName()));
    }
    MongoDbSettings mongoDbSettings = database.getMongoDbSettings();
    if(mongoDbSettings != null) {
      return mongoDbSettings.createMongoDBDatasourceFactory(datasourceName);
    }
    throw new IllegalArgumentException("Unknown datasource config for database " + database.getClass());
  }

  private class DataSourceCacheLoader extends CacheLoader<String, DataSource> {

    @Override
    public DataSource load(String databaseName) throws Exception {
      log.info("Building DataSource {}", databaseName);
      return dataSourceFactory.createDataSource(getDatabase(databaseName));
    }
  }

  private static class DataSourceRemovalListener implements RemovalListener<String, DataSource> {

    @Override
    public void onRemoval(RemovalNotification<String, DataSource> notification) {
      log.info("Destroying DataSource {}", notification.getKey());
      DataSource dataSource = notification.getValue();
      if(dataSource != null) {
        ((AbstractDataSourceBean) dataSource).close();
      }
    }
  }

  private class SessionFactoryCacheLoader extends CacheLoader<String, SessionFactory> {

    @Override
    public SessionFactory load(String databaseName) throws Exception {
      log.info("Building SessionFactory {}", databaseName);
      return sessionFactoryFactory.getSessionFactory(getDataSource(databaseName, null));
    }
  }

  private static class SessionFactoryRemovalListener implements RemovalListener<String, SessionFactory> {
    @Override
    public void onRemoval(RemovalNotification<String, SessionFactory> notification) {
      try {
        log.info("Destroying SessionFactory {}", notification.getKey());
        SessionFactory sessionFactory = notification.getValue();
        if(sessionFactory != null) {
          sessionFactory.close();
        }
      } catch(HibernateException e) {
        log.warn("Ignoring exception during SessionFactory shutdown: ", e);
      }
    }
  }

  @VisibleForTesting
  void clearCaches() {
    dataSourceCache.invalidateAll();
    sessionFactoryCache.invalidateAll();
    registrations.clear();
  }

}
