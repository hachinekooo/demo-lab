package com.github;

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
import java.lang.reflect.Field;

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

    public boolean loadConfigFromDb() {
        log.info("====================== Loading configuration from database ======================");
        log.info("Starting to fetch dynamic configuration...");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.error("Thread interrupted while loading config", e);
        }

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
        log.info("====================== Adding property source ======================");
        log.info("Creating MapPropertySource with name: {}", DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME);

        MapPropertySource dynConfPropertySource = new MapPropertySource(DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME, dynamicConfigs);
        environment.getPropertySources().addFirst(dynConfPropertySource);

        log.info("Property source added with highest priority");
        log.info("Current property sources count: {}", environment.getPropertySources().size());
    }

    public void rebind() {
        log.info("====================== Rebinding configuration properties ======================");
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);

        log.info("Found {} beans with @ConfigurationProperties annotation", beansWithAnnotation.size());

        beansWithAnnotation.forEach((beanName, bean) -> {
            ConfigurationProperties annotation = AnnotationUtils.findAnnotation(bean.getClass(), ConfigurationProperties.class);
            if (annotation != null) {
                log.info("--- Processing bean: {} with prefix: {} ---", beanName, annotation.prefix());

                // 打印重新绑定前的配置值
                printCurrentBeanProperties(beanName, bean, "BEFORE rebinding");

                // 执行重新绑定
                Bindable<?> bindable = Bindable.ofInstance(bean).withAnnotations(annotation);
                bind(bindable);

                // 打印重新绑定后的配置值
                printCurrentBeanProperties(beanName, bean, "AFTER rebinding");
            }
        });

        log.info("Configuration rebinding completed");
    }

    private void printCurrentBeanProperties(String beanName, Object bean, String stage) {
        log.info("=== {} - {} ===", stage, beanName);
        try {
            // 使用反射获取所有字段
            Field[] fields = bean.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(bean);
                log.info("  {}: {} ({})",
                        field.getName(),
                        value,
                        value != null ? value.getClass().getSimpleName() : "null"
                );
            }
        } catch (Exception e) {
            log.warn("Failed to read properties for bean: {}, error: {}", beanName, e.getMessage());
        }
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