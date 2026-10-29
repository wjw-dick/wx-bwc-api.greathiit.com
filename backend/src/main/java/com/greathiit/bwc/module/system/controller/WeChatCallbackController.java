package com.greathiit.bwc.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.module.system.entity.SysMessageLog;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysWechatConfig;
import com.greathiit.bwc.module.system.mapper.SysMessageLogMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysWechatConfigMapper;
import com.greathiit.bwc.module.system.service.WeChatService;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信公众号服务器回调：验签、关注/取关事件、关键词自动回复。
 * 合法域名：wx-bwc-api.greathiit.com，URL：https://wx-bwc-api.greathiit.com/api/wechat/callback
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat")
@RequiredArgsConstructor
public class WeChatCallbackController {

    private final WeChatService weChatService;
    private final SysWechatConfigMapper configMapper;
    private final SysMessageLogMapper messageLogMapper;
    private final SysTenantMapper tenantMapper;

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@RequestParam(required = false) String signature,
                         @RequestParam(required = false) String timestamp,
                         @RequestParam(required = false) String nonce,
                         @RequestParam(required = false) String echostr,
                         @RequestParam(required = false) String tenant) {
        String token = token(tenant);
        if (weChatService.checkSignature(token, timestamp, nonce, signature)) {
            log.info("微信公众号回调验签通过");
            return echostr;
        }
        log.warn("微信公众号回调验签失败");
        return "invalid signature";
    }

    @PostMapping(value = "/callback", produces = MediaType.APPLICATION_XML_VALUE)
    public String receive(@RequestBody String xml,
                          @RequestParam(required = false) String tenant) {
        Map<String, String> message = parse(xml);
        String from = message.getOrDefault("FromUserName", "");
        String type = message.getOrDefault("MsgType", "event");
        String event = message.getOrDefault("Event", "");
        String content = message.getOrDefault("Content", "");

        SysMessageLog logEntity = new SysMessageLog();
        Long tid = tenantId(tenant);
        logEntity.setTenantId(tid);
        logEntity.setChannel("MP_CALLBACK");
        logEntity.setBizType(type.equals("event") ? "EVENT_" + event : "TEXT");
        logEntity.setToUser(from);
        logEntity.setContent(content);
        logEntity.setStatus("RECEIVED");
        TenantContext.set(tid, true, false, false);
        try {
            messageLogMapper.insert(logEntity);
        } finally {
            TenantContext.clear();
        }

        if ("event".equals(type) && "subscribe".equalsIgnoreCase(event)) {
            return reply(from, "欢迎关注校园安全服务号！\n"
                    + "回复【访客】预约入校，回复【失物】查看失物招领，"
                    + "回复【报修】提交设备报修，回复【考试】进入安全考试。");
        }
        if ("text".equals(type)) {
            String keyword = content == null ? "" : content.trim();
            if (keyword.contains("访客")) {
                return reply(from, "访客预约：请点击菜单【访客预约】或访问 "
                        + "https://wx-bwc-api.greathiit.com/visitor 提交预约。");
            }
            if (keyword.contains("失物")) {
                return reply(from, "失物招领：请点击菜单【失物招领】查看最新公告。");
            }
            if (keyword.contains("报修")) {
                return reply(from, "设备报修：请点击菜单【报修】填写故障信息，保卫处将尽快处理。");
            }
            if (keyword.contains("考试")) {
                return reply(from, "安全考试：请点击菜单【安全教育】进入课程与考试。");
            }
            return reply(from, "感谢留言。回复【访客】【失物】【报修】【考试】获取对应服务。");
        }
        return "success";
    }

    private String token(String tenantCode) {
        SysWechatConfig config = config(tenantCode);
        return config == null ? "bwc-token" : config.getToken();
    }

    private Long tenantId(String tenantCode) {
        SysTenant tenant = resolveTenant(tenantCode);
        if (tenant != null) {
            return tenant.getId();
        }
        SysWechatConfig config = config(tenantCode);
        return config == null ? 1L : config.getTenantId();
    }

    private SysTenant resolveTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }
        return tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, tenantCode));
    }

    private SysWechatConfig config(String tenantCode) {
        SysTenant tenant = resolveTenant(tenantCode);
        TenantContext.set(tenant == null ? 0L : tenant.getId(), true, false, false);
        try {
            LambdaQueryWrapper<SysWechatConfig> wrapper = new LambdaQueryWrapper<SysWechatConfig>()
                    .eq(SysWechatConfig::getAppType, "MP");
            if (tenant != null) {
                wrapper.eq(SysWechatConfig::getTenantId, tenant.getId());
            }
            wrapper.orderByAsc(SysWechatConfig::getId).last("limit 1");
            return configMapper.selectOne(wrapper);
        } finally {
            TenantContext.clear();
        }
    }

    private Map<String, String> parse(String xml) {
        Map<String, String> result = new LinkedHashMap<>();
        if (xml == null || xml.isBlank()) {
            return result;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            var nodes = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                var node = nodes.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    result.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception e) {
            log.warn("微信回调 XML 解析失败: {}", e.getMessage());
        }
        return result;
    }

    private String reply(String to, String content) {
        String from = "gh_bwc_service";
        return "<xml><ToUserName><![CDATA[" + to + "]]></ToUserName>"
                + "<FromUserName><![CDATA[" + from + "]]></FromUserName>"
                + "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[" + content + "]]></Content></xml>";
    }
}
