package org.apache.james.mailbox.events;

import org.apache.james.metrics.api.NoopMetricFactory;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public class JPAEventBusTest implements KeyContract.SingleEventBusKeyContract, GroupContract.SingleEventBusGroupContract,
        ErrorHandlingContract{

    private JPAEventBus eventBus;
    private JPAEventDeadLetters deadLetters;

    @BeforeEach
    void setUp() {
        deadLetters = new JPAEventDeadLetters();
        eventBus = new JPAEventBus(
                new InVmEventDelivery(new NoopMetricFactory()), RetryBackoffConfiguration.DEFAULT, deadLetters);
    }

    @Override
    public EventBus eventBus() {
        return eventBus;
    }

    @Override
    public EventDeadLetters deadLetter() {
        return deadLetters;
    }
}