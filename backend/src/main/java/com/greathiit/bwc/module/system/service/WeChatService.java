package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.module.system.entity.SysWechatConfig;
import com.greathiit.bwc.module.system.mapper.SysWechatConfigMapper;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 微信生态：code2Session、网页授权、access_token、自定义菜单同步、模板消息发送、回调验签。
 * 真实能力依赖服务号认证与合法域名 wx-bwc-api.greathiit.com；未配置时降级并给出明确提示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatService {

    private static final String API = "https://api.weixin.qq.com";
    private final SysWechatConfigMapper configMapper;
    private final CryptoService cryptoService;

    @Value("${bwc.wechat.mp-app-id:}")
    private String envMpAppId;
    @Value("${bwc.wechat.mp-app-secret:}")
    private String envMpSecret;
    @Value("${bwc.wechat.mini-app-id:}")
    private String envMiniAppId;
    @Value("${bwc.wechat.mini-app-secret:}")
    private String envMiniSecret;

    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenExpire = new ConcurrentHashMap<>();
    private final RestTemplate rest = new RestTemplate(clientFactory());

    private static SimpleClientHttpRequestFactory clientFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        return factory;
    }

    public SysWechatConfig config(Long tenantId, String appType) {
        return configMapper.selectOne(new LambdaQueryWrapper<SysWechatConfig>()
                .eq(SysWechatConfig::getTenantId, tenantId)
                .eq(SysWechatConfig::getAppType, appType));
    }

    private String appId(Long tenantId, String appType) {
        SysWechatConfig config = config(tenantId, appType);
        if (config != null && config.getAppId() != null && !config.getAppId().isBlank()) {
            return config.getAppId();
        }
        return "MINI".equals(appType) ? envMiniAppId : envMpAppId;
    }

    private String appSecret(Long tenantId, String appType) {
        SysWechatConfig config = config(tenantId, appType);
        if (config != null && config.getAppSecretEnc() != null) {
            String secret = cryptoService.decrypt(config.getAppSecretEnc());
            if (secret != null && !secret.isBlank()) {
                return secret;
            }
        }
        return "MINI".equals(appType) ? envMiniSecret : envMpSecret;
    }

    /**
     * 小程序 code2Session；H5/公众号使用网页授权 code 换取用户信息。
     */
    public Map<String, String> code2Session(String appType, String code) {
        Long tenantId = TenantContext.getTenantId() == 0 ? 1L : TenantContext.getTenantId();
        String type = appType == null || appType.isBlank() ? "MINI" : appType;
        String appid = appId(tenantId, type);
        String secret = appSecret(tenantId, type);
        if (appid == null || appid.isBlank() || secret == null || secret.isBlank()) {
            throw new BizException(400, "微信登录尚未配置，请在后台【微信配置】填写 AppID/AppSecret");
        }
        String url;
        if ("MINI".equals(type)) {
            url = API + "/sns/jscode2session?appid=" + appid + "&secret=" + secret
                    + "&js_code=" + code + "&grant_type=authorization_code";
        } else {
            url = API + "/sns/oauth2/access_token?appid=" + appid + "&secret=" + secret
                    + "&code=" + code + "&grant_type=authorization_code";
        }
        Map<?, ?> response = rest.getForObject(url, Map.class);
        Map<String, String> result = new HashMap<>();
        if (response == null) {
            throw new BizException(500, "微信接口无响应");
        }
        if (response.get("errcode") != null && !"0".equals(String.valueOf(response.get("errcode")))) {
            throw new BizException(400, "微信登录失败：" + response.get("errmsg"));
        }
        response.forEach((k, v) -> result.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        return result;
    }

    public String accessToken(String appType) {
        Long tenantId = TenantContext.getTenantId() == 0 ? 1L : TenantContext.getTenantId();
        String cacheKey = tenantId + ":" + appType;
        Long expire = tokenExpire.get(cacheKey);
        if (expire != null && expire > System.currentTimeMillis()) {
            return tokenCache.get(cacheKey);
        }
        String appid = appId(tenantId, appType);
        String secret = appSecret(tenantId, appType);
        if (appid == null || secret == null || appid.isBlank() || secret.isBlank()) {
            return null;
        }
        String url = API + "/cgi-bin/token?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        Map<?, ?> response = rest.getForObject(url, Map.class);
        if (response == null || response.get("access_token") == null) {
            log.warn("获取微信 access_token 失败: {}", response);
            return null;
        }
        String token = String.valueOf(response.get("access_token"));
        tokenCache.put(cacheKey, token);
        long expiresIn = response.get("expires_in") == null ? 7200 : Long.parseLong(String.valueOf(response.get("expires_in")));
        tokenExpire.put(cacheKey, System.currentTimeMillis() + (expiresIn - 300) * 1000);
        return token;
    }

    public boolean syncMenu(String appType, Object menuJson) {
        String token = accessToken(appType);
        if (token == null) {
            throw new BizException(400, "微信 access_token 不可用，请检查微信配置");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = menuJson instanceof String s ? s : cn.hutool.json.JSONUtil.toJsonStr(menuJson);
        Map<?, ?> response = rest.postForObject(API + "/cgi-bin/menu/create?access_token=" + token,
                new HttpEntity<>(body, headers), Map.class);
        return response != null && "0".equals(String.valueOf(response.get("errcode")));
    }

    public boolean sendTemplate(String appType, String openid, String templateId, Map<String, Object> data,
                                String url) {
        String token = accessToken(appType);
        if (token == null) {
            return false;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        body.put("url", url);
        body.put("data", data);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            Map<?, ?> response = rest.postForObject(API + "/cgi-bin/message/template/send?access_token=" + token,
                    new HttpEntity<>(body, headers), Map.class);
            return response != null && "0".equals(String.valueOf(response.get("errcode")));
        } catch (Exception e) {
            log.warn("模板消息发送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 上传公众号永久素材（image/voice/video/thumb）。返回微信原始响应，含 media_id 与 url。
     */
    public Map<String, Object> uploadMaterial(String appType, String type, byte[] bytes, String filename) {
        String token = requireToken(appType);
        String normalized = type == null || type.isBlank() ? "image" : type.toLowerCase();
        if (!List.of("image", "voice", "video", "thumb").contains(normalized)) {
            throw new BizException(400, "不支持的素材类型：" + type);
        }
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("media", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? "material." + normalized : filename;
            }
        });
        if ("video".equals(normalized)) {
            form.add("description", "{\"title\":\"校园安全视频\",\"introduction\":\"\"}");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            Map<?, ?> response = rest.postForObject(
                    API + "/cgi-bin/material/add_material?access_token=" + token + "&type=" + normalized,
                    new HttpEntity<>(form, headers), Map.class);
            return requireOk(response, "素材上传失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "素材上传失败：" + e.getMessage());
        }
    }

    /**
     * 分页拉取永久素材列表。
     */
    public Map<String, Object> listMaterials(String appType, String type, int offset, int count) {
        String token = requireToken(appType);
        String normalized = type == null || type.isBlank() ? "image" : type.toLowerCase();
        Map<String, Object> body = new HashMap<>();
        body.put("type", normalized);
        body.put("offset", Math.max(offset, 0));
        body.put("count", Math.min(Math.max(count, 1), 20));
        return requireOk(postJson(API + "/cgi-bin/material/batchget_material?access_token=" + token, body),
                "素材列表获取失败");
    }

    public boolean deleteMaterial(String appType, String mediaId) {
        String token = requireToken(appType);
        Map<String, Object> body = new HashMap<>();
        body.put("media_id", mediaId);
        Map<String, Object> response = requireOk(
                postJson(API + "/cgi-bin/material/del_material?access_token=" + token, body), "素材删除失败");
        return "0".equals(String.valueOf(response.get("errcode")));
    }

    public List<?> listTags(String appType) {
        String token = requireToken(appType);
        Map<String, Object> response = requireOk(rest.getForObject(
                API + "/cgi-bin/tags/get?access_token=" + token, Map.class), "标签列表获取失败");
        Object tags = response.get("tags");
        return tags instanceof List<?> list ? list : List.of();
    }

    public Map<String, Object> createTag(String appType, String name) {
        String token = requireToken(appType);
        Map<String, Object> tag = new HashMap<>();
        tag.put("name", name);
        Map<String, Object> body = new HashMap<>();
        body.put("tag", tag);
        Map<String, Object> response = requireOk(
                postJson(API + "/cgi-bin/tags/create?access_token=" + token, body), "标签创建失败");
        Object created = response.get("tag");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("tag", created);
        return result;
    }

    public Map<String, Object> tagFans(String appType, Long tagId, List<String> openids) {
        String token = requireToken(appType);
        Map<String, Object> body = new HashMap<>();
        body.put("tagid", tagId);
        body.put("openid_list", openids);
        return requireOk(postJson(API + "/cgi-bin/tags/members/batchtagging?access_token=" + token, body),
                "粉丝打标签失败");
    }

    /**
     * 按标签群发：msgType=text 时 content 为文本；msgType=mpnews 时 content 为素材 media_id。
     */
    public Map<String, Object> massSendByTag(String appType, Long tagId, String msgType, String content) {
        String token = requireToken(appType);
        Map<String, Object> filter = new HashMap<>();
        filter.put("is_to_all", tagId == null || tagId <= 0);
        if (tagId != null && tagId > 0) {
            filter.put("tag_id", tagId);
        }
        Map<String, Object> media = new LinkedHashMap<>();
        if ("text".equalsIgnoreCase(msgType)) {
            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            media.put("text", text);
        } else {
            Map<String, Object> mpnews = new HashMap<>();
            mpnews.put("media_id", content);
            media.put("mpnews", mpnews);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("filter", filter);
        body.put("msgtype", "text".equalsIgnoreCase(msgType) ? "text" : "mpnews");
        body.putAll(media);
        return requireOk(postJson(API + "/cgi-bin/message/mass/sendall?access_token=" + token, body), "群发失败");
    }

    /**
     * 小程序订阅消息发送。
     */
    public boolean sendSubscribe(String openid, String templateId, Map<String, Object> data, String page) {
        String token = accessToken("MINI");
        if (token == null) {
            return false;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        body.put("page", page == null ? "pages/index/index" : page);
        body.put("data", data);
        body.put("miniprogram_state", "formal");
        body.put("lang", "zh_CN");
        try {
            Map<?, ?> response = postJson(API + "/cgi-bin/message/subscribe/send?access_token=" + token, body);
            return response != null && "0".equals(String.valueOf(response.get("errcode")));
        } catch (Exception e) {
            log.warn("订阅消息发送失败: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> postJson(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForObject(url, new HttpEntity<>(body, headers), Map.class);
    }

    private String requireToken(String appType) {
        String token = accessToken(appType == null || appType.isBlank() ? "MP" : appType);
        if (token == null) {
            throw new BizException(400, "微信 access_token 不可用，请先在【微信配置】填写 AppID/AppSecret 并确保 IP 白名单已放行");
        }
        return token;
    }

    private Map<String, Object> requireOk(Map<?, ?> response, String message) {
        if (response == null) {
            throw new BizException(500, message + "：微信接口无响应");
        }
        Object errcode = response.get("errcode");
        if (errcode != null && !"0".equals(String.valueOf(errcode))) {
            throw new BizException(400, message + "：" + response.get("errmsg") + "（" + errcode + "）");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        response.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    /**
     * 公众号服务器回调验签：sha1(sort(token,timestamp,nonce))。
     */
    public boolean checkSignature(String token, String timestamp, String nonce, String signature) {
        if (token == null || signature == null) {
            return false;
        }
        String raw = Arrays.asList(token, timestamp, nonce).stream().sorted()
                .collect(Collectors.joining());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
