package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.module.system.entity.SysPlatformParam;
import com.greathiit.bwc.module.system.mapper.SysPlatformParamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 平台参数读取（缓存，保存后主动刷新）。
 */
@Service
@RequiredArgsConstructor
public class ParamService {

    private final SysPlatformParamMapper paramMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String key, String defaultValue) {
        String value = cache.get(key);
        if (value != null) {
            return value;
        }
        SysPlatformParam param = paramMapper.selectOne(
                new LambdaQueryWrapper<SysPlatformParam>().eq(SysPlatformParam::getParamKey, key));
        if (param == null || param.getParamValue() == null) {
            return defaultValue;
        }
        cache.put(key, param.getParamValue());
        return param.getParamValue();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public void refresh() {
        cache.clear();
    }
}
