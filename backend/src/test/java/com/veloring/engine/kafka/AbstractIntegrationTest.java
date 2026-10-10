package com.veloring.engine.kafka;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
    "management.server.port=0",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "veloring.detection.fixed-delay-ms=500",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.properties.max.block.ms=5000"
})
public abstract class AbstractIntegrationTest {

    @org.springframework.beans.factory.annotation.Value("${veloring.kafka.topic:payment-events}")
    protected String topic;

    static final KafkaContainer kafka;
    static final Neo4jContainer<?> neo4j;

    static {
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
        neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.9.0"))
                .withoutAuthentication();
        
        kafka.start();
        neo4j.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        String uniqueId = java.util.UUID.randomUUID().toString();
        registry.add("veloring.kafka.topic", () -> "events-" + uniqueId);
        registry.add("veloring.kafka.group-id", () -> "group-" + uniqueId);
        
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("veloring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("veloring.neo4j.username", () -> "");
        registry.add("veloring.neo4j.password", () -> "");
        registry.add("management.health.neo4j.enabled", () -> "false");
        registry.add("management.health.kafka.enabled", () -> "false");
    }
}
