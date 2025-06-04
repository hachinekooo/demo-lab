package com.github.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.controller.vo.GlobalConfVO;
import com.github.converter.GlobalConfConverter;
import com.github.model.GlobalConfDO;

import java.util.List;

public interface GlobalConfMapper extends BaseMapper<GlobalConfDO> {
    default List<GlobalConfVO> selectByGroup(String group) {
        List<GlobalConfDO> globalConfDOS = selectList(
                new LambdaQueryWrapper<GlobalConfDO>().eq(GlobalConfDO::getConfGroup, group)
        );
        return GlobalConfConverter.convertToVOList(globalConfDOS);
    }

    default List<GlobalConfVO> selectAllValid() {
        List<GlobalConfDO> globalConfDOS = selectList(
                new LambdaQueryWrapper<GlobalConfDO>().eq(GlobalConfDO::getDeleted, 0)
        );
        return GlobalConfConverter.convertToVOList(globalConfDOS);
    }
}