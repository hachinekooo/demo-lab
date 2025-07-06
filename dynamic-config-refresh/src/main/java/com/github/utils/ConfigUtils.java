package com.github.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.mapper.GlobalConfMapper;
import com.github.mapper.model.GlobalConfDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ConfigUtils {
    public static final String DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME = "dynamic-config";

    // 从数据库加载配置的公共方法
    public static Map<String, Object> loadConfigFromDb(GlobalConfMapper mapper, String targetGroup) {
        log.info("Loading configuration from database for group: {}", targetGroup);

        List<GlobalConfDO> list = targetGroup == null ?
            mapper.selectList(null) :
            mapper.selectList(new LambdaQueryWrapper<GlobalConfDO>().eq(GlobalConfDO::getConfGroup, targetGroup));

        Map<String, Object> newConfigs = new HashMap<>();
        for (GlobalConfDO conf : list) {
            newConfigs.put(conf.getConfKey(), conf.getConfValue());
        }

        return newConfigs;
    }

    // 添加属性源的公共方法
    public static void addPropertySource(ConfigurableEnvironment environment, Map<String, Object> configs) {
        log.info("Adding property source with name: {}", DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME);

        MapPropertySource propertySource = new MapPropertySource(
            DYNAMIC_CONFIG_PROPERTY_SOURCE_NAME,
            configs
        );
        environment.getPropertySources().addFirst(propertySource);
    }

    public static boolean hasConfigChanged(Map<String, Object> oldConfigs,
                                           Map<String, Object> newConfigs) {
        return !Objects.equals(oldConfigs, newConfigs);
    }


    /**
     * 更新配置缓存的配置信息
     * @param targetGroup 目标分组
     * @param newConfigs 新配置
     * @param configCache 配置缓存引用
     * @return 是否更新成功
     */
    public static boolean updateConfigs(String targetGroup,
                                      Map<String, Object> newConfigs,
                                      Map<String, Object> configCache) {
        if (targetGroup == null && !newConfigs.isEmpty()) {
            configCache.clear();
            configCache.putAll(newConfigs);
            log.info("Successfully loaded {} configuration properties:", configCache.size());
            return true;
        }

        if (targetGroup != null && !newConfigs.isEmpty()) {
            configCache.putAll(newConfigs);
            log.info("Successfully added configuration properties for group: {}", targetGroup);
            return true;
        }

        log.warn("No configuration found in database");
        return false;
    }
}
