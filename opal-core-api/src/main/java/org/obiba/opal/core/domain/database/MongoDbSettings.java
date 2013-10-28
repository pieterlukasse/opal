package org.obiba.opal.core.domain.database;

import javax.annotation.Nonnull;

import org.hibernate.validator.constraints.NotBlank;
import org.obiba.magma.datasource.mongodb.MongoDBDatasourceFactory;

public class MongoDbSettings {

  /**
   * mongodb://{hostname}:{port}/{databaseName}
   */
  @Nonnull
  @NotBlank
  private String url;

  private String username;

  private String password;

  private String properties;

  public MongoDBDatasourceFactory createMongoDBDatasourceFactory(String datasourceName) {
    return new MongoDBDatasourceFactory(datasourceName, url, username, password, properties);
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  @Nonnull
  public String getUrl() {
    return url;
  }

  public void setUrl(@Nonnull String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @SuppressWarnings("ParameterHidesMemberVariable")
  public static class Builder {

    private MongoDbSettings settings;

    private Builder() {
    }

    public static Builder create() {
      Builder builder = new Builder();
      builder.settings = new MongoDbSettings();
      return builder;
    }

    public Builder password(String password) {
      settings.password = password;
      return this;
    }

    public Builder url(String url) {
      settings.url = url;
      return this;
    }

    public Builder username(String username) {
      settings.username = username;
      return this;
    }

    public Builder properties(String properties) {
      settings.properties = properties;
      return this;
    }

    public MongoDbSettings build() {
      return settings;
    }

  }

}
