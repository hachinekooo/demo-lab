package com.github;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class DynamicConfigManager implements EnvironmentAware, ApplicationContextAware, CommandLineRunner {

    private static final String DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME = "dynamic-config";

    private ConfigurableEnvironment environment;

    private ApplicationContext applicationContext;

    private volatile Binder binder;

    private PropertySources propertySources;

    private Map<String, Object> dynamicConfigs = new HashMap<>();

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

        dynamicConfigs.clear();

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
                bind(bindable);
            }
        });

        log.info("Configuration rebinding completed");
    }


    private <T> void bind(Bindable<T> bindable) {
        ConfigurationProperties annotation = bindable.getAnnotation(ConfigurationProperties.class);
        if (annotation != null) {
            BindHandler bindHandler = getBindHandler(annotation);
            getBinder().bind(annotation.prefix(), bindable, bindHandler);
        }
    }

    private BindHandler getBindHandler(ConfigurationProperties annotation) {
        BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
        if (annotation.ignoreInvalidFields()) { // 如果在配置属性时发现目标对象中有无效字段，则忽略这些无效字段
            handler = new IgnoreErrorsBindHandler(handler);
        }
        if (!annotation.ignoreUnknownFields()) { // 如果在配置属性时发现目标对象中有未知字段，则忽略这些未知字段
            UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
            handler = new NoUnboundElementsBindHandler(handler, filter);
        }
        return handler;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
        this.propertySources = ((ConfigurableEnvironment) environment).getPropertySources();
    }


    // ================================= 初始化 Spring Binder =============================

    /**
     * @return 初始化 Spring Binder 对象
     */
    private Binder getBinder() {
        if (this.binder == null) {
            synchronized (this) {
                if (this.binder == null) {
                    this.binder = new Binder(
                            getConfigurationPropertySources(),
                            getPropertySourcesPlaceholdersResolver(),
                            getConversionService(),
                            getPropertyEditorInitializer());
                }
            }
        }
        return this.binder;
    }

    private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
        return ConfigurationPropertySources.from(this.propertySources);
    }

    /**
     * 指定占位符的前缀、后缀、默认值分隔符、未解析忽略、环境变量容器
     *
     * @return
     */
    private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
        return new PropertySourcesPlaceholdersResolver(this.propertySources);
    }

    /**
     * 类型转换
     *
     * @return
     */
    private ConversionService getConversionService() {
        return new DefaultConversionService();
    }

    private Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
        if (this.applicationContext instanceof ConfigurableApplicationContext) {
            return ((ConfigurableApplicationContext) this.applicationContext)
                    .getBeanFactory()::copyRegisteredEditorsTo;
        }
        return null;
    }

    // 在 Spring Boot 完全启动后才执行在 Spring Boot 完全启动后才执行
    @Override
    public void run(String... args) throws Exception {
        loadConfigFromDb();
        addPropertySource();
        rebind();
    }
}