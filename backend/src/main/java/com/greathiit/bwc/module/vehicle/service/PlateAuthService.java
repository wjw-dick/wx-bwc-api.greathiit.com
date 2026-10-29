package com.greathiit.bwc.module.vehicle.service;

import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateAuthRecord;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateCamera;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateConfig;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateAuthRecordMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateCameraMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateConfigMapper;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车牌识别接口（plate_auth_api）对接：BASE/API Key 租户级配置、健康检查、相机、车牌下发/删除/查询、日志同步。
 * 所有远程写操作落授权台账并写审计日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlateAuthService {

    private final BwcPlateConfigMapper configMapper;
    private final BwcPlateCameraMapper cameraMapper;
    private final BwcPlateAuthRecordMapper authRecordMapper;
    private final CryptoService cryptoService;

    private final RestTemplate rest = restTemplate();

    private static RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    public BwcPlateConfig config() {
        return configMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BwcPlateConfig>());
    }

    public Map<String, Object> maskedConfig() {
        BwcPlateConfig config = config();
        Map<String, Object> result = new HashMap<>();
        if (config == null) {
            result.put("baseUrl", "");
            result.put("apiKeyMask", "");
            result.put("status", "DISABLED");
            result.put("mqttConnected", false);
            return result;
        }
        result.put("id", config.getId());
        result.put("baseUrl", config.getBaseUrl());
        result.put("apiKeyMask", config.getApiKeyMask());
        result.put("status", config.getStatus());
        result.put("mqttConnected", config.getMqttConnected() == 1);
        result.put("lastTestTime", config.getLastTestTime());
        return result;
    }

    /**
     * 保存配置：密钥 AES 加密，页面回掩码；未通过健康检查时状态置 DISABLED。
     */
    public Map<String, Object> saveConfig(String baseUrl, String apiKey, boolean tested, boolean mqttConnected) {
        BwcPlateConfig config = config();
        if (config == null) {
            config = new BwcPlateConfig();
            config.setBaseUrl(baseUrl);
            if (apiKey != null && !apiKey.isBlank()) {
                config.setApiKeyEnc(cryptoService.encrypt(apiKey));
                config.setApiKeyMask(cryptoService.maskSecret(apiKey));
            }
            config.setStatus(tested && mqttConnected ? "ENABLED" : "DISABLED");
            config.setMqttConnected(mqttConnected ? 1 : 0);
            config.setLastTestTime(tested ? LocalDateTime.now() : null);
            configMapper.insert(config);
        } else {
            config.setBaseUrl(baseUrl);
            if (apiKey != null && !apiKey.isBlank()) {
                config.setApiKeyEnc(cryptoService.encrypt(apiKey));
                config.setApiKeyMask(cryptoService.maskSecret(apiKey));
            }
            config.setStatus(tested && mqttConnected ? "ENABLED" : "DISABLED");
            config.setMqttConnected(mqttConnected ? 1 : 0);
            if (tested) {
                config.setLastTestTime(LocalDateTime.now());
            }
            configMapper.updateById(config);
        }
        return maskedConfig();
    }

    public Map<String, Object> test(String baseUrl, String apiKey) {
        String url = (baseUrl == null || baseUrl.isBlank()) ? storedBaseUrl() : baseUrl;
        String key = (apiKey == null || apiKey.isBlank()) ? storedApiKey() : apiKey;
        Map<String, Object> result = new HashMap<>();
        if (url == null || url.isBlank()) {
            throw new BizException(400, "请先填写 BASE 地址");
        }
        try {
            Map<?, ?> response = call(url, key, HttpMethod.GET, "/api/health", null);
            boolean mqtt = response != null && Boolean.parseBoolean(String.valueOf(response.get("mqtt_connected")));
            result.put("success", true);
            result.put("mqttConnected", mqtt);
            result.put("raw", response);
            BwcPlateConfig config = config();
            if (config != null) {
                config.setMqttConnected(mqtt ? 1 : 0);
                config.setLastTestTime(LocalDateTime.now());
                if (!mqtt) {
                    config.setStatus("DISABLED");
                }
                configMapper.updateById(config);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("mqttConnected", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private String storedBaseUrl() {
        BwcPlateConfig config = config();
        return config == null ? null : config.getBaseUrl();
    }

    private String storedApiKey() {
        BwcPlateConfig config = config();
        return config == null ? null : cryptoService.decrypt(config.getApiKeyEnc());
    }

    private Map<?, ?> call(String baseUrl, String apiKey, HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null) {
            headers.set("X-API-Key", apiKey);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = rest.exchange(baseUrl + path, method, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("车牌识别接口调用失败 {} {}: {}", method, path, e.getMessage());
            throw new BizException(502, "车牌识别接口调用失败：" + e.getMessage());
        }
    }

    private Map<?, ?> callStored(HttpMethod method, String path, Object body) {
        String baseUrl = storedBaseUrl();
        String key = storedApiKey();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException(400, "车牌识别接口未配置，请先在后台配置 BASE 地址与 API 密钥");
        }
        return call(baseUrl, key, method, path, body);
    }

    // ------------------------- 相机管理 -------------------------

    public List<BwcPlateCamera> localCameras() {
        return cameraMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BwcPlateCamera>()
                .orderByAsc(BwcPlateCamera::getId));
    }

    public List<BwcPlateCamera> syncCameras() {
        Map<?, ?> response = callStored(HttpMethod.GET, "/api/cameras", null);
        List<Map<String, Object>> remote = extractList(response, "cameras");
        List<BwcPlateCamera> local = localCameras();
        for (Map<String, Object> item : remote) {
            String camId = str(item.get("cam_id"), str(item.get("camId"), null));
            if (camId == null) {
                continue;
            }
            BwcPlateCamera camera = local.stream()
                    .filter(c -> camId.equals(c.getCamId())).findFirst().orElse(null);
            boolean isNew = camera == null;
            if (isNew) {
                camera = new BwcPlateCamera();
                camera.setCamId(camId);
                camera.setTenantId(TenantContext.getTenantId());
            }
            camera.setName(str(item.get("name"), camId));
            camera.setCampus(str(item.get("campus"), camera.getCampus()));
            camera.setCamType(str(item.get("cam_type"), camera.getCamType()));
            camera.setRemark(str(item.get("remark"), camera.getRemark()));
            camera.setEnabled(parseBool(item.get("enabled")) ? 1 : 0);
            camera.setSyncStatus("SYNCED");
            camera.setSyncTime(LocalDateTime.now());
            if (isNew) {
                cameraMapper.insert(camera);
            } else {
                cameraMapper.updateById(camera);
            }
        }
        return localCameras();
    }

    public BwcPlateCamera saveCamera(BwcPlateCamera camera) {
        if (camera.getId() == null) {
            cameraMapper.insert(camera);
        } else {
            cameraMapper.updateById(camera);
        }
        return camera;
    }

    public void deleteCamera(Long id) {
        BwcPlateCamera camera = cameraMapper.selectById(id);
        if (camera == null) {
            throw BizException.notFound("相机不存在");
        }
        cameraMapper.deleteById(id);
    }

    // ------------------------- 车牌下发 -------------------------

    public Map<String, Object> addAll(String plateNo, String operator, String action) {
        Map<String, Object> body = new HashMap<>();
        body.put("plate_num", plateNo);
        Map<?, ?> response = callStored(HttpMethod.POST, "/api/auth/add-all", body);
        String batchNo = "BATCH-" + System.currentTimeMillis();
        return writeRecords(plateNo, "FIXED", action, batchNo, response, operator, null, null);
    }

    public Map<String, Object> add(String plateNo, String cameraId, String operator) {
        Map<String, Object> body = new HashMap<>();
        body.put("plate_num", plateNo);
        if (cameraId != null) {
            body.put("camera_id", cameraId);
        }
        Map<?, ?> response = callStored(HttpMethod.POST, "/api/auth/add", body);
        String batchNo = "BATCH-" + System.currentTimeMillis();
        return writeRecords(plateNo, "FIXED", "ADD", batchNo, response, operator, null, null);
    }

    public Map<String, Object> addTemp(String plateNo, LocalDateTime start, LocalDateTime end, String operator) {
        Map<String, Object> body = new HashMap<>();
        body.put("plate_num", plateNo);
        body.put("start_time", start == null ? null : start.toString());
        body.put("end_time", end == null ? null : end.toString());
        Map<?, ?> response = callStored(HttpMethod.POST, "/api/auth/temp", body);
        String batchNo = "BATCH-" + System.currentTimeMillis();
        return writeRecords(plateNo, "TEMP", "TEMP", batchNo, response, operator, start, end);
    }

    public Map<String, Object> delete(String plateNo, String cameraId, String operator) {
        Map<String, Object> body = new HashMap<>();
        body.put("plate_num", plateNo);
        if (cameraId != null) {
            body.put("camera_id", cameraId);
        }
        Map<?, ?> response = callStored(HttpMethod.POST, "/api/auth/del", body);
        String batchNo = "BATCH-" + System.currentTimeMillis();
        return writeRecords(plateNo, "FIXED", "DEL", batchNo, response, operator, null, null);
    }

    public Map<String, Object> deleteAll(String plateNo, String operator) {
        Map<String, Object> body = new HashMap<>();
        body.put("plate_num", plateNo);
        Map<?, ?> response = callStored(HttpMethod.POST, "/api/auth/del-all", body);
        String batchNo = "BATCH-" + System.currentTimeMillis();
        return writeRecords(plateNo, "FIXED", "DEL_ALL", batchNo, response, operator, null, null);
    }

    public Map<?, ?> query(String plateNo) {
        return callStored(HttpMethod.GET, "/api/auth/query?plate_num=" + plateNo, null);
    }

    public Map<?, ?> records() {
        return callStored(HttpMethod.GET, "/api/auth/records", null);
    }

    public Map<?, ?> logs(String plateNo, String cameraId, String inOut, String start, String end) {
        StringBuilder path = new StringBuilder("/api/logs?");
        if (plateNo != null) {
            path.append("plate_num=").append(plateNo).append("&");
        }
        if (cameraId != null) {
            path.append("camera_id=").append(cameraId).append("&");
        }
        if (inOut != null) {
            path.append("in_out=").append(inOut).append("&");
        }
        if (start != null) {
            path.append("start_time=").append(start).append("&");
        }
        if (end != null) {
            path.append("end_time=").append(end).append("&");
        }
        return callStored(HttpMethod.GET, path.toString(), null);
    }

    public Map<?, ?> inPark() {
        return callStored(HttpMethod.GET, "/api/logs/in-park", null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> writeRecords(String plateNo, String plateType, String action, String batchNo,
                                             Map<?, ?> response, String operator,
                                             LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new HashMap<>();
        int total = 0;
        int success = 0;
        int failed = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        if (response == null) {
            throw new BizException(502, "车牌识别接口无响应");
        }
        Object resultsObj = response.get("results");
        List<Map<String, Object>> results = resultsObj instanceof List<?> list
                ? (List<Map<String, Object>>) list : Collections.emptyList();
        if (results.isEmpty()) {
            String cameraId = str(response.get("camera_id"), null);
            Map<String, Object> single = new HashMap<>();
            single.put("camera_id", cameraId);
            single.put("name", str(response.get("name"), cameraId));
            single.put("status", str(response.get("status"), "pending"));
            single.put("msg", str(response.get("msg"), null));
            results = List.of(single);
        }
        for (Map<String, Object> item : results) {
            total++;
            String status = str(item.get("status"), "pending");
            boolean ok = status == null || "active".equalsIgnoreCase(status)
                    || "ok".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status);
            if (ok) {
                success++;
            } else {
                failed++;
            }
            BwcPlateAuthRecord record = new BwcPlateAuthRecord();
            record.setTenantId(TenantContext.getTenantId());
            record.setPlateNo(plateNo);
            record.setPlateType(plateType);
            record.setCameraId(str(item.get("camera_id"), null));
            record.setCameraName(str(item.get("name"), null));
            record.setAction(action);
            record.setStatus(status == null ? "pending" : status);
            record.setMessage(str(item.get("msg"), null));
            record.setStartTime(start);
            record.setEndTime(end);
            record.setBatchNo(batchNo);
            record.setOperator(operator);
            authRecordMapper.insert(record);
            details.add(Map.of("cameraId", String.valueOf(record.getCameraId()),
                    "cameraName", String.valueOf(record.getCameraName()),
                    "status", record.getStatus(), "msg", String.valueOf(record.getMessage())));
        }
        Object totalObj = response.get("total");
        if (totalObj != null) {
            total = Integer.parseInt(String.valueOf(totalObj));
        }
        Object successObj = response.get("success");
        if (successObj != null) {
            success = Integer.parseInt(String.valueOf(successObj));
        }
        Object failedObj = response.get("failed");
        if (failedObj != null) {
            failed = Integer.parseInt(String.valueOf(failedObj));
        }
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("details", details);
        result.put("batchNo", batchNo);
        return result;
    }

    private List<Map<String, Object>> extractList(Map<?, ?> response, String key) {
        Object data = response == null ? null : response.get(key);
        if (data == null && response != null) {
            data = response.get("data");
        }
        if (data instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> row = new HashMap<>();
                    map.forEach((k, v) -> row.put(String.valueOf(k), v));
                    result.add(row);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private String str(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? defaultValue : s;
    }

    private boolean parseBool(Object value) {
        if (value == null) {
            return true;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
