package com.fastpay.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 公网访问地址工具类
 * 根据反向代理请求头自动推导用户实际访问的协议和域名/IP。
 *
 * @author FastPay
 */
public class PublicUrlUtil {

    private PublicUrlUtil() {
    }

    /**
     * 获取公网访问 Origin，例如 https://pay.example.com 或 http://1.2.3.4:18080。
     *
     * @param request 当前请求
     * @return 公网 Origin
     */
    public static String getPublicOrigin(HttpServletRequest request) {
        String protocol = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        if (!StringUtils.hasText(protocol)) {
            protocol = request.getScheme();
        }

        String host = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (!StringUtils.hasText(host)) {
            host = request.getHeader("Host");
        }
        if (!StringUtils.hasText(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (port > 0 && !isDefaultPort(protocol, port)) {
                host = host + ":" + port;
            }
        }

        return protocol + "://" + host;
    }

    /**
     * 读取代理头的第一个值。
     *
     * @param header 请求头
     * @return 第一个请求头值
     */
    private static String firstHeaderValue(String header) {
        if (!StringUtils.hasText(header)) {
            return "";
        }
        return header.split(",")[0].trim();
    }

    /**
     * 判断端口是否为协议默认端口。
     *
     * @param protocol 协议
     * @param port     端口
     * @return 是否默认端口
     */
    private static boolean isDefaultPort(String protocol, int port) {
        return ("http".equalsIgnoreCase(protocol) && port == 80)
                || ("https".equalsIgnoreCase(protocol) && port == 443);
    }
}
