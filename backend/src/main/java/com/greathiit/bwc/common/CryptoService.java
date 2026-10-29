package com.greathiit.bwc.common;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AES 加解密：身份证号、手机号、车牌识别 API Key、微信密钥等敏感字段。
 */
@Component
public class CryptoService {

    private final AES aes;

    public CryptoService(@Value("${bwc.aes-key}") String key) {
        byte[] keyBytes = DigestUtil.sha256(key.getBytes(StandardCharsets.UTF_8));
        this.aes = SecureUtil.aes(keyBytes);
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        return aes.encryptBase64(plain);
    }

    public String decrypt(String cipher) {
        if (cipher == null || cipher.isEmpty()) {
            return null;
        }
        try {
            return aes.decryptStr(cipher);
        } catch (Exception e) {
            return null;
        }
    }

    public String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "****";
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return "****";
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
