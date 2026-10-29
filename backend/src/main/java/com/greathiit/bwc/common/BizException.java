package com.greathiit.bwc.common;

import lombok.Getter;

/**
 * 业务异常，code 与 HTTP 状态保持一致（400/401/403/404/429/500）。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException forbidden(String message) {
        return new BizException(403, message);
    }

    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public static BizException tooMany(String message) {
        return new BizException(429, message);
    }
}
