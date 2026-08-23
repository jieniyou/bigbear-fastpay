package com.fastpay.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 客户端 IP 解析回归测试，锁定反向代理多地址头不能写入订单字段的行为。
 */
class ClientIpUtilTest {

    /**
     * 代理链只取第一个合法地址，避免整段头部造成数据库截断。
     */
    @Test
    void shouldUseFirstValidForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.8, 10.0.0.2, 2001:db8::1");

        assertEquals("203.0.113.8", ClientIpUtil.resolve(request));
    }

    /**
     * 代理头为未知值时回退连接地址，不把 unknown 或端口文本写入订单。
     */
    @Test
    void shouldFallbackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "not-an-ip");
        request.setRemoteAddr("198.51.100.22");

        assertEquals("198.51.100.22", ClientIpUtil.resolve(request));
    }

    /**
     * IPv4 地址带端口时只保留地址部分，兼容部分反向代理实现。
     */
    @Test
    void shouldStripIpv4Port() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.22:443");

        assertEquals("198.51.100.22", ClientIpUtil.resolve(request));
    }
}
