package com.qiao.flow.orchestrator.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.qiao.flow.orchestrator.example",
        "com.qiao.flow.orchestrator.core", "com.qiao.flow.orchestrator.starter"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
