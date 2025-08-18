package com;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"com.github"})
public class EchoPatternApplication {
    public static void main(String[] args) {
        SpringApplication.run(EchoPatternApplication.class, args);
    }
}
