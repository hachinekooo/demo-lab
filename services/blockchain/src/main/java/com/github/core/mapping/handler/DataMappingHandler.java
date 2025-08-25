package com.github.core.mapping.handler;

import com.github.core.extractor.CertifyData;

/**
 * 数据映射处理器接口
 * 
 * @author wangwenpeng
 * @date 2025-08-25
 */
public interface DataMappingHandler {

    /**
     * 判断该业务类型是否需要映射
     * 
     * @param bizType 业务类型
     * @return true-需要，false-不需要
     */
    boolean needMapping(String bizType);

    /**
     * 执行映射处理
     * 
     * @param data 存证数据
     */
    void handleMapping(CertifyData data);

    /**
     * 获取处理器优先级
     * 
     * @return 优先级，数值越小优先级越高
     */
    default int getPriority() {
        return 0;
    }

}