package com.github.converter;

import com.github.controller.vo.GlobalConfVO;
import com.github.model.GlobalConfDO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

// 转换工具类
public class GlobalConfConverter {
    public static List<GlobalConfVO> convertToVOList(List<GlobalConfDO> globalConfDOS) {
        return globalConfDOS.stream()
                .map(GlobalConfConverter::convertToVO)
                .collect(Collectors.toList());
    }
    
    public static GlobalConfVO convertToVO(GlobalConfDO globalConfDO) {
        GlobalConfVO globalConfVO = new GlobalConfVO();
        BeanUtils.copyProperties(globalConfDO, globalConfVO);
        return globalConfVO;
    }
}