package com.github.controller;

import com.github.config.TestConfigProperties;
import com.github.service.DynamicConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/refresh")
    public String refreshConfig(@RequestParam String group) {
        dynamicConfigManager.reloadConfig(group);
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

    // 新增的配置管理接口
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/add")
    public String addConfig(@RequestParam String key, 
                           @RequestParam String value, 
                           @RequestParam(required = false) String group,
                           @RequestParam(required = false) String comment) {
        String sql = "INSERT INTO global_conf (`key`, `value`, `group`, `comment`) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, key, value, group, comment);
        return "Configuration added successfully!";
    }

    @PutMapping("/update")
    public String updateConfig(@RequestParam String key, 
                              @RequestParam String value, 
                              @RequestParam(required = false) String group,
                              @RequestParam(required = false) String comment) {
        String sql = "UPDATE global_conf SET `value` = ?, `group` = ?, `comment` = ?, `version` = `version` + 1 WHERE `key` = ?";
        jdbcTemplate.update(sql, value, group, comment, key);
        return "Configuration updated successfully!";
    }

    @DeleteMapping("/delete")
    public String deleteConfig(@RequestParam String key) {
        String sql = "UPDATE global_conf SET deleted = 1 WHERE `key` = ?";
        jdbcTemplate.update(sql, key);
        return "Configuration marked as deleted successfully!";
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listConfigs(@RequestParam(required = false) String group) {
        String sql = group == null ? 
            "SELECT `key`, `value`, `group`, `comment`, `version` FROM global_conf WHERE deleted = 0" :
            "SELECT `key`, `value`, `group`, `comment`, `version` FROM global_conf WHERE deleted = 0 AND `group` = ?";
        return jdbcTemplate.queryForList(sql, group);
    }
}
