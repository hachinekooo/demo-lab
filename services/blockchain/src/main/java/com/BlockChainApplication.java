package com;

import com.github.config.BlockchainProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"com.github"}) // 组建扫描
@MapperScan("com.github.core.mapper") // Mapper 扫描
@EnableConfigurationProperties(BlockchainProperties.class) // 注册配置类
public class BlockChainApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockChainApplication.class, args);
    }
}
