package com.github.dynamic;

import cn.hutool.json.JSONUtil;
import com.github.mapper.GlobalConfMapper;
import com.github.utils.ConfigUtils;
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
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

    private Map<String, Object> dynamicConfigs = new HashMap<>(); // 配置信息缓存

    private Map<Class, Runnable> refreshCallBack = new HashMap<>(); // 回调任务

    @Autowired
    private DynamicConfigBinder dynamicConfigBinder;
    @Autowired
    private GlobalConfMapper globalConfMapper;

    public void reloadConfig(String group) {
        if (group == null) {
            String before = JSONUtil.toJsonStr(dynamicConfigs);
            boolean isUpdated = loadConfigFromDb(null);
            if (isUpdated) { // 配置信息有更新，才需要重新绑定
                addPropertySource();
                rebind(null); // 全量绑定
                log.info("全量配置刷新! 旧:{}, 新:{}", before, JSONUtil.toJsonStr(dynamicConfigs));
            }
        } else {
            String before = JSONUtil.toJsonStr(dynamicConfigs);
            boolean isUpdated = loadConfigFromDb(group);
            if (isUpdated) {
                rebind(group);
                log.info("增量配置刷新! 旧:{}, 新:{}", before, JSONUtil.toJsonStr(dynamicConfigs));
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
        Map<String, Object> newConfigs = ConfigUtils.loadConfigFromDb(globalConfMapper, targetGroup);

        // 如果配置没有发生变化，则直接返回 false
        if (!ConfigUtils.hasConfigChanged(dynamicConfigs, newConfigs)) { return false; }

        return ConfigUtils.updateConfigs(targetGroup, newConfigs, dynamicConfigs);
    }

    public void addPropertySource() {
        ConfigUtils.addPropertySource(environment, dynamicConfigs);
    }

    public void rebind(String group) {
        log.info("====================== Rebinding configuration properties ======================\n");

        // 找到所有添加了 ConfigurationProperties 注解的 bean
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);


        if (group == null) {  // 全量刷新
            beansWithAnnotation.values().forEach(bean -> {
                rebindBean(bean);
                // 如果该 Bean 的类在 refreshCallback 中有对应的回调任务，则执行该回调任务
                executeCallback(bean);
            });
        } else { // 部分刷新
            beansWithAnnotation.values().stream()
                    .filter(bean -> Objects.equals(bean.getClass().getSimpleName(), group))
                    .forEach(bean1 -> {
                        rebindBean(bean1);
                        // 如果该 Bean 的类在 refreshCallback 中有对应的回调任务，则执行该回调任务
                        executeCallback(bean1);
                    });
        }

        log.info("Configuration rebinding completed");
    }

    private void executeCallback(Object bean) {
        if (refreshCallBack.containsKey(bean.getClass())) {
            refreshCallBack.get(bean.getClass()).run();
        }
    }

    private void rebindBean(Object bean) {
        ConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
        if (annotation != null) {
            // 执行重新绑定
            Bindable<?> bindable = Bindable.ofInstance(bean).withAnnotations(annotation);
            dynamicConfigBinder.bind(bindable);
        }
    }

    public void registerRefreshCallback(Object bean, Runnable run) {
        refreshCallBack.put(bean.getClass(), run);
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
    * 在所有bean创建完成并且应用上下文完全设置好之后执行此方法。在Spring应用的run方法完成之前执行
    * yaml 中的默认值得以保留，作为默认值使用
    * */
    @Override
    public void run(String... args) throws Exception {
        reloadConfig(null);
    }
}