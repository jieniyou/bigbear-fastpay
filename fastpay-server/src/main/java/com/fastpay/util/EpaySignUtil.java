package com.fastpay.util;

import cn.hutool.crypto.SecureUtil;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * 标准易支付签名工具类
 * 兼容常见 Epay 协议：参数按字典序拼接，排除 sign/sign_type，最后直接追加商户密钥后 MD5。
 *
 * @author FastPay
 */
public class EpaySignUtil {

    private EpaySignUtil() {
    }

    /**
     * 生成标准易支付签名。
     *
     * @param params    请求参数
     * @param apiSecret 商户 API Secret
     * @return MD5 签名
     */
    public static String generateSign(Map<String, ?> params, String apiSecret) {
        return SecureUtil.md5(buildSignContent(params) + apiSecret);
    }

    /**
     * 验证标准易支付签名。
     *
     * @param params    请求参数
     * @param apiSecret 商户 API Secret
     * @return 签名是否正确
     */
    public static boolean verifySign(Map<String, ?> params, String apiSecret) {
        Object signObj = params.get("sign");
        if (signObj == null || !StringUtils.hasText(String.valueOf(signObj))) {
            return false;
        }
        String calculatedSign = generateSign(params, apiSecret);
        return String.valueOf(signObj).equalsIgnoreCase(calculatedSign);
    }

    /**
     * 构建标准易支付待签名字符串。
     *
     * @param params 请求参数
     * @return 待签名字符串
     */
    public static String buildSignContent(Map<String, ?> params) {
        Map<String, String> filteredParams = new TreeMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if ("sign".equalsIgnoreCase(key) || "sign_type".equalsIgnoreCase(key)) {
                continue;
            }
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                continue;
            }
            filteredParams.put(key, String.valueOf(value));
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filteredParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
