package com.cen.ai;

/** AI 调用失败（HTTP 非 2xx、网络异常、JSON 解析失败等）。 */
public class AiCallException extends RuntimeException {
    public AiCallException(String message) {
        super(message);
    }
    public AiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
