package com.veloring.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VeloRingGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(VeloRingGeneratorApplication.class, args);
    }
}
