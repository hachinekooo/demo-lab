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

import java.util.UUID;
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

    @Value("${cache.lock.waitTime:200}")
    private long lockWaitTime; // 自旋等待时间

    @Value("${prefix.lock:lock_}")
    private String LOCK_PREFIX;

    private final RedisClient redisClient;
    private final RedisLuaUtil redisLuaUtil;



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
            Boolean isLocked = redisClient.stringSetIfAbsentWithExpire(lockKey, lockID, 60L, TimeUnit.SECONDS); // 尝试获取锁

            try {
                if (isLocked) { // 获取锁成功
                    log.info("线程 {} 获取分布式锁成功：{}", Thread.currentThread() ,LOCK_PREFIX + articleId);

                    String queryResult = selectById(articleId); // 查询数据库

                    log.info("写入到缓存...");
                    redisClient.stringSet(ARTICLE_PREFIX + articleId, queryResult);

                    return ApiResponse.success(queryResult);
                } else { // 获取锁失败，自旋重试
                    log.info("获取分布式锁 {} 失败，正在进行第 {} 次重试", LOCK_PREFIX + articleId,retryTimes + 1);
                    retryTimes++;
                    Thread.sleep(lockWaitTime); // 等待一段时间再重试
                }
            } finally {
                if (isLocked) { // 只有成功获取锁的线程才需要释放锁
                    Long result = redisLuaUtil.cad(lockKey, lockID);
                    log.info("释放锁 {} 结果: {}", LOCK_PREFIX + articleId, result);
                }
            }
        }

        return ApiResponse.error(0, "查询时失败");
    }

    private static String selectById(Long articleId) throws InterruptedException {
        log.info("查询数据库。。。");
        Thread.sleep(500); // 模拟数据库查询延迟
        return "保持谦虚，保持学习，脚踏实地，不好高骛远，先完成，再优化。";
    }
}