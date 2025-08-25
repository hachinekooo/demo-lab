package com.github.core.extractor;

/**
 * 数据提取器接口
 * 想要存证的业务，需要对此接口进行实现，以构建该业务的数据
 * 
 * @author wangwenpeng
 * @since 2025-08-25
 */
public interface DataExtractor {
    
    /**
     * 从方法参数中获取参数，构建存证数据
     * 
     * @param args 方法参数数组
     * @return 构建好的存证数据
     */
    CertifyData extract(Object[] args);

}