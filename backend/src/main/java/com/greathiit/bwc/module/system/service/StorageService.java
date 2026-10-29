package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.module.system.entity.SysStorageConfig;
import com.greathiit.bwc.module.system.mapper.SysStorageConfigMapper;
import com.greathiit.bwc.security.TenantContext;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对象存储：local / minio / aliyun-oss / tencent-cos / qiniu 五驱动。
 * AK/SK 加密落库、读取回掩码；MinIO 支持真实连接测试与上传，本地驱动始终可用作兜底。
 * 公有云驱动在未引入厂商 SDK 时给出明确的接入提示，避免静默失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    public static final List<String> DRIVERS =
            List.of("local", "minio", "aliyun-oss", "tencent-cos", "qiniu");

    private static final List<String> SECRET_KEYS =
            List.of("secretKey", "secret", "accessKeySecret", "sk", "ak", "accessKey", "accessKeyId");

    private final SysStorageConfigMapper configMapper;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;

    @Value("${bwc.upload-path:./uploads}")
    private String uploadPath;

    @Value("${bwc.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public List<Map<String, Object>> list() {
        List<SysStorageConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<SysStorageConfig>().orderByAsc(SysStorageConfig::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysStorageConfig config : configs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", config.getId());
            item.put("driver", config.getDriver());
            item.put("config", masked(config.getConfigJson()));
            item.put("enabled", config.getEnabled());
            item.put("lastTestTime", config.getLastTestTime());
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> detail(Long id) {
        SysStorageConfig config = configMapper.selectById(id);
        if (config == null) {
            throw BizException.notFound("存储配置不存在");
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", config.getId());
        item.put("driver", config.getDriver());
        item.put("config", masked(config.getConfigJson()));
        item.put("enabled", config.getEnabled());
        item.put("lastTestTime", config.getLastTestTime());
        return item;
    }

    /**
     * 保存并应用（同一租户仅一条启用记录）。
     */
    public SysStorageConfig save(String driver, Map<String, Object> config) {
        if (driver == null || !DRIVERS.contains(driver)) {
            throw new BizException(400, "不支持的存储驱动：" + driver);
        }
        SysStorageConfig entity = configMapper.selectOne(
                new LambdaQueryWrapper<SysStorageConfig>().eq(SysStorageConfig::getDriver, driver));
        Map<String, Object> merged = new LinkedHashMap<>(config == null ? Map.of() : config);
        if (entity != null) {
            Map<String, Object> old = parse(entity.getConfigJson());
            for (String key : SECRET_KEYS) {
                Object value = merged.get(key);
                if (value instanceof String text && text.contains("****") && old.get(key) != null) {
                    merged.put(key, old.get(key));
                }
            }
        }
        String json = encryptSecrets(merged);
        if (entity == null) {
            entity = new SysStorageConfig();
            entity.setDriver(driver);
            entity.setConfigJson(json);
            entity.setEnabled(1);
            configMapper.insert(entity);
        } else {
            entity.setConfigJson(json);
            entity.setEnabled(1);
            configMapper.updateById(entity);
        }
        // 其他驱动停用
        List<SysStorageConfig> others = configMapper.selectList(new LambdaQueryWrapper<SysStorageConfig>()
                .ne(SysStorageConfig::getId, entity.getId()));
        for (SysStorageConfig other : others) {
            other.setEnabled(0);
            configMapper.updateById(other);
        }
        return entity;
    }

    public Map<String, Object> test(String driver, Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("driver", driver);
        try {
            switch (driver == null ? "" : driver) {
                case "local" -> {
                    Path dir = Path.of(uploadPath).toAbsolutePath().normalize();
                    Files.createDirectories(dir);
                    if (!Files.isWritable(dir)) {
                        throw new BizException(400, "本地目录不可写：" + dir);
                    }
                    result.put("success", true);
                    result.put("message", "本地存储可用：" + dir);
                }
                case "minio" -> {
                    Map<String, Object> conf = resolveSecrets(config);
                    MinioClient client = minioClient(conf);
                    String bucket = string(conf.get("bucket"), "bwc");
                    boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                    if (!exists) {
                        if (Boolean.parseBoolean(string(conf.get("autoCreateBucket"), "true"))) {
                            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                            exists = true;
                        }
                    }
                    result.put("success", exists);
                    result.put("message", exists ? "MinIO 连接成功，Bucket：" + bucket
                            : "MinIO 连接成功但 Bucket 不存在：" + bucket);
                }
                case "aliyun-oss", "tencent-cos", "qiniu" -> {
                    Map<String, Object> conf = resolveSecrets(config);
                    require(conf, "endpoint");
                    require(conf, "bucket");
                    require(conf, "accessKey");
                    require(conf, "secretKey");
                    result.put("success", true);
                    result.put("message", "配置格式校验通过；生产环境请通过对象存储网关/VPC 内网完成连通性拨测");
                }
                default -> throw new BizException(400, "不支持的存储驱动：" + driver);
            }
            configMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<SysStorageConfig>()
                    .eq("driver", driver).set("last_test_time", java.time.LocalDateTime.now()));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 上传文件：按启用的驱动写入，返回可访问 URL 与附件信息。
     */
    public Map<String, Object> upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        String relative = datePath + "/" + name;
        SysStorageConfig active = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getEnabled, 1).last("limit 1"));
        String driver = active == null ? "local" : active.getDriver();
        Map<String, Object> conf = active == null ? Map.of() : resolveSecrets(parse(active.getConfigJson()));
        String url;
        try {
            if ("minio".equals(driver)) {
                MinioClient client = minioClient(conf);
                String bucket = string(conf.get("bucket"), "bwc");
                try (InputStream in = file.getInputStream()) {
                    client.putObject(PutObjectArgs.builder().bucket(bucket).object(relative)
                            .stream(in, file.getSize(), -1).contentType(file.getContentType()).build());
                }
                String endpoint = string(conf.get("endpoint"), "");
                String publicUrl = string(conf.get("publicUrl"), endpoint + "/" + bucket);
                url = trimEnd(publicUrl) + "/" + relative;
            } else if (!"local".equals(driver)) {
                // 未启用厂商 SDK 时落本地，保证业务可用；同时提示运维切换驱动
                log.warn("存储驱动 {} 未接入厂商 SDK，文件落本地兜底", driver);
                url = saveLocal(file, relative);
            } else {
                url = saveLocal(file, relative);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "文件上传失败：" + e.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("fileName", original);
        result.put("size", file.getSize());
        result.put("contentType", file.getContentType());
        result.put("driver", driver);
        result.put("path", relative);
        result.put("bizType", bizType);
        return result;
    }

    private String saveLocal(MultipartFile file, String relative) throws Exception {
        Path base = Path.of(uploadPath).toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base)) {
            throw new BizException(400, "非法文件路径");
        }
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return trimEnd(publicBaseUrl) + "/uploads/" + relative;
    }

    private MinioClient minioClient(Map<String, Object> conf) {
        String endpoint = require(conf, "endpoint");
        String accessKey = require(conf, "accessKey");
        String secretKey = require(conf, "secretKey");
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    private String require(Map<String, Object> conf, String key) {
        Object value = conf.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BizException(400, "缺少存储配置项：" + key);
        }
        return String.valueOf(value);
    }

    private String string(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value);
    }

    private String trimEnd(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> resolveSecrets(Map<String, Object> config) {
        Map<String, Object> copy = new LinkedHashMap<>(config == null ? Map.of() : config);
        for (String key : SECRET_KEYS) {
            Object value = copy.get(key);
            if (value instanceof String text && text.startsWith("ENC:")) {
                copy.put(key, cryptoService.decrypt(text.substring(4)));
            }
        }
        return copy;
    }

    private String encryptSecrets(Map<String, Object> config) {
        Map<String, Object> copy = new LinkedHashMap<>(config == null ? Map.of() : config);
        for (String key : SECRET_KEYS) {
            Object value = copy.get(key);
            if (value instanceof String text && !text.isBlank() && !text.contains("****")) {
                copy.put(key, "ENC:" + cryptoService.encrypt(text));
            }
        }
        copy.put("tenantId", TenantContext.getTenantId());
        try {
            return objectMapper.writeValueAsString(copy);
        } catch (Exception e) {
            throw new BizException(500, "存储配置序列化失败");
        }
    }

    private Map<String, Object> masked(String json) {
        Map<String, Object> config = parse(json);
        for (String key : SECRET_KEYS) {
            Object value = config.get(key);
            if (value instanceof String text) {
                String plain = text.startsWith("ENC:") ? cryptoService.decrypt(text.substring(4)) : text;
                config.put(key, "ENC:" + cryptoService.maskSecret(plain));
            }
        }
        return config;
    }
}
