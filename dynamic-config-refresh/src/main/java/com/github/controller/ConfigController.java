package com.github.controller;

import com.github.DynamicConfigManager;
import com.github.config.TestConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private TestConfigProperties testConfigProperties;
    @Autowired
    private DynamicConfigManager dynamicConfigManager;

    @GetMapping("/refresh")
    public String refreshConfig() {
        dynamicConfigManager.reloadConfig();
        return "Configuration refreshed!";
    }

    @GetMapping("/current")
    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("name", testConfigProperties.getName());
        configMap.put("age", testConfigProperties.getVersion());
        configMap.put("enabled", testConfigProperties.isEnabled());
        return configMap;
    }
}
