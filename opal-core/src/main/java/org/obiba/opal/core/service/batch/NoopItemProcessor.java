package org.obiba.opal.core.service.batch;

import org.obiba.opal.core.domain.batch.BatchImportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class NoopItemProcessor implements ItemProcessor<BatchImportConfig, Void> {

  private static final Logger log = LoggerFactory.getLogger(NoopItemProcessor.class);

  @Override
  public Void process(BatchImportConfig item) throws Exception {
    log.info("Process {}", item);
    return null;
  }
}
