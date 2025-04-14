package com.github.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%d", host, port);
        
        config.useSingleServer()
              .setAddress(redisUrl)
              .setDatabase(database);
        
        // 如果设置了密码，则添加密码
        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }
        
        return Redisson.create(config);
    }
}