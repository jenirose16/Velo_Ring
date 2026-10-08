package com.veloring.engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        // Ensure manual ack mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // Configure retry policy for transient errors (e.g. Neo4j failures)
        // 3 retries, 1000ms backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Max retries exceeded for record: {}", record, exception),
                new FixedBackOff(1000L, 3L)
        );
        
        // Ensure the error handler logs failures but doesn't retry infinitely
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }
}
