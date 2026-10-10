package com.veloring.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VeloRingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeloRingApplication.class, args);
    }
}
