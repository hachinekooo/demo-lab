package com.github;

import com.github.config.TestConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class DynamicConfigTest {
    @Autowired
    private TestConfigProperties testConfigProperties;

    @PostConstruct
    public void init() throws InterruptedException {
        log.info("Initial configuration values:");
        printCurrentConfigValues();
        Thread.sleep(5000);
        printCurrentConfigValues();
    }

    private void printCurrentConfigValues() {

    }

}