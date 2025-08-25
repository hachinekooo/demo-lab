package com.github.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 区块链存证的注解
 *
 * @author wangwenpeng
 * @date 2025-08-25
 */
@Target(ElementType.METHOD) // 只能加在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解会被保留到运行时，可通过反射获取
public @interface BlockchainCertify {
    
    /**
     * 业务类型
     * 如 stockIn/stockOut/stockTransfer
     */
    String businessType();
    
    /**
     * 描述
     */
    String description() default "";
}