package bio.overture.ego.reactor.receiver;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.reactor.events.UserEvents;
import bio.overture.ego.service.UserService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;

import javax.annotation.PostConstruct;

import static bio.overture.ego.service.UserService.USER_CONVERTER;

@Component
@Slf4j
public class UserReceiver {

  @Autowired private EventBus eventBus;
  @Autowired private UserService userService;

  @PostConstruct
  public void onStartUp() {
    // Initialize Reactor Listeners
    // ============================

    // UPDATE
    eventBus.on(Selectors.R(UserEvents.UPDATE), update());
  }

  private Consumer<Event<?>> update() {
    return (updateEvent) -> {
      log.debug("Update event received: " + updateEvent.getData());
      try {
        val data = (User) updateEvent.getData();
        val userId = data.getId();
        val updateRequest = USER_CONVERTER.convertToUpdateRequest(data);
        userService.partialUpdate(userId, updateRequest);
      } catch (ClassCastException e) {
        log.error("Update event received incompatible data applicationType.", e);
      }
    };
  }
}
