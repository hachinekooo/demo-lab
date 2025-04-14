package com.github;

import com.github.bean.ApiResponse;
import com.github.cache.RedisClient;
import com.github.cache.RedisLuaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/article/api/")
public class ArticleController {

    @Value("${prefix.article:article_}")
    private String ARTICLE_PREFIX;

    @Value("${cache.lock.maxRetry:3}")
    private int MAX_RETRY_TIMES; // 最大自旋次数，防止无限循环

    @Value("${cache.lock.sleepTime:200}")
    private long sleepTime; // 睡眠休息时间

    @Value("${prefix.lock:lock_}")
    private String LOCK_PREFIX;

    private final RedisClient redisClient;
    private final RedisLuaUtil redisLuaUtil;

    private final ScheduledExecutorService watchdogExecutor = Executors.newSingleThreadScheduledExecutor();

    @PreDestroy
    public void shutdownWatchdog() {
        watchdogExecutor.shutdownNow();
    }

    @GetMapping("/queryById/{articleId}")
    public ApiResponse queryById(@PathVariable Long articleId) throws InterruptedException {
        int retryTimes = 0;

        while (retryTimes <= MAX_RETRY_TIMES) {
            String article = redisClient.stringGet(ARTICLE_PREFIX + articleId); // 检查缓存

            if (article != null) { // 缓存命中直接返回
                log.info("命中缓存，直接返回");
                return ApiResponse.success(article);
            }

            // 如果已经达到最大重试次数，返回数据库查询结果
            if (retryTimes == MAX_RETRY_TIMES) {
                String queryResult = selectById(articleId);
                redisClient.stringSet(ARTICLE_PREFIX + articleId, queryResult);
                return ApiResponse.success(queryResult);
            }

            String lockKey = LOCK_PREFIX + articleId;
            String lockID = UUID.randomUUID().toString();
            long lockTimeout = 60L;
            Boolean isLocked = redisClient.stringSetIfAbsentWithExpire(lockKey, lockID, lockTimeout, TimeUnit.SECONDS); // 尝试获取锁

            ScheduledFuture<?> watchDogFuture = null;
            try {
                if (isLocked) { // 获取锁成功
                    log.info("线程 {} 获取分布式锁成功：{}", Thread.currentThread() ,LOCK_PREFIX + articleId);

                    // 再次检查缓存，防止其他线程已经填充了缓存
                    String cachedArticle = redisClient.stringGet(ARTICLE_PREFIX + articleId);
                    if (cachedArticle != null) {
                        log.info("双重检查缓存命中，直接返回");
                        return ApiResponse.success(cachedArticle);
                    }

                    watchDogFuture = startWatchDog(lockKey, lockID, lockTimeout, TimeUnit.SECONDS);// 开启watchdog

                    String queryResult = selectById(articleId); // 查询数据库

                    log.info("写入到缓存...");
                    redisClient.stringSet(ARTICLE_PREFIX + articleId, queryResult);

                    return ApiResponse.success(queryResult);
                } else { // 获取锁失败，自旋重试
                    log.info("获取分布式锁 {} 失败，正在进行第 {} 次重试", LOCK_PREFIX + articleId,retryTimes + 1);
                    retryTimes++;
                    Thread.sleep(sleepTime); // 等待一段时间再重试
                }
            } finally {
                if (isLocked) { // 只有成功获取锁的线程才需要释放锁
                    try {
                        int releaseRetry = 0;
                        boolean releaseStatus = false;
                        while (releaseRetry < 3 && !releaseStatus) {
                            try {
                                // 检查锁是否已过期
                                if (redisClient.ttl(lockKey) < 0) {
                                    log.warn("锁已自动过期，无需释放");
                                    releaseStatus = true;
                                    break;
                                }

                                Long result = redisLuaUtil.cad(lockKey, lockID);
                                releaseStatus = result != null && result > 0;
                                log.info("释放锁 {} 结果: {}", LOCK_PREFIX + articleId, result);
                            } catch (Exception e) {
                                log.warn("释放锁异常，重试中...", e);
                                Thread.sleep(100);
                            }
                            releaseRetry++;
                        }
                        if (!releaseStatus) {
                            log.error("最终未能释放锁: {}", lockKey);
                        }
                    } finally {
                        if (watchDogFuture != null) {
                            log.info("watchdog：取消对锁{}的续约检查", lockKey);
                            watchDogFuture.cancel(true); // 强制中断续约任务
                        }
                    }
                }
            }
        }

        return ApiResponse.error(0, "查询时失败");
    }

    private String selectById(Long articleId) throws InterruptedException {
        log.info("查询数据库。。。");
        Thread.sleep(120_000); // 模拟数据库查询延迟
        return "保持谦虚，保持学习，脚踏实地，不好高骛远，先完成，再优化。";
    }

    private ScheduledFuture<?> startWatchDog(String lockKey, String lockId, long timeout, TimeUnit timeUnit) {
        long interval = timeout * 2 / 3; // 60秒的锁，每40秒续约一次
        log.info("启动 watchdog");
        return watchdogExecutor.scheduleAtFixedRate(() -> {
            try {
                log.info("watchdog：对锁{} 进行续约检查", lockKey);

                Long ttl = redisClient.ttl(lockKey); // 获取锁的ttl
                if (ttl == null) {
                    log.warn("watchdog：锁不存在或访问异常，取消续约任务");
                    throw new RuntimeException("锁不存在"); // 触发任务取消
                }

                String lockIdInRedis = redisClient.stringGet(lockKey);
                if (!lockId.equals(lockIdInRedis)) {
                    log.warn("watchdog：锁值不匹配，可能已释放或过期");
                    throw new RuntimeException("锁已失效");
                }

                // 续约操作
                redisClient.expire(lockKey, timeout, timeUnit);
                log.info("watchdog：锁续约成功，TTL重置为 {} {}", timeout, timeUnit);
            } catch (Exception e) {
                log.warn("watchdog：锁续约异常", e);
                throw e;
            }

        }, interval, interval, timeUnit);

    }
}