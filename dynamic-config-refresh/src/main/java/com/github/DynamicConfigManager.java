package com.github;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态配置管理器，负责拉取配置、封装配置与调用绑定器
 *
 * @author wangwenpeng
 * @date 2025/06/03
 */
@Slf4j
@Component
public class DynamicConfigManager implements EnvironmentAware, ApplicationContextAware, CommandLineRunner {

    private static final String DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME = "dynamic-config";

    private ConfigurableEnvironment environment;

    private ApplicationContext applicationContext;

    private Map<String, Object> dynamicConfigs = new HashMap<>();

    @Autowired
    private DynamicConfigBinder dynamicConfigBinder;

    public void reloadConfig() {
        String before = JSONUtil.toJsonStr(dynamicConfigs);
        boolean toRefresh = loadConfigFromDb();
        if (toRefresh) {
            rebind();
            log.info("配置刷新! 旧:{}, 新:{}", before, JSONUtil.toJsonStr(dynamicConfigs));
        }
    }

    public boolean loadConfigFromDb() {
        log.info("====================== Loading configuration from database ======================\n");
        log.info("Starting to fetch dynamic configuration...");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.error("Thread interrupted while loading config", e);
        }

        dynamicConfigs.clear(); // 先清空

        // 再从数据库拉取，这里使用模拟数据
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("test.config.name", "ApplicationName_DB");
        hashMap.put("test.config.version", 2);
        hashMap.put("test.config.enabled", true);

        if (!hashMap.isEmpty()) {
            dynamicConfigs = hashMap;
            log.info("Successfully loaded {} configuration properties:", dynamicConfigs.size());
            dynamicConfigs.forEach((key, value) ->
                    log.info("  {} = {} ({})", key, value, value.getClass().getSimpleName())
            );
            log.info("Configuration loading completed successfully");
            return true;
        }

        log.warn("No configuration found in database");
        return false;
    }

    public void addPropertySource() {
        log.info("====================== Adding property source ======================\n");
        log.info("Creating MapPropertySource with name: {}", DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME);

        int preSize = environment.getPropertySources().size();

        MapPropertySource dynConfPropertySource = new MapPropertySource(DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME, dynamicConfigs);
        environment.getPropertySources().addFirst(dynConfPropertySource);

        log.info("Property source added with highest priority");
        log.info("Current property sources count: {} -> {}", preSize, environment.getPropertySources().size());
    }

    public void rebind() {
        log.info("====================== Rebinding configuration properties ======================\n");
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);

        log.info("Found {} beans with @ConfigurationProperties annotation", beansWithAnnotation.size());

        beansWithAnnotation.forEach((beanName, bean) -> {
            ConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
            if (annotation != null) {
                // 执行重新绑定
                Bindable<?> bindable = Bindable.ofInstance(bean).withAnnotations(annotation);
                dynamicConfigBinder.bind(bindable);
            }
        });

        log.info("Configuration rebinding completed");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }


    /*
    * 在 Spring Boot 完全启动后才执行在 Spring Boot 完全启动后才执行
    * yaml 中的默认值得以保留，作为默认值使用
    * */
    @Override
    public void run(String... args) throws Exception {
        loadConfigFromDb();
        addPropertySource();
        rebind();
    }
}