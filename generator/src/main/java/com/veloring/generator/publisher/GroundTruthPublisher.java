package com.veloring.generator.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloring.generator.config.GeneratorProperties;
import com.veloring.generator.model.GroundTruthRecord;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Component
@Slf4j
public class GroundTruthPublisher {
    private final ObjectMapper objectMapper;
    private final BufferedWriter writer;

    public GroundTruthPublisher(GeneratorProperties properties, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        this.writer = new BufferedWriter(new FileWriter(properties.getGroundTruthFile(), false));
    }

    public synchronized void publish(GroundTruthRecord record) {
        try {
            writer.write(objectMapper.writeValueAsString(record));
            writer.newLine();
        } catch (IOException e) {
            log.error("Failed to write ground truth record", e);
        }
    }

    @PreDestroy
    public void close() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Failed to close ground truth writer", e);
        }
    }
}
