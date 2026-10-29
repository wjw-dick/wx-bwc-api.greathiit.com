package com.greathiit.bwc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${bwc.jwt-secret}") String secret,
                   @Value("${bwc.jwt-expire-hours:24}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String create(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("tenantId", user.getTenantId())
                .claim("tenantCode", user.getTenantCode())
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("roleCode", user.getRoleCode())
                .claim("userType", user.getUserType())
                .claim("platform", user.isPlatform())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public LoginUser parse(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(claims.getSubject()));
        user.setTenantId(toLong(claims.get("tenantId")));
        user.setTenantCode((String) claims.get("tenantCode"));
        user.setUsername((String) claims.get("username"));
        user.setRealName((String) claims.get("realName"));
        user.setRoleCode((String) claims.get("roleCode"));
        user.setUserType((String) claims.get("userType"));
        user.setPlatform(Boolean.TRUE.equals(claims.get("platform")));
        return user;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return value instanceof Number n ? n.longValue() : Long.valueOf(value.toString());
    }
}
