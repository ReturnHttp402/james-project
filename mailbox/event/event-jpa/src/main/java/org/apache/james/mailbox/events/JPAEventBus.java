package org.apache.james.mailbox.events;

import reactor.core.publisher.Mono;

import java.util.Set;

public class JPAEventBus implements EventBus{
    @Override
    public Registration register(MailboxListener listener, RegistrationKey key) {
        return ;
    }

    @Override
    public Registration register(MailboxListener listener, Group group) throws GroupAlreadyRegistered {
        return null;
    }

    @Override
    public Mono<Void> dispatch(Event event, Set<RegistrationKey> key) {
        return null;
    }

    @Override
    public Mono<Void> dispatch(Event event, RegistrationKey key) {
        return null;
    }

    @Override
    public Registration register(MailboxListener.GroupMailboxListener groupMailboxListener) {
        return null;
    }
}
