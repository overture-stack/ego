package org.overture.ego.reactor.receiver;

import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.User;
import org.overture.ego.reactor.events.UserEvents;
import org.overture.ego.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class UserReceiver {

  @Autowired
  private EventBus eventBus;
  @Autowired
  private UserService userService;

  @PostConstruct
  public void onStartUp() {
    // Initialize Reactor Listeners
    // ============================

    // UPDATE
    eventBus.on(
      Selectors.R(UserEvents.UPDATE),
      update()
    );
  }

  private Consumer<Event<?>> update() {
    return (updateEvent) -> {
      log.debug("Update event received: " + updateEvent.getData());
      try {
        User data = (User) updateEvent.getData();
        userService.update(data);
      } catch (ClassCastException e) {
        log.error("Update event received incompatible data type.", e);
      }
    };
  }

}
