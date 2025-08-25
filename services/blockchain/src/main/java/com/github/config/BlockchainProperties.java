package com.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
* 区块链相关属性的配置类
* */
@Data
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainProperties {

    private boolean enabled = true; // 是否开启
    private boolean async = true; // 是否异步


    @Data
    public static class TestConfig {

        private boolean mockEnabled = true; // 是否启用Mock模式

        private double mockFailureRate = 0.1; // Mock失败率 (0.0-1.0)
    }

}