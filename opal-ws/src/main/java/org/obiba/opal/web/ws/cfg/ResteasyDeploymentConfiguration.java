/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.ws.cfg;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.springmvc.ResteasyInitializer;
import org.springframework.context.annotation.Bean;

//@Configuration
public class ResteasyDeploymentConfiguration {

  @Bean
  public ResteasyProviderFactory providerFactory() {
    ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
    new ResteasyInitializer(factory);
    return factory;
  }

  @Bean
  public Dispatcher dispatcher() {
    return new SynchronousDispatcher(providerFactory());
  }

  @Bean
  public Registry registry() {
    return dispatcher().getRegistry();
  }

//  @Bean
//  public ResteasySpringListener listener() {
//    return new ResteasySpringListener(providerFactory(), registry());
//  }

//  @Bean
//  public ServerInterceptorSpringBeanProcessor resteasyInterceptorPostProcessor() {
//    return new ServerInterceptorSpringBeanProcessor(dispatcher());
//  }
//
//  /**
//   * Required because the normal SpringBeanProcessor does not pickup classes annotated with {@code ServerInterceptor}.
//   * https://jira.jboss.org/browse/RESTEASY-394
//   */
//  private static class ServerInterceptorSpringBeanProcessor implements BeanPostProcessor {
//
//    private static final Logger log = LoggerFactory.getLogger(ServerInterceptorSpringBeanProcessor.class);
//
//    private final Dispatcher dispatcher;
//
//    private ServerInterceptorSpringBeanProcessor(Dispatcher dispatcher) {
//      this.dispatcher = dispatcher;
//    }
//
//    @Override
//    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//      Class<?> beanClass = bean.getClass();
//      if(beanClass.isAnnotationPresent(ServerInterceptor.class)) {
//        if(PreProcessInterceptor.class.isAssignableFrom(beanClass)) {
//          log.info("Registering bean '{}' as pre-process interceptor.", beanName);
//          dispatcher.getProviderFactory().getServerPreProcessInterceptorRegistry()
//              .register((PreProcessInterceptor) bean);
//        }
//        if(PostProcessInterceptor.class.isAssignableFrom(beanClass)) {
//          log.info("Registering bean '{}' as post-process interceptor.", beanName);
//          dispatcher.getProviderFactory().getServerPostProcessInterceptorRegistry()
//              .register((PostProcessInterceptor) bean);
//        }
//      }
//      if(ExceptionMapper.class.isAssignableFrom(beanClass)) {
//        log.info("Registering bean '{}' as exception mapper.", beanName);
//        dispatcher.getProviderFactory().addExceptionMapper((ExceptionMapper<?>) bean);
//      }
//      return bean;
//    }
//
//    @Override
//    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
//      return bean;
//    }
//
//  }
}
