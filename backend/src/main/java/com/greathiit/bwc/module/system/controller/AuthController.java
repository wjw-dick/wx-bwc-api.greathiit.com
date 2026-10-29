package com.greathiit.bwc.module.system.controller;

import com.greathiit.bwc.common.R;
import com.greathiit.bwc.common.RateLimitService;
import com.greathiit.bwc.module.system.dto.ChangePasswordRequest;
import com.greathiit.bwc.module.system.dto.LoginRequest;
import com.greathiit.bwc.module.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * 认证：账号密码登录、微信授权登录/绑定、当前用户、修改密码。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        rateLimitService.check("login:" + request.getUsername(), 10, Duration.ofMinutes(1));
        return R.ok(authService.login(request));
    }

    @PostMapping("/wx-login")
    public R<Map<String, Object>> wxLogin(@RequestBody Map<String, String> body) {
        rateLimitService.check("wx-login", 30, Duration.ofMinutes(1));
        return R.ok(authService.wxLogin(body.get("code"), body.get("appType")));
    }

    @PostMapping("/wx-bind")
    public R<Map<String, Object>> wxBind(@RequestBody Map<String, String> body) {
        rateLimitService.check("wx-bind", 10, Duration.ofMinutes(1));
        return R.ok(authService.wxBind(body.get("username"), body.get("password"),
                body.get("code"), body.get("appType")));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.me());
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return R.ok();
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}
