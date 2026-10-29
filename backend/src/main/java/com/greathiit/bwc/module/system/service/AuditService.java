package com.greathiit.bwc.module.system.service;

import com.greathiit.bwc.module.system.entity.SysAuditLog;
import com.greathiit.bwc.module.system.mapper.SysAuditLogMapper;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志：平台与租户的全部写操作、越权访问、敏感查询均留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SysAuditLogMapper auditLogMapper;

    public void record(String module, String action, String target, String detail) {
        try {
            SysAuditLog entity = new SysAuditLog();
            entity.setTenantId(TenantContext.getTenantId());
            entity.setUserId(AuthContext.get() == null ? null : AuthContext.userId());
            entity.setUsername(AuthContext.username());
            entity.setModule(module);
            entity.setAction(action);
            entity.setTarget(target);
            entity.setDetail(detail);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            HttpServletRequest request = currentRequest();
            if (request != null) {
                entity.setIp(clientIp(request));
                entity.setUserAgent(request.getHeader("User-Agent"));
            }
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
