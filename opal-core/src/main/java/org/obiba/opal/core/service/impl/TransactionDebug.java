package org.obiba.opal.core.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionDebug {

  private static final Logger log = LoggerFactory.getLogger(TransactionDebug.class);

  public static void showTransactionStatus(String message) {
    //noinspection StringConcatenationArgumentToLogCall
    log.info((TransactionSynchronizationManager.isActualTransactionActive() ? "[+] " : "[-] ") + message);
  }

  public static void transactionRequired(String message) {
    showTransactionStatus(message);
    if(!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Transaction required but not active [" + message + "]");
    }
  }

}
