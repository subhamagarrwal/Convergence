package com.subham.convergence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConvergenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConvergenceApplication.class, args);
    }
}