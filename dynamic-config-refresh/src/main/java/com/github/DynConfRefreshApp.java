package com.github;

import com.github.config.TestConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@MapperScan("com.github.mapper")
@SpringBootApplication
@EnableConfigurationProperties({TestConfigProperties.class})
public class DynConfRefreshApp {
    public static void main(String[] args) {
        SpringApplication.run(DynConfRefreshApp.class, args);
    }
}
