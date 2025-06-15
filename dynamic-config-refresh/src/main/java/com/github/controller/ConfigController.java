package com.github.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.config.TestConfigProperties;
import com.github.controller.dto.GlobalConfAddDTO;
import com.github.controller.dto.GlobalConfUpdateDTO;
import com.github.controller.vo.GlobalConfVO;
import com.github.dynamic.DynamicConfigManager;
import com.github.mapper.GlobalConfMapper;
import com.github.mapper.model.GlobalConfDO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private TestConfigProperties testConfigProperties;
    @Autowired
    private DynamicConfigManager dynamicConfigManager;
    @Autowired
    private GlobalConfMapper globalConfMapper;

    @GetMapping("/refresh")
    public String refreshConfig(@RequestParam String group) {
        dynamicConfigManager.reloadConfig(group);
        return "Configuration refreshed!";
    }

    @GetMapping("/current")
    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("name", testConfigProperties.getName());
        configMap.put("version", testConfigProperties.getVersion());
        configMap.put("enabled", testConfigProperties.isEnabled());
        return configMap;
    }

    // 新增的配置管理接口
    @PostMapping("/add")
    public String addConfig(@RequestBody @Valid GlobalConfAddDTO dto) {
        GlobalConfDO conf = new GlobalConfDO();
        BeanUtils.copyProperties(dto, conf);
        conf.setDeleted(0);
        globalConfMapper.insert(conf);
        dynamicConfigManager.reloadConfig(conf.getConfGroup());
        return "Added";
    }

    @PutMapping("/update")
    public String updateConfig(@RequestBody @Valid GlobalConfUpdateDTO dto) {
        LambdaQueryWrapper<GlobalConfDO> wrapper =
                new LambdaQueryWrapper<GlobalConfDO>().eq(GlobalConfDO::getConfKey, dto.getConfKey());
        GlobalConfDO conf = globalConfMapper.selectOne(wrapper);
        if (conf != null) {
            conf.setConfValue(dto.getConfValue());
            globalConfMapper.updateById(conf);

            String confGroup = conf.getConfGroup();
            dynamicConfigManager.reloadConfig(confGroup);
        }
        return "Configuration updated successfully!";
    }

    @GetMapping("/list")
    public List<GlobalConfVO> listConfigs(@RequestParam(required = false) String group) {
        if (group == null) {
            return globalConfMapper.selectAllValid();
        } else {
            return globalConfMapper.selectByGroup(group);
        }
    }

    @GetMapping("/register-callback")
    public String registerCallbackTest() {
        dynamicConfigManager.registerRefreshCallback(testConfigProperties, new Runnable() {
            @Override
            public void run() {
                System.out.println("The callback mission of configuration class 'testConfigProperties' has been successfully invoked. ");
            }
        });
        return "register callback successfully!";
    }
}
