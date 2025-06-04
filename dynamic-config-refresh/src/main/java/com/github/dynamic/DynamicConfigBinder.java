package com.github.dynamic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 动态配置绑定器，负责重新绑定属性
 *
 * @author wangwenpeng
 * @date 2025/06/03
 */
@Slf4j
@Component
public class DynamicConfigBinder implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    private PropertySources propertySources;

    private volatile Binder binder;


    public  <T> void bind(Bindable<T> bindable) {
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

    /**
     * 初始化 Spring Binder 对象
     *
     * @return Spring Binder 对象
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.propertySources = ((ConfigurableApplicationContext) applicationContext).getEnvironment().getPropertySources();
    }
}