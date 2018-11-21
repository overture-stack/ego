package bio.overture.ego.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.Environment;
import reactor.bus.EventBus;

@Configuration
public class ReactorConfig {

  @Bean
  public Environment env() {
    return Environment.initializeIfEmpty()
      .assignErrorJournal();
  }

  @Bean
  public EventBus createEventBus(Environment env) {
    return EventBus.create(env, Environment.THREAD_POOL);
  }

}
