package org.apache.james.mailbox.events;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class JPAEventDeadLetters implements EventDeadLetters {
    @Override
    public Mono<Void> store(Group registeredGroup, Event failDeliveredEvent) {
        return null;
    }

    @Override
    public Mono<Void> remove(Group registeredGroup, Event.EventId failDeliveredEventId) {
        return null;
    }

    @Override
    public Mono<Event> failedEvent(Group registeredGroup, Event.EventId failDeliveredEventId) {
        return null;
    }

    @Override
    public Flux<Event.EventId> failedEventIds(Group registeredGroup) {
        return null;
    }

    @Override
    public Flux<Group> groupsWithFailedEvents() {
        return null;
    }
}
