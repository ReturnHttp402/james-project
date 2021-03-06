/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.backend.rabbitmq;

import static org.apache.james.backend.rabbitmq.RabbitMQFixture.DEFAULT_MANAGEMENT_CREDENTIAL;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apache.james.util.concurrent.NamedThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nurkiewicz.asyncretry.AsyncRetryExecutor;

class RabbitMQConnectionFactoryTest {

    private ScheduledExecutorService scheduledExecutor;

    @BeforeEach
    void setUp() throws Exception {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.withClassName(getClass()));
    }

    @Test
    void creatingAFactoryShouldWorkWhenConfigurationIsValid() {
        RabbitMQConfiguration rabbitMQConfiguration = RabbitMQConfiguration.builder()
            .amqpUri(URI.create("amqp://james:james@rabbitmq_host:5672"))
            .managementUri(URI.create("http://james:james@rabbitmq_host:15672/api/"))
            .managementCredentials(DEFAULT_MANAGEMENT_CREDENTIAL)
            .build();

        new RabbitMQConnectionFactory(rabbitMQConfiguration, new AsyncRetryExecutor(scheduledExecutor));
    }

    @Test
    void creatingAFactoryShouldThrowWhenConfigurationIsInvalid() {
        RabbitMQConfiguration rabbitMQConfiguration = RabbitMQConfiguration.builder()
            .amqpUri(URI.create("badprotocol://james:james@rabbitmq_host:5672"))
            .managementUri(URI.create("http://james:james@rabbitmq_host:15672/api/"))
            .managementCredentials(DEFAULT_MANAGEMENT_CREDENTIAL)
            .build();

        assertThatThrownBy(() -> new RabbitMQConnectionFactory(rabbitMQConfiguration, new AsyncRetryExecutor(scheduledExecutor)))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createShouldFailWhenConnectionCantBeDone() {
        RabbitMQConfiguration rabbitMQConfiguration = RabbitMQConfiguration.builder()
                .amqpUri(URI.create("amqp://james:james@rabbitmq_host:5672"))
                .managementUri(URI.create("http://james:james@rabbitmq_host:15672/api/"))
                .managementCredentials(DEFAULT_MANAGEMENT_CREDENTIAL)
                .maxRetries(1)
                .minDelay(1)
                .build();

        RabbitMQConnectionFactory rabbitMQConnectionFactory = new RabbitMQConnectionFactory(rabbitMQConfiguration, new AsyncRetryExecutor(scheduledExecutor));

        assertThatThrownBy(() -> rabbitMQConnectionFactory.create())
            .isInstanceOf(RuntimeException.class);
    }
}
