/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.kafka.integration;

import java.util.Collections;
import java.util.UUID;

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.integration.common.KafkaTestUtil;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.idempotent.kafka.KafkaIdempotentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.apache.camel.component.kafka.serde.KafkaSerdeHelper.numericHeader;

@DisabledIfSystemProperty(named = "enable.kafka.consumer.idempotency.tests", matches = "false")
public class KafkaConsumerIdempotentGroupIdIT extends KafkaConsumerIdempotentTestSupport {

    private static final String TOPIC;
    private static final String REPOSITORY_TOPIC;
    private final int size = 200;

    static {
        UUID topicId = UUID.randomUUID();
        TOPIC = "idempt_" + topicId;
        REPOSITORY_TOPIC = "TEST_IDEMPOTENT_" + topicId;
    }

    @BeforeAll
    public static void createRepositoryTopic() {
        KafkaTestUtil.createTopic(service, REPOSITORY_TOPIC, 1);
    }

    @AfterAll
    public static void removeRepositoryTopic() {
        kafkaAdminClient.deleteTopics(Collections.singleton(REPOSITORY_TOPIC)).all();
    }

    @BindToRegistry("kafkaIdempotentRepository")
    private final KafkaIdempotentRepository testIdempotent
            = new KafkaIdempotentRepository(REPOSITORY_TOPIC, getBootstrapServers(), "test_1");

    @BeforeEach
    public void before() {
        doSend(size, TOPIC);
    }

    @AfterEach
    public void after() {
        kafkaAdminClient.deleteTopics(Collections.singleton(TOPIC)).all();
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {

            @Override
            public void configure() {
                from("kafka:" + TOPIC
                     + "?groupId=KafkaConsumerIdempotentIT&autoOffsetReset=earliest"
                     + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
                     + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
                     + "&autoCommitIntervalMs=1000&pollTimeoutMs=1000&autoCommitEnable=true"
                     + "&interceptorClasses=org.apache.camel.component.kafka.MockConsumerInterceptor").routeId("foo")
                        .idempotentConsumer(numericHeader("id"))
                        .idempotentRepository("kafkaIdempotentRepository")
                        .to(KafkaTestUtil.MOCK_RESULT);
            }
        };
    }

    @Test
    @DisplayName("Numeric headers is consumable when using idempotent (CAMEL-16914)")
    void kafkaIdempotentMessageIsConsumedByCamel() {
        MockEndpoint to = contextExtension.getMockEndpoint(KafkaTestUtil.MOCK_RESULT);

        doRun(to, size);
    }
}
