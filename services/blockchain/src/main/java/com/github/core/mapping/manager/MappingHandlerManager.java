package com.github.core.mapping.manager;


import com.github.core.extractor.CertifyData;
import com.github.core.mapping.handler.DataMappingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 映射处理器管理器
 * 负责管理所有的映射处理器
 *
 * @author wangwenpeng
 * @date 2025-08-25
 */
@Slf4j
@Component
public class MappingHandlerManager {
    @Resource
    private List<DataMappingHandler> mappingHandlers;


    /**
     * 处理所有映射
     *
     * @param data 存证数据
     */
    void processAllMappings(CertifyData data) {

    }
}