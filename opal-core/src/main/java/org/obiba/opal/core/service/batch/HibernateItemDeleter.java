package org.obiba.opal.core.service.batch;

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.batch.item.database.HibernateItemWriter;

public class HibernateItemDeleter<T> extends HibernateItemWriter<T> {

  @Override
  protected void doWrite(SessionFactory sessionFactory, List<? extends T> items) {
    for(T item : items) {
      sessionFactory.getCurrentSession().delete(item);
    }
  }
}
