package com.github.dynamic.beans;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

/**
 * 容器类，保存某个类上所有需要进行值注入的信息
 *
 * @author wangwenpeng
 * @date 2025/06/15
 */
public class ValueMetadata {
    private final Class<?> targetClass; // 目标类

    private final Collection<ValueElement> valueElements; // 所有需要注入的元素


    /**
     * 处理注入
     *
     * @param target
     * @param beanName
     * @throws Throwable
     */
    public void processInject(Object target, String beanName) throws Throwable {
        // 遍历 valueElements 、逐个调用 inject()
    }

    public ValueMetadata(Class<?> targetClass, Collection<ValueElement> injectedElements) {
        this.targetClass = targetClass;
        this.valueElements = injectedElements;
    }

    /**
     * 用来表示需要‘值注入元素’信息的抽象类
     *
     * @author wangwenpeng
     * @date 2025/06/15
     */
    public abstract static class ValueElement {

        protected final Field field;

        protected ValueElement(Field field) {
            this.field = field;
        }


        /**
         * 执行真正注入的方法
         *
         * @param target 目标类
         * @param requestingBeanName
         */
        protected void inject(Object target, String requestingBeanName) {
            // 将 field 设置为可以访问，避免 private 的问题

            // 解析想要对 field 设置的目标值

            // 赋值

        }

        /**
         * 解析将要注入的目标值
         *
         * @param field 目标属性
         * @param bean
         * @param beanName
         * @return {@link Object }
         */
        protected Object resolveFieldValue(Field field, Object bean, String beanName) {
            // 1.创建依赖描述符，包装字段信息

            // 2/设置包含类，提供类型上下文

            // 3.搞一个 Spring 的类型转换器

            /*
            * 4.用于检测循环依赖的 Set
            * Spring会把解析过程中用到的Bean名称放进去，
            * 解析完成后，autowiredBeanNames 就包含了所有相关的Bean名称
            * */


            // 5.使用 beanFactory 解析依赖

            return null;
        }
    }


    public static ValueMetadata forElements(Collection<ValueElement> elements, Class<?> clazz) {
        return elements.isEmpty() ?
                new ValueMetadata(clazz, Collections.emptyList()) :
                new ValueMetadata(clazz, elements);
    }
}
