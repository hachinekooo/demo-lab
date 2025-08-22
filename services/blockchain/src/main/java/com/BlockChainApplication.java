package com;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"com.github"})
@MapperScan("com.github.mapper")
public class BlockChainApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockChainApplication.class, args);
    }
}
