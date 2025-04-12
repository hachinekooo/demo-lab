package com.github.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * Redis工具类中实现一个用于原子性地比较并删除的方法
 */
@Component
public class RedisLuaUtil {

    @Value("${prefix.demo}")
    private String DEMO_FIX;


    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    public void register(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 比较并删除 - Compare And Delete
     * 只有当key存在且value匹配时才删；
     * @param key Redis键
     * @param expectedValue 期望的值
     * @return 1-删除成功；0-值不匹配或key不存在
     */
    public Long cad(String key, String expectedValue) {
        // 确保使用String序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        String prefixed_key = DEMO_FIX + key;
        String script =
                "local value = redis.call('get', KEYS[1]); " +
                        "if value == ARGV[1] then " +
                        "    return redis.call('del', KEYS[1]); " +
                        "else " +
                        "    return 0; " +
                        "end";

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(prefixed_key),
                expectedValue);

        if (result == 0) {
            throw new RuntimeException("释放锁失败：key=" + prefixed_key + "，expectedValue=" + expectedValue + "，实际值=" + redisTemplate.opsForValue().get(prefixed_key));

        }
        return result;
    }
}