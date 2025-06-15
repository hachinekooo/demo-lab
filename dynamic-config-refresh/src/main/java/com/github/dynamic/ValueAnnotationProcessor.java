package com.github.dynamic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.dynamic.beans.PropertyValueElement;
import com.github.dynamic.beans.ValueMetadata;
import com.github.mapper.GlobalConfMapper;
import com.github.mapper.model.GlobalConfDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Value 注解处理器
 *
 * @author wangwenpeng
 * @date 2025/06/04
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 添加这行确保最高优先级
public class ValueAnnotationProcessor implements ApplicationContextAware, BeanFactoryAware, CommandLineRunner, EnvironmentAware {

    private ApplicationContext applicationContext;

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

    public ValueAnnotationProcessor() {
        this.autowiredAnnotationTypes.add(Value.class);
    }

    // 构建元信息
    private ValueMetadata buildMetadata(Class<?> clazz) {
        Class<?> targetClass = clazz;

        // 创建一个 list ，以收集所有的 @ValueElement，也包括父类中的
        List<ValueMetadata.ValueElement> elements = new ArrayList<>();

        do {
            List<ValueMetadata.ValueElement> currElements = new ArrayList<>();

            // 使用 ReflectionUtils.doWithLocalFields() 遍历字
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                MergedAnnotation<?> annotation = findAutowiredAnnotation(field);
                if (annotation != null) {
                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    if (isStatic) { // 静态变量不做处理
                        return;
                    }

                    // 如果有 @Value 注解，包装 成 ValueElement 放入 list
                    PropertyValueElement propertyValueElement = new PropertyValueElement(field);
                    currElements.add(propertyValueElement);
                }

            });

            // Spring的注入遵循从父类到子类的顺序，确保父类的依赖先被满足
            elements.addAll(0, currElements);
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);

        return ValueMetadata.forElements(elements, clazz);
    }

    /**
     * 处理 Value 值注入
     *
     */
    public void processValueInject(String targetBean) {
        if (targetBean == null) { // 对所有 bean 都执行
            // 从 ApplicationContext 获取所有 beans
            String[] allBeansName = applicationContext.getBeanDefinitionNames();
            for (String beanName : allBeansName) {
                Object bean = applicationContext.getBean(beanName);

                try {
                    // 循环构建元信息
                    ValueMetadata valueMetadata = buildMetadata(bean.getClass());
                    // 调用处理注入的方法
                    valueMetadata.processInject(bean, beanName);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        } else { // 对目标 bean 执行
            Object targetBeanObj = applicationContext.getBean(targetBean);

            try {
                // 构建目标 bean 的元信息 ValueMetadata
                ValueMetadata valueMetadata = buildMetadata(targetBeanObj.getClass());
                // 调用 ValueMetadata 中处理注入的方法
                valueMetadata.processInject(targetBeanObj, targetBean);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
        // 收集这个字段上的所有注解
        MergedAnnotations an = MergedAnnotations.from(ao);

        for (Class<? extends Annotation> autowiredAnnotationType : autowiredAnnotationTypes) {
            MergedAnnotation<? extends Annotation> mergedAnnotation = an.get(autowiredAnnotationType);
            if (mergedAnnotation.isPresent()) {
                return mergedAnnotation;
            }
        }

        return null;  // 没找到任何相关注解
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    }

    // ============================= 拉取配置，添加配置源，后期抽离出来做成单独的工具 ====================

    private ConfigurableEnvironment environment;

    private Map<String, Object> dynamicConfigs = new HashMap<>(); // 配置信息缓存

    private static final String DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME = "dynamic-config";

    @Autowired
    private GlobalConfMapper globalConfMapper;

    public boolean loadConfigFromDb(String targetGroup) {
        log.info("====================== Loading configuration from database ======================\n");
        log.info("Starting to fetch dynamic configuration...");

        List<GlobalConfDO> list;
        if (targetGroup == null) {
            list = globalConfMapper.selectList(null);
        } else {
            list = globalConfMapper.selectList(new LambdaQueryWrapper<GlobalConfDO>().eq(GlobalConfDO::getConfGroup, targetGroup));
        }

        Map<String, Object> newConfigs = new HashMap<>();
        for (GlobalConfDO conf : list) {
            newConfigs.put(conf.getConfKey(), conf.getConfValue());
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


    @Override
    public void run(String... args) throws Exception {
        loadConfigFromDb(null);
        addPropertySource();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }
}
