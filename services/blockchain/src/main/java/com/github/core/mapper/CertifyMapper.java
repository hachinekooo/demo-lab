package com.github.core.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.core.dataobject.entity.CertifyRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区块链存证记录Mapper接口
 * 
 * @author wangwenpeng
 * @date 2025-08-25
 */
@Mapper
public interface CertifyMapper extends BaseMapper<CertifyRecordDO> {
    // 基础的增删改查方法由BaseMapper提供
}