package org.overture.ego.reactor.receiver;

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
public class UserReceiver {

  @Autowired
  private EventBus eventBus;
  @Autowired
  UserService userService;

  @PostConstruct
  public void onStartUp() {
    // Initialize Reactor Listeners
    // ============================

    // UPDATE
    eventBus.on(
      Selectors.R(UserEvents.UPDATE.toString()),
      update()
    );
  }

  private Consumer<Event<User>> update() {
    return (updateEvent) -> userService.update(updateEvent.getData());
  }

}
