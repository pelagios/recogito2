package storage.es

import com.google.inject.AbstractModule

/** Binding ES as eager singleton, so we can start & stop properly **/
class ESModule extends AbstractModule {

  def configure = {
    bind(classOf[ES]).asEagerSingleton
  }

}