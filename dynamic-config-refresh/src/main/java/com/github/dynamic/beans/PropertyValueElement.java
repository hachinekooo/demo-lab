package com.github.dynamic.beans;

import java.lang.reflect.Field;

public class PropertyValueElement extends ValueMetadata.ValueElement {

    protected PropertyValueElement(Field field) {
        super(field);
    }

    @Override
    protected void inject(Object target, String requestingBeanName) {
        // 将 field 设置为可以访问，避免 private 的问题

        // 解析想要对 field 设置的目标值

        // 赋值

    }

    @Override
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
