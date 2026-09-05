package com.fastpay.util;

import com.fastpay.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回调 URL 长度校验测试，确保三方系统长回跳地址不会被错误限制为 255 字符。
 */
class CallbackUrlUtilTest {

    private static final String URL_PREFIX = "https://sub2api.wuyis.cn/payment/result?token=";

    /**
     * 2048 字符以内的 URL 应允许正常保存。
     */
    @Test
    void shouldAllowUrlWithinLimit() {
        String url = URL_PREFIX + "a".repeat(CallbackUrlUtil.MAX_URL_LENGTH - URL_PREFIX.length());

        assertEquals(url, CallbackUrlUtil.ensureMaxLength(url, "支付成功跳转地址"));
    }

    /**
     * 超过数据库容量的 URL 应返回业务错误，而不是落到数据库截断异常。
     */
    @Test
    void shouldRejectUrlOverLimit() {
        String url = URL_PREFIX + "a".repeat(CallbackUrlUtil.MAX_URL_LENGTH - URL_PREFIX.length() + 1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> CallbackUrlUtil.ensureMaxLength(url, "支付成功跳转地址"));

        assertEquals(400, exception.getCode());
        assertEquals("支付成功跳转地址不能超过2048个字符", exception.getMessage());
    }
}
