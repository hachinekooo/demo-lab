package com.github.service;

import cn.hutool.extra.spring.SpringUtil;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public void reloadConfig(String group) {
        if (group == null) {
            loadConfigFromDb(null); // 全量加载
            addPropertySource();
            rebind(null); // 全量绑定
        } else {
            String before = JSONUtil.toJsonStr(dynamicConfigs);
            boolean toRefresh = loadConfigFromDb(group);
            if (toRefresh) {
                rebind(group);
                log.info("配置刷新! 旧:{}, 新:{}", before, JSONUtil.toJsonStr(dynamicConfigs));
            }
        }
    }

    /**
     * 从数据库加载配置信息
     *
     * @param targetGroup 目标分组，null 表示加载所有分组
     * @return boolean
     */
    public boolean loadConfigFromDb(String targetGroup) {
        log.info("====================== Loading configuration from database ======================\n");
        log.info("Starting to fetch dynamic configuration...");

        String sql = targetGroup != null ?
                "SELECT `key`, `value`, `group`FROM global_conf WHERE `group` = ? AND deleted = 0" :
                "SELECT `key`, `value`, `group` FROM global_conf WHERE deleted = 0";

        List<Map<String, Object>> list = targetGroup != null ?
                SpringUtil.getBean(JdbcTemplate.class).queryForList(sql, targetGroup) :
                SpringUtil.getBean(JdbcTemplate.class).queryForList(sql);

        Map<String, Object> newConfigs = new HashMap<>();
        for (Map<String, Object> row : list) {
            String key = (String) row.get("key");
            String value = (String) row.get("value");
            newConfigs.put(key, value);

            String group = (String) row.get("group");
        }

        // 如果 targetGroup 为null（全量加载），且拉取到的配置信息不为空
        if (targetGroup == null && !newConfigs.isEmpty()) {
            dynamicConfigs = newConfigs;
            log.info("Successfully loaded {} configuration properties:", dynamicConfigs.size());
            return true;
        }

        // 如果 targetGroup 不为null（部分加载），且拉取到的配置信息不为空
        if (targetGroup != null && !newConfigs.isEmpty()) {
            // 更新 dynamicConfigs 中相应的配置项
            dynamicConfigs.putAll(newConfigs);
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

    public void rebind(String group) {
        log.info("====================== Rebinding configuration properties ======================\n");

        // 找到所有添加了 ConfigurationProperties 注解的 bean
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);


        if (group == null) {  // 全量刷新
            beansWithAnnotation.forEach((beanName, bean) -> {
                ConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
                if (annotation != null) {
                    // 执行重新绑定
                    Bindable<?> bindable = Bindable.ofInstance(bean).withAnnotations(annotation);
                    dynamicConfigBinder.bind(bindable);
                }
            });
        } else { // 部分刷新
            for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
                Object bean = entry.getValue(); // bean 对象
                String className = bean.getClass().getSimpleName();

                if (Objects.equals(className, group)) {
                    ConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
                    if (annotation != null) {
                        Bindable<?> bindable = Bindable.ofInstance(bean).withAnnotations(annotation);
                        dynamicConfigBinder.bind(bindable);
                    }
                }
            }
        }

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
        reloadConfig(null);
    }
}