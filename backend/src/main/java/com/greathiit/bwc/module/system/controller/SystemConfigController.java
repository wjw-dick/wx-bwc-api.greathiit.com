package com.greathiit.bwc.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.common.ExcelUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.entity.SysAttachment;
import com.greathiit.bwc.module.system.entity.SysAuditLog;
import com.greathiit.bwc.module.system.entity.SysGlobalDict;
import com.greathiit.bwc.module.system.entity.SysLanguage;
import com.greathiit.bwc.module.system.entity.SysMenu;
import com.greathiit.bwc.module.system.entity.SysMessage;
import com.greathiit.bwc.module.system.entity.SysMessageTemplate;
import com.greathiit.bwc.module.system.entity.SysPlatformParam;
import com.greathiit.bwc.module.system.entity.SysStorageConfig;
import com.greathiit.bwc.module.system.entity.SysTenantConfig;
import com.greathiit.bwc.module.system.entity.SysTenantDict;
import com.greathiit.bwc.module.system.entity.SysWechatConfig;
import com.greathiit.bwc.module.system.mapper.SysAttachmentMapper;
import com.greathiit.bwc.module.system.mapper.SysAuditLogMapper;
import com.greathiit.bwc.module.system.mapper.SysGlobalDictMapper;
import com.greathiit.bwc.module.system.mapper.SysLanguageMapper;
import com.greathiit.bwc.module.system.mapper.SysMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysMessageMapper;
import com.greathiit.bwc.module.system.mapper.SysMessageTemplateMapper;
import com.greathiit.bwc.module.system.mapper.SysPlatformParamMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantConfigMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantDictMapper;
import com.greathiit.bwc.module.system.mapper.SysWechatConfigMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.system.service.ParamService;
import com.greathiit.bwc.module.system.service.StorageService;
import com.greathiit.bwc.module.system.service.TenantService;
import com.greathiit.bwc.module.system.service.WeChatService;
import com.greathiit.bwc.security.AccessGuard;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.RequirePermission;
import com.greathiit.bwc.security.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置：平台参数、租户配置、字典、多语言、菜单可视化、微信配置、消息模板、
 * 站内消息、审计日志、对象存储与上传。
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SysPlatformParamMapper paramMapper;
    private final SysTenantConfigMapper tenantConfigMapper;
    private final SysGlobalDictMapper globalDictMapper;
    private final SysTenantDictMapper tenantDictMapper;
    private final SysLanguageMapper languageMapper;
    private final SysMenuMapper menuMapper;
    private final SysWechatConfigMapper wechatConfigMapper;
    private final SysMessageTemplateMapper messageTemplateMapper;
    private final SysMessageMapper messageMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final SysAttachmentMapper attachmentMapper;
    private final StorageService storageService;
    private final WeChatService weChatService;
    private final NotifyService notifyService;
    private final ParamService paramService;
    private final TenantService tenantService;
    private final CryptoService cryptoService;
    private final PermissionService permissionService;
    private final AccessGuard accessGuard;
    private final AuditService auditService;

    // ------------------------------------------------------------------ 平台参数
    @GetMapping("/params")
    @RequirePermission("param:read")
    public R<List<SysPlatformParam>> params() {
        return R.ok(paramMapper.selectList(new LambdaQueryWrapper<SysPlatformParam>()
                .orderByAsc(SysPlatformParam::getId)));
    }

    @PutMapping("/params/{key}")
    @RequirePermission("param:write")
    public R<SysPlatformParam> updateParam(@PathVariable String key, @RequestBody Map<String, String> body) {
        permissionService.requireSystem();
        SysPlatformParam param = paramMapper.selectOne(
                new LambdaQueryWrapper<SysPlatformParam>().eq(SysPlatformParam::getParamKey, key));
        if (param == null) {
            throw BizException.notFound("参数不存在：" + key);
        }
        param.setParamValue(body.get("paramValue"));
        paramMapper.updateById(param);
        paramService.refresh();
        auditService.record("param", "UPDATE", key, "平台参数：" + param.getParamValue());
        return R.ok(param);
    }

    // ------------------------------------------------------------------ 租户配置
    @GetMapping("/tenant-config")
    @RequirePermission("system:read")
    public R<Map<String, String>> tenantConfig() {
        Map<String, String> raw = tenantService.tenantConfig(TenantContext.getTenantId());
        Map<String, String> result = new LinkedHashMap<>();
        raw.forEach((k, v) -> result.put(k, isSecretKey(k) && v != null ? cryptoService.maskSecret(v) : v));
        return R.ok(result);
    }

    @PutMapping("/tenant-config")
    @RequirePermission("system:write")
    public R<Map<String, String>> saveTenantConfig(@RequestBody Map<String, String> body) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId == 0) {
            throw new BizException(400, "请在租户下保存租户配置");
        }
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            SysTenantConfig config = tenantConfigMapper.selectOne(new LambdaQueryWrapper<SysTenantConfig>()
                    .eq(SysTenantConfig::getConfigKey, entry.getKey()));
            boolean secret = isSecretKey(entry.getKey());
            String value = entry.getValue();
            if (secret && value != null && value.contains("****")) {
                continue;
            }
            if (secret && value != null && !value.isBlank()) {
                value = cryptoService.encrypt(value);
            }
            if (config == null) {
                config = new SysTenantConfig();
                config.setTenantId(tenantId);
                config.setConfigKey(entry.getKey());
                config.setConfigValue(value);
                config.setEncrypted(secret ? 1 : 0);
                tenantConfigMapper.insert(config);
            } else {
                config.setConfigValue(value);
                config.setEncrypted(secret ? 1 : 0);
                tenantConfigMapper.updateById(config);
            }
        }
        auditService.record("system", "TENANT_CONFIG", "tenant", "保存租户配置");
        return tenantConfig();
    }

    private boolean isSecretKey(String key) {
        String lower = key == null ? "" : key.toLowerCase();
        return lower.contains("secret") || lower.contains("password") || lower.endsWith("_key")
                || lower.contains("private");
    }

    // ------------------------------------------------------------------ 字典
    @GetMapping("/dicts")
    @RequirePermission("dict:read")
    public R<Map<String, Object>> dicts(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<SysGlobalDict> globalWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysTenantDict> tenantWrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            globalWrapper.eq(SysGlobalDict::getDictType, type);
            tenantWrapper.eq(SysTenantDict::getDictType, type);
        }
        globalWrapper.orderByAsc(SysGlobalDict::getDictType).orderByAsc(SysGlobalDict::getSort);
        tenantWrapper.orderByAsc(SysTenantDict::getDictType).orderByAsc(SysTenantDict::getSort);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("global", globalDictMapper.selectList(globalWrapper));
        result.put("tenant", tenantDictMapper.selectList(tenantWrapper));
        return R.ok(result);
    }

    @PostMapping("/dicts/global")
    @RequirePermission("dict:write")
    public R<SysGlobalDict> createGlobalDict(@RequestBody SysGlobalDict dict) {
        permissionService.requireSystem();
        dict.setId(null);
        dict.setTenantId(0L);
        if (dict.getStatus() == null) {
            dict.setStatus("NORMAL");
        }
        globalDictMapper.insert(dict);
        auditService.record("dict", "GLOBAL_CREATE", dict.getDictType(), "新增全局字典：" + dict.getDictLabel());
        return R.ok(dict);
    }

    @PutMapping("/dicts/global/{id}")
    @RequirePermission("dict:write")
    public R<SysGlobalDict> updateGlobalDict(@PathVariable Long id, @RequestBody SysGlobalDict dict) {
        permissionService.requireSystem();
        dict.setId(id);
        globalDictMapper.updateById(dict);
        return R.ok(globalDictMapper.selectById(id));
    }

    @DeleteMapping("/dicts/global/{id}")
    @RequirePermission("dict:write")
    public R<Void> deleteGlobalDict(@PathVariable Long id) {
        permissionService.requireSystem();
        globalDictMapper.deleteById(id);
        return R.ok();
    }

    @PostMapping("/dicts/tenant")
    @RequirePermission("dict:write")
    public R<SysTenantDict> createTenantDict(@RequestBody SysTenantDict dict) {
        dict.setId(null);
        dict.setTenantId(TenantContext.getTenantId());
        if (dict.getStatus() == null) {
            dict.setStatus("NORMAL");
        }
        tenantDictMapper.insert(dict);
        auditService.record("dict", "TENANT_CREATE", dict.getDictType(), "新增租户字典：" + dict.getDictLabel());
        return R.ok(dict);
    }

    @PutMapping("/dicts/tenant/{id}")
    @RequirePermission("dict:write")
    public R<SysTenantDict> updateTenantDict(@PathVariable Long id, @RequestBody SysTenantDict dict) {
        dict.setId(id);
        tenantDictMapper.updateById(dict);
        return R.ok(tenantDictMapper.selectById(id));
    }

    @DeleteMapping("/dicts/tenant/{id}")
    @RequirePermission("dict:write")
    public R<Void> deleteTenantDict(@PathVariable Long id) {
        tenantDictMapper.deleteById(id);
        return R.ok();
    }

    // ------------------------------------------------------------------ 多语言
    @GetMapping("/languages")
    @RequirePermission("i18n:read")
    public R<List<SysLanguage>> languages() {
        return R.ok(languageMapper.selectList(new LambdaQueryWrapper<SysLanguage>()
                .orderByAsc(SysLanguage::getSort)));
    }

    @PostMapping("/languages")
    @RequirePermission("i18n:write")
    public R<SysLanguage> createLanguage(@RequestBody SysLanguage language) {
        permissionService.requireSystem();
        language.setId(null);
        language.setTenantId(0L);
        if (language.getEnabled() == null) {
            language.setEnabled(1);
        }
        if (language.getSort() == null) {
            language.setSort(99);
        }
        languageMapper.insert(language);
        auditService.record("i18n", "CREATE", language.getLangCode(), "新增语言");
        return R.ok(language);
    }

    @PutMapping("/languages/{id}")
    @RequirePermission("i18n:write")
    public R<SysLanguage> updateLanguage(@PathVariable Long id, @RequestBody SysLanguage language) {
        permissionService.requireSystem();
        language.setId(id);
        languageMapper.updateById(language);
        return R.ok(languageMapper.selectById(id));
    }

    @DeleteMapping("/languages/{id}")
    @RequirePermission("i18n:write")
    public R<Void> deleteLanguage(@PathVariable Long id) {
        permissionService.requireSystem();
        languageMapper.deleteById(id);
        return R.ok();
    }

    // ------------------------------------------------------------------ 菜单可视化
    @GetMapping("/menus")
    @RequirePermission("menu:read")
    public R<List<SysMenu>> menus(@RequestParam(defaultValue = "admin") String platform) {
        return R.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, platform)
                .in(SysMenu::getTenantId, List.of(0L, TenantContext.getTenantId()))
                .orderByAsc(SysMenu::getSort)));
    }

    @GetMapping("/menus/tree")
    @RequirePermission("menu:read")
    public R<List<Map<String, Object>>> menuTree(@RequestParam(defaultValue = "admin") String platform) {
        return R.ok(tenantService.menuTree(platform, TenantContext.getTenantId()));
    }

    @PostMapping("/menus")
    @RequirePermission("menu:write")
    public R<SysMenu> createMenu(@RequestBody SysMenu menu) {
        permissionService.requireSystem();
        menu.setId(null);
        menu.setTenantId(0L);
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getSort() == null) {
            menu.setSort(99);
        }
        menuMapper.insert(menu);
        auditService.record("menu", "CREATE", menu.getPlatform(), "新增菜单：" + menu.getName());
        return R.ok(menu);
    }

    @PutMapping("/menus/{id}")
    @RequirePermission("menu:write")
    public R<SysMenu> updateMenu(@PathVariable Long id, @RequestBody SysMenu menu) {
        permissionService.requireSystem();
        menu.setId(id);
        menuMapper.updateById(menu);
        auditService.record("menu", "UPDATE", String.valueOf(id), "修改菜单：" + menu.getName());
        return R.ok(menuMapper.selectById(id));
    }

    @PostMapping("/menus/{id}/visible")
    @RequirePermission("menu:write")
    public R<SysMenu> visible(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw BizException.notFound("菜单不存在");
        }
        if (body.get("visible") != null) {
            menu.setVisible(Integer.valueOf(String.valueOf(body.get("visible"))));
        }
        if (body.get("configJson") != null) {
            menu.setConfigJson(String.valueOf(body.get("configJson")));
        }
        menuMapper.updateById(menu);
        auditService.record("menu", "VISIBLE", String.valueOf(id), "菜单可视化配置");
        return R.ok(menu);
    }

    @DeleteMapping("/menus/{id}")
    @RequirePermission("menu:write")
    public R<Void> deleteMenu(@PathVariable Long id) {
        permissionService.requireSystem();
        menuMapper.deleteById(id);
        return R.ok();
    }

    // ------------------------------------------------------------------ 微信配置
    @GetMapping("/wechat/configs")
    @RequirePermission("wechat:read")
    public R<List<Map<String, Object>>> wechatConfigs() {
        List<SysWechatConfig> configs = wechatConfigMapper.selectList(
                new LambdaQueryWrapper<SysWechatConfig>().orderByAsc(SysWechatConfig::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysWechatConfig config : configs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", config.getId());
            item.put("appType", config.getAppType());
            item.put("appId", config.getAppId());
            item.put("appSecretMask", config.getAppSecretMask());
            item.put("token", config.getToken());
            item.put("aesKeyMask", config.getAesKeyEnc() == null ? null : "****");
            item.put("serverUrl", config.getServerUrl());
            item.put("authDomain", config.getAuthDomain());
            item.put("jsDomain", config.getJsDomain());
            item.put("remark", config.getRemark());
            result.add(item);
        }
        return R.ok(result);
    }

    @PutMapping("/wechat/configs")
    @RequirePermission("wechat:write")
    public R<SysWechatConfig> saveWechatConfig(@RequestBody Map<String, String> body) {
        permissionService.requireSystem();
        String appType = body.getOrDefault("appType", "MP");
        SysWechatConfig config = wechatConfigMapper.selectOne(new LambdaQueryWrapper<SysWechatConfig>()
                .eq(SysWechatConfig::getAppType, appType));
        if (config == null) {
            config = new SysWechatConfig();
            config.setTenantId(TenantContext.getTenantId());
            config.setAppType(appType);
            config.setAppId(body.get("appId"));
            applyWechatSecrets(config, body);
            wechatConfigMapper.insert(config);
        } else {
            if (body.get("appId") != null) {
                config.setAppId(body.get("appId"));
            }
            applyWechatSecrets(config, body);
            wechatConfigMapper.updateById(config);
        }
        auditService.record("wechat", "CONFIG", appType, "保存微信配置");
        return R.ok(config);
    }

    private void applyWechatSecrets(SysWechatConfig config, Map<String, String> body) {
        String secret = body.get("appSecret");
        if (secret != null && !secret.isBlank() && !secret.contains("****")) {
            config.setAppSecretEnc(cryptoService.encrypt(secret));
            config.setAppSecretMask(cryptoService.maskSecret(secret));
        }
        String aesKey = body.get("aesKey");
        if (aesKey != null && !aesKey.isBlank() && !aesKey.contains("****")) {
            config.setAesKeyEnc(cryptoService.encrypt(aesKey));
        }
        if (body.get("token") != null) {
            config.setToken(body.get("token"));
        }
        if (body.get("serverUrl") != null) {
            config.setServerUrl(body.get("serverUrl"));
        }
        if (body.get("authDomain") != null) {
            config.setAuthDomain(body.get("authDomain"));
        }
        if (body.get("jsDomain") != null) {
            config.setJsDomain(body.get("jsDomain"));
        }
        if (body.get("remark") != null) {
            config.setRemark(body.get("remark"));
        }
    }

    @PostMapping("/wechat/menu/sync")
    @RequirePermission("wechat:write")
    public R<Map<String, Object>> syncWechatMenu(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        String appType = body.get("appType") == null ? "MP" : String.valueOf(body.get("appType"));
        boolean success = weChatService.syncMenu(appType, body.get("menu"));
        auditService.record("wechat", "MENU_SYNC", appType, "同步公众号菜单：" + success);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("message", success ? "菜单已同步到微信" : "菜单同步失败，请检查微信配置");
        return R.ok(result);
    }

    // ------------------------------------------------------------------ 微信素材 / 标签 / 群发 / 订阅消息
    @PostMapping("/wechat/materials/upload")
    @RequirePermission("wechat:write")
    public R<Map<String, Object>> uploadWechatMaterial(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "image") String type,
                                                       @RequestParam(defaultValue = "MP") String appType)
            throws IOException {
        permissionService.requireSystem();
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的素材文件");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BizException(400, "素材不能超过 10MB");
        }
        Map<String, Object> result = weChatService.uploadMaterial(appType, type,
                file.getBytes(), file.getOriginalFilename());
        auditService.record("wechat", "MATERIAL_UPLOAD", String.valueOf(result.get("media_id")),
                "上传微信素材：" + file.getOriginalFilename());
        return R.ok(result);
    }

    @GetMapping("/wechat/materials")
    @RequirePermission("wechat:read")
    public R<Map<String, Object>> wechatMaterials(@RequestParam(defaultValue = "image") String type,
                                                  @RequestParam(defaultValue = "0") int offset,
                                                  @RequestParam(defaultValue = "20") int count,
                                                  @RequestParam(defaultValue = "MP") String appType) {
        return R.ok(weChatService.listMaterials(appType, type, offset, count));
    }

    @DeleteMapping("/wechat/materials/{mediaId}")
    @RequirePermission("wechat:write")
    public R<Void> deleteWechatMaterial(@PathVariable String mediaId,
                                        @RequestParam(defaultValue = "MP") String appType) {
        permissionService.requireSystem();
        weChatService.deleteMaterial(appType, mediaId);
        auditService.record("wechat", "MATERIAL_DELETE", mediaId, "删除微信永久素材");
        return R.ok();
    }

    @GetMapping("/wechat/tags")
    @RequirePermission("wechat:read")
    public R<List<?>> wechatTags(@RequestParam(defaultValue = "MP") String appType) {
        return R.ok(weChatService.listTags(appType));
    }

    @PostMapping("/wechat/tags")
    @RequirePermission("wechat:write")
    public R<Map<String, Object>> createWechatTag(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
        if (name == null || name.isBlank()) {
            throw new BizException(400, "标签名称不能为空");
        }
        Map<String, Object> result = weChatService.createTag(
                String.valueOf(body.getOrDefault("appType", "MP")), name);
        auditService.record("wechat", "TAG_CREATE", name, "创建微信粉丝标签");
        return R.ok(result);
    }

    @PostMapping("/wechat/tags/fans")
    @RequirePermission("wechat:write")
    public R<Map<String, Object>> tagWechatFans(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        Long tagId = body.get("tagId") == null ? null : Long.valueOf(String.valueOf(body.get("tagId")));
        List<String> openids = new ArrayList<>();
        Object list = body.get("openids");
        if (list instanceof List<?> items) {
            items.forEach(item -> openids.add(String.valueOf(item)));
        }
        if (tagId == null || openids.isEmpty()) {
            throw new BizException(400, "tagId 与 openids 不能为空");
        }
        Map<String, Object> result = weChatService.tagFans(
                String.valueOf(body.getOrDefault("appType", "MP")), tagId, openids);
        auditService.record("wechat", "TAG_FANS", String.valueOf(tagId), "批量打标签：" + openids.size() + " 人");
        return R.ok(result);
    }

    @PostMapping("/wechat/mass/send")
    @RequirePermission("wechat:write")
    public R<Map<String, Object>> massSend(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        if (content == null || content.isBlank()) {
            throw new BizException(400, "群发内容不能为空");
        }
        Long tagId = body.get("tagId") == null ? null : Long.valueOf(String.valueOf(body.get("tagId")));
        String msgType = String.valueOf(body.getOrDefault("msgType", "text"));
        Map<String, Object> result = weChatService.massSendByTag(
                String.valueOf(body.getOrDefault("appType", "MP")), tagId, msgType, content);
        auditService.record("wechat", "MASS_SEND", msgType, "按标签群发：" + (tagId == null ? "全部" : tagId));
        return R.ok(result);
    }

    @PostMapping("/wechat/subscribe/send")
    @RequirePermission("wechat:write")
    public R<Map<String, Object>> sendSubscribe(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        String openid = body.get("openid") == null ? null : String.valueOf(body.get("openid"));
        String templateId = body.get("templateId") == null ? null : String.valueOf(body.get("templateId"));
        if (openid == null || templateId == null) {
            throw new BizException(400, "openid 与 templateId 不能为空");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        Object raw = body.get("data");
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> data.put(String.valueOf(k), v));
        }
        boolean success = weChatService.sendSubscribe(openid, templateId, data,
                body.get("page") == null ? null : String.valueOf(body.get("page")));
        auditService.record("wechat", "SUBSCRIBE_SEND", templateId, "发送小程序订阅消息：" + success);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("message", success ? "订阅消息已发送" : "订阅消息发送失败，请检查小程序配置与用户授权");
        return R.ok(result);
    }

    // ------------------------------------------------------------------ 消息模板 / 站内消息
    @GetMapping("/message-templates")
    @RequirePermission("wechat:read")
    public R<List<SysMessageTemplate>> messageTemplates() {
        return R.ok(messageTemplateMapper.selectList(new LambdaQueryWrapper<SysMessageTemplate>()
                .orderByAsc(SysMessageTemplate::getBizType)));
    }

    @PostMapping("/message-templates")
    @RequirePermission("wechat:write")
    public R<SysMessageTemplate> createTemplate(@RequestBody SysMessageTemplate template) {
        template.setId(null);
        template.setTenantId(TenantContext.getTenantId());
        if (template.getEnabled() == null) {
            template.setEnabled(1);
        }
        messageTemplateMapper.insert(template);
        auditService.record("message", "TEMPLATE_CREATE", template.getBizType(), "新增消息模板");
        return R.ok(template);
    }

    @PutMapping("/message-templates/{id}")
    @RequirePermission("wechat:write")
    public R<SysMessageTemplate> updateTemplate(@PathVariable Long id, @RequestBody SysMessageTemplate template) {
        template.setId(id);
        messageTemplateMapper.updateById(template);
        return R.ok(messageTemplateMapper.selectById(id));
    }

    @DeleteMapping("/message-templates/{id}")
    @RequirePermission("wechat:write")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        messageTemplateMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/messages")
    public R<IPage<SysMessage>> messages(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Integer readFlag) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysMessage::getUserId, AuthContext.userId())
                .or().isNull(SysMessage::getUserId));
        if (readFlag != null) {
            wrapper.eq(SysMessage::getReadFlag, readFlag);
        }
        wrapper.orderByDesc(SysMessage::getId);
        return R.ok(messageMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/messages/{id}/read")
    public R<Void> readMessage(@PathVariable Long id) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw BizException.notFound("消息不存在");
        }
        if (message.getUserId() != null && !message.getUserId().equals(AuthContext.userId())) {
            throw BizException.forbidden("无权操作他人消息");
        }
        message.setReadFlag(1);
        messageMapper.updateById(message);
        return R.ok();
    }

    @PostMapping("/messages/send")
    @RequirePermission("system:write")
    public R<Void> sendMessage(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        notifyService.send(TenantContext.getTenantId(), userId,
                String.valueOf(body.getOrDefault("title", "系统消息")),
                String.valueOf(body.getOrDefault("content", "")),
                String.valueOf(body.getOrDefault("bizType", "SYSTEM")),
                body.get("bizId") == null ? null : Long.valueOf(String.valueOf(body.get("bizId"))));
        return R.ok();
    }

    // ------------------------------------------------------------------ 审计
    @GetMapping("/audit")
    @RequirePermission("audit:read")
    public R<IPage<SysAuditLog>> audit(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String module,
                                        @RequestParam(required = false) String action,
                                        @RequestParam(required = false) String start,
                                        @RequestParam(required = false) String end) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysAuditLog::getUsername, keyword)
                    .or().like(SysAuditLog::getTarget, keyword)
                    .or().like(SysAuditLog::getDetail, keyword));
        }
        if (module != null && !module.isBlank()) {
            wrapper.eq(SysAuditLog::getModule, module);
        }
        if (action != null && !action.isBlank()) {
            wrapper.eq(SysAuditLog::getAction, action);
        }
        if (start != null && !start.isBlank()) {
            wrapper.ge(SysAuditLog::getCreateTime, start.replace(" ", "T"));
        }
        if (end != null && !end.isBlank()) {
            wrapper.le(SysAuditLog::getCreateTime, end.replace(" ", "T"));
        }
        wrapper.orderByDesc(SysAuditLog::getId);
        return R.ok(auditLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/audit/export")
    @RequirePermission("audit:read")
    public void exportAudit(HttpServletResponse response) throws IOException {
        List<SysAuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<SysAuditLog>()
                .orderByDesc(SysAuditLog::getId).last("limit 5000"));
        List<Map<String, ?>> rows = new ArrayList<>();
        for (SysAuditLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", log.getCreateTime());
            row.put("username", log.getUsername());
            row.put("module", log.getModule());
            row.put("action", log.getAction());
            row.put("target", log.getTarget());
            row.put("detail", log.getDetail());
            row.put("ip", log.getIp());
            rows.add(row);
        }
        ExcelUtil.write(response, "审计日志", List.of("时间", "操作人", "模块", "动作", "对象", "详情", "IP"),
                List.of("time", "username", "module", "action", "target", "detail", "ip"), rows);
    }

    // ------------------------------------------------------------------ 存储 / 上传 / 附件
    @GetMapping("/storage")
    @RequirePermission("storage:read")
    public R<List<Map<String, Object>>> storage() {
        return R.ok(storageService.list());
    }

    @GetMapping("/storage/drivers")
    @RequirePermission("storage:read")
    public R<List<String>> drivers() {
        return R.ok(StorageService.DRIVERS);
    }

    @PostMapping("/storage/test")
    @RequirePermission("storage:write")
    public R<Map<String, Object>> testStorage(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        String driver = String.valueOf(body.getOrDefault("driver", "local"));
        @SuppressWarnings("unchecked")
        Map<String, Object> config = body.get("config") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : new LinkedHashMap<>();
        return R.ok(storageService.test(driver, config));
    }

    @PostMapping("/storage/save")
    @RequirePermission("storage:write")
    public R<SysStorageConfig> saveStorage(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        String driver = String.valueOf(body.getOrDefault("driver", "local"));
        @SuppressWarnings("unchecked")
        Map<String, Object> config = body.get("config") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : new LinkedHashMap<>();
        SysStorageConfig configEntity = storageService.save(driver, config);
        auditService.record("storage", "SAVE", driver, "保存并应用对象存储配置");
        return R.ok(configEntity);
    }

    @PostMapping("/upload")
    @RequirePermission("system:write")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String bizType,
                                          @RequestParam(required = false) Long bizId) {
        Map<String, Object> result = storageService.upload(file, bizType);
        SysAttachment attachment = new SysAttachment();
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setFileName(String.valueOf(result.get("fileName")));
        attachment.setUrl(String.valueOf(result.get("url")));
        attachment.setSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachmentMapper.insert(attachment);
        result.put("attachmentId", attachment.getId());
        auditService.record("storage", "UPLOAD", attachment.getFileName(), "上传文件：" + bizType);
        return R.ok(result);
    }

    /**
     * 登录用户通用上传：事件/隐患/巡检/报修等一线取证，不要求系统管理权限。
     */
    @PostMapping("/upload/user")
    public R<Map<String, Object>> userUpload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String bizType,
                                              @RequestParam(required = false) Long bizId) {
        accessGuard.requireSelfService();
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/") && !"application/pdf".equals(contentType)) {
            throw new BizException(400, "仅支持上传图片或 PDF 附件");
        }
        return upload(file, bizType, bizId);
    }

    @GetMapping("/attachments")
    @RequirePermission("system:read")
    public R<List<SysAttachment>> attachments(@RequestParam(required = false) String bizType,
                                                @RequestParam(required = false) Long bizId) {
        LambdaQueryWrapper<SysAttachment> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(SysAttachment::getBizType, bizType);
        }
        if (bizId != null) {
            wrapper.eq(SysAttachment::getBizId, bizId);
        }
        wrapper.orderByDesc(SysAttachment::getId);
        return R.ok(attachmentMapper.selectList(wrapper));
    }

    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("time", LocalDateTime.now());
        result.put("tenantId", TenantContext.getTenantId());
        result.put("user", AuthContext.username());
        return R.ok(result);
    }
}
