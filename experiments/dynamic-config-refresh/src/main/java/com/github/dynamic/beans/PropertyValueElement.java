package com.github.dynamic.beans;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

public class PropertyValueElement extends ValueMetadata.ValueElement {

    private ConfigurableListableBeanFactory beanFactory;

    public PropertyValueElement(Field field, ConfigurableListableBeanFactory beanFactory) {
        super(field);
        this.beanFactory = beanFactory;
    }

    @Override
    protected void inject(Object target, String requestingBeanName) {
        // 将 field 设置为可以访问，避免 private 的问题
        ReflectionUtils.makeAccessible(field);

        // 解析想要对 field 设置的目标值
        Object value = resolveFieldValue(field, target, requestingBeanName);

        // 赋值
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Object resolveFieldValue(Field field, Object bean, String beanName) {
        /*
        * 1.创建依赖描述符，包装字段信息，描述这个字段的依赖关系
        * */
        DependencyDescriptor desc = new DependencyDescriptor(field, true);

        // 2.设置包含类，提供类型上下文
        desc.setContainingClass(bean.getClass());

        // 3.搞一个 Spring 的类型转换器
        if (beanFactory == null) {
            throw new IllegalStateException("BeanFactory is not initialized");
        }
        TypeConverter converter = beanFactory.getTypeConverter();

        /*
         * 4.用于检测循环依赖的 Set
         * Spring会把解析过程中用到的Bean名称放进去，
         * 解析完成后，autowiredBeanNames 就包含了所有相关的Bean名称
         * */
        Set<String> autowiredBeanNames = new LinkedHashSet<>(1);

        // 5.使用 beanFactory 解析依赖
        Object valueObj;
        valueObj = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, converter);

        return valueObj;
    }
}
