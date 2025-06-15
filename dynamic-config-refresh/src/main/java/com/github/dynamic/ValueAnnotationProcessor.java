package com.github.dynamic;

import com.github.dynamic.beans.ValueMetadata;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Value 注解处理器
 *
 * @author wangwenpeng
 * @date 2025/06/04
 */
@Component
public class ValueAnnotationProcessor implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

    public ValueAnnotationProcessor() {
        this.autowiredAnnotationTypes.add(Value.class);
    }


    // 构建元信息
    private ValueMetadata buildMetadata(Class<?> clazz) {
        // 检查类中是否含有目标注解 @Value

        // 创建一个 list ，以收集所有的 @ValueElement，也包括父类中的

        // 使用 ReflectionUtils.doWithFields() 遍历字

        // 如果有 @Value 注解，包装 成 ValueElement 放入 list


        return null;
    }


    /**
     * 处理 Value 值注入
     *
     */
    public void processValueInject(String targetGroup) {
        if (targetGroup == null) { // 对所有 bean 都执行
            // 从 ApplicationContext 获取所有 beans

            // 循环构建元信息，并调用处理注入的方法

        } else { // 对目标 bean 执行
            // 构建目标 bean 的元信息 ValueMetadata

            // 调用 ValueMetadata 中处理注入的方法
        }
    }

    private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {

        return null;  // 没找到任何相关注解
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
