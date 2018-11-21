package bio.overture.ego.reactor.events;

import bio.overture.ego.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class UserEvents {

  // EVENT NAMES
  public static String UPDATE = UserEvents.class.getName() + ".UPDATE";

  @Autowired
  private EventBus eventBus;

  public void update(User user) {
    eventBus.notify(
      UserEvents.UPDATE,
      Event.wrap(user)
    );
  }

}
