package com.github.cache;

import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class RedisClient {
    private static final Charset CODE = StandardCharsets.UTF_8;

    @Value("${prefix.demo}")
    private String DEMO_FIX;

    @Resource
    private  RedisTemplate<String, String> template;

    public void register(RedisTemplate template) {
        this.template = template;
    }

    public void nullCheck(Object... args) {
        for (Object arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("参数不能为空！");
            }
        }
    }


    /**
     * @param key redis key
     * @return 字节数组
     */
    public byte[] keyBytes(String key) {
        nullCheck(key);
        key = DEMO_FIX + key;
        return key.getBytes(CODE);
    }

    /**
     *
     * @param val redis value
     * @return 字节数组
     * @param <T> value的类型
     */
    public <T> byte[] valBytes(T val) {
        nullCheck(val);

        if (val instanceof String) {
            return ((String) val).getBytes(CODE);
        } else {
            return JSONUtil.toJsonStr(val).getBytes(CODE);
        }

    }

    /**
     *
     * 删除缓存
     *
     * @param key redis key
     */
    public void del(String key) {
        template.execute((RedisCallback<Long>) con -> con.del(keyBytes(key)));
    }

    /**
     *
     * 设置一个 k v
     *
     * @param key key
     * @param value value
     */
    public void stringSet(String key, String value) {
        template.execute(
                new RedisCallback<Void>() {
                    @Override
                    public Void doInRedis(RedisConnection connection) throws DataAccessException {
                        connection.set(keyBytes(key), valBytes(value));
                        return null;
                    }
                }
        );
    }

    /**
     *
     * 设置一个带有过期时间的 k v
     *
     * @param key key
     * @param value value
     */
    public void stringSetWithExpire(String key, String value, Long expire) {
        template.execute(
                (RedisCallback<Void>) connection -> {
                    connection.setEx(keyBytes(key), expire, valBytes(value));
                    return null;
                }
        );
    }

    /**
     * 如果 key 不存在，则设置 key-value 并设置过期时间。
     *
     * @param key    Redis key
     * @param value  Redis value
     * @param expire 过期时间（秒）
     * @return 如果设置成功返回 true，如果 key 已存在则返回 false
     */
    public Boolean stringSetIfAbsentWithExpire(String key, String value, Long expire, TimeUnit timeUnit) {
        return template.execute((RedisCallback<Boolean>) connection -> {
            // 使用 Redis 的 set 命令，NX 表示仅在 key 不存在时设置，EX 表示设置过期时间（单位为秒）
            return connection.set(keyBytes(key), valBytes(value), Expiration.from(expire, timeUnit), RedisStringCommands.SetOption.SET_IF_ABSENT);
        });
    }


    /**
     *
     * 获取 k 的 value
     *
     * @param key redis key
     * @return redis value
     */
    public String stringGet(String key) {
        return template.execute((RedisCallback<String>) connection -> {
            byte[] valBytes = connection.get(keyBytes(key));
            return valBytes == null ? null : new String(valBytes);
        });
    }

    public void expire(String key, Long expire, TimeUnit timeUnit) {
        template.execute((RedisCallback<Void>) connection -> {
            connection.expire(keyBytes(key), expire);
            return null;
        });
    }

    /**
     *
     * 获取过期时间
     *
     * @param key
     * @return
     */
    public Long ttl(String key) {
        return template.execute((RedisCallback<Long>) connection -> connection.ttl(keyBytes(key)));
    }

}
