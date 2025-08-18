package com.github;

import com.github.bean.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/article/api/")
public class ArticleController2 {

    @Value("${prefix.article:article_}")
    private String ARTICLE_PREFIX;

    @Value("${prefix.lock:lock_}")
    private String LOCK_PREFIX;

    @Value("${cache.lock.waitTime:3}")
    private long lockWaitTime; // 获取锁等待时间(秒)

    @Value("${cache.lock.leaseTime:30}")
    private long lockLeaseTime; // 锁持有时间(秒)

    private final RedissonClient redissonClient;

    @GetMapping("/queryById2/{articleId}")
    public ApiResponse queryById(@PathVariable Long articleId) {
        String cacheKey = ARTICLE_PREFIX + articleId;

        // 检查缓存
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String article = bucket.get();
        if (article != null) {
            log.info("命中缓存，直接返回");
            return ApiResponse.success(article);
        }

        // 定义分布式锁的key
        String lockKey = LOCK_PREFIX + articleId;
        // 获取Redisson的锁实例
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，在 lockWaitTime 时间内自旋重试，如果超过这个时间还设置失败就返回false
            // Redisson的锁会自动续期，默认锁TTL = 30，自动续期的时候也重置TTL = 30，所以不需要手动实现看门狗
            boolean isLocked = lock.tryLock(lockWaitTime, TimeUnit.SECONDS);

            if (isLocked) {
                log.info("线程 {} 获取分布式锁成功：{}", Thread.currentThread(), lockKey);

                // 双重检查，防止在获取锁的过程中其他线程已经设置了缓存
                article = bucket.get();
                if (article != null) {
                    log.info("获取锁后二次检查缓存命中，直接返回");
                    return ApiResponse.success(article);
                }

                try {
                    // 查询数据库
                    String queryResult = selectById(articleId);

                    // 写入缓存
                    log.info("线程 {} 正在操作写入缓存...", Thread.currentThread());
                    bucket.set(queryResult);

                    log.info("线程 {} 查询数据库并写入缓存成功，返回查询结果", Thread.currentThread());
                    return ApiResponse.success(queryResult);
                } finally {
                    // 释放锁，Redisson会自动处理锁的释放
                    // 即使在处理过程中发生异常，也会释放锁
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("线程 {} 释放分布式锁：{}", Thread.currentThread(), lockKey);
                    }
                }
            } else {
                log.info("线程 {} 获取分布式锁 {} 失败，返回数据库查询结果", Thread.currentThread(), lockKey);
                // 获取锁失败时，直接查询数据库并返回结果，但不缓存
                String queryResult = selectById(articleId);
                return ApiResponse.success(queryResult);
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁过程中被中断", e);
            Thread.currentThread().interrupt();
            return ApiResponse.error(0, "查询时被中断");
        } catch (Exception e) {
            log.error("查询出现异常", e);
            return ApiResponse.error(0, "查询时发生异常");
        }
    }

    private String selectById(Long articleId) throws InterruptedException {
        log.info("线程 {} 查询数据库...", Thread.currentThread());
        long start = System.currentTimeMillis();
        TimeUnit.SECONDS.sleep(35); // 休眠35秒
        long duration = (System.currentTimeMillis() - start) / 1000;
        System.out.println("查询数据库耗时: " + duration + "秒");
        return "保持谦虚，保持学习，脚踏实地，不好高骛远，先完成，再优化。";
    }
}