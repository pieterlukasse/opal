/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.runtime.jdbc;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.obiba.opal.core.domain.database.SqlDatabase;
import org.springframework.stereotype.Component;

@Component
public class DataSourceFactory {

  public DataSource createDataSource(@Nonnull SqlDatabase database) {
    DataSourceFactoryBean factoryBean = new DataSourceFactoryBean();
    factoryBean.setDriverClass(database.getDriverClass());
    factoryBean.setUrl(database.getUrl());
    factoryBean.setUsername(database.getUsername());
    factoryBean.setPassword(database.getPassword());
    return factoryBean.getObject();
  }

}
