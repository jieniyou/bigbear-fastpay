package com.fastpay.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * 客户端 IP 解析工具。
 *
 * 代理可能把多个地址放在 X-Forwarded-For 中，订单只保存一个合法地址，
 * 避免把整段代理链写入数据库导致字段超长，也避免把异常请求头当作 IP。
 */
public final class ClientIpUtil {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private ClientIpUtil() {
    }

    /**
     * 按代理头优先级解析客户端 IP；无法解析时回退到 Servlet 连接地址。
     *
     * @param request 当前 HTTP 请求
     * @return 单个合法 IPv4/IPv6 地址，完全无法识别时返回 0.0.0.0
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        for (String headerName : headerNames) {
            String ip = firstValidIp(request.getHeader(headerName));
            if (ip != null) {
                return ip;
            }
        }

        String remoteIp = normalizeToken(request.getRemoteAddr());
        return isValidIp(remoteIp) ? remoteIp : "0.0.0.0";
    }

    /**
     * 从代理地址列表中选择第一个合法地址，兼容逗号分隔的多级代理链。
     *
     * @param header 代理请求头内容
     * @return 合法地址；不存在时返回 null
     */
    private static String firstValidIp(String header) {
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        String[] candidates = header.split(",");
        for (String candidate : candidates) {
            String ip = normalizeToken(candidate);
            if (isValidIp(ip)) {
                return ip;
            }
        }
        return null;
    }

    /**
     * 清理代理头中的引号、IPv6 方括号和 IPv4 地址端口后缀。
     *
     * @param value 原始地址片段
     * @return 可校验的地址文本
     */
    private static String normalizeToken(String value) {
        if (value == null) {
            return null;
        }
        String token = value.trim();
        if (token.isEmpty() || "unknown".equalsIgnoreCase(token)) {
            return null;
        }
        if (token.startsWith("[") && token.contains("]")) {
            token = token.substring(1, token.indexOf(']'));
        } else if (token.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}:\\d{1,5}$")) {
            token = token.substring(0, token.lastIndexOf(':'));
        }
        return token;
    }

    /**
     * 校验 IPv4 每段范围及 IPv6 的数字字符格式。
     *
     * @param ip 待校验地址
     * @return 是否为合法地址格式
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if (IPV4_PATTERN.matcher(ip).matches()) {
            String[] parts = ip.split("\\.");
            for (String part : parts) {
                try {
                    if (Integer.parseInt(part) > 255) {
                        return false;
                    }
                } catch (NumberFormatException ex) {
                    return false;
                }
            }
            return true;
        }
        return ip.contains(":") && IPV6_PATTERN.matcher(ip).matches();
    }
}
