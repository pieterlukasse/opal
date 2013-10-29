/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.core.service.impl;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.obiba.opal.core.domain.OpalGeneralConfig;
import org.obiba.opal.core.service.OrientDbService;
import org.obiba.opal.core.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default implementation of System Service
 */
@Component
public class DefaultGeneralConfigService implements SystemService {

  private static final String UNIQUE_INDEX = "name";

  @Autowired
  private OrientDbService orientDbService;

  @Override
  @PostConstruct
  public void start() {
    orientDbService.createUniqueStringIndex(OpalGeneralConfig.class, UNIQUE_INDEX);
  }

  @Override
  public void stop() {
  }

  public void save(@Nonnull OpalGeneralConfig config) {
    orientDbService.save(config, UNIQUE_INDEX);
  }

  @Nonnull
  public OpalGeneralConfig getConfig() throws OpalGeneralConfigMissingException {
    Iterator<OpalGeneralConfig> iterator = orientDbService.list(OpalGeneralConfig.class).iterator();
    if(iterator.hasNext()) return iterator.next();
    throw new OpalGeneralConfigMissingException();
  }

}
