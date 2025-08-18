package com.github.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 日志注解
 *
 * @author wangwenpeng
 * @date 2025/05/24
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAccessLog {

    /**
     * 是否记录访问日志
     */
    boolean enable() default true;

    /*
    * 描述信息
    * */
    String description() default "";
}
