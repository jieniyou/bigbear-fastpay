package com.fastpay.util;

import com.fastpay.common.BusinessException;
import org.springframework.util.StringUtils;

/**
 * 回调与跳转 URL 校验工具。
 *
 * 三方系统常会在 returnUrl 中追加订单号、状态和恢复 Token，长度会明显超过普通配置地址。
 */
public final class CallbackUrlUtil {

    /**
     * 回调与跳转 URL 最大长度，匹配数据库 VARCHAR(2048) 字段容量。
     */
    public static final int MAX_URL_LENGTH = 2048;

    private CallbackUrlUtil() {
    }

    /**
     * 校验 URL 长度，空值原样返回。
     *
     * @param url       待校验 URL
     * @param fieldName 字段中文名称
     * @return 原始 URL
     */
    public static String ensureMaxLength(String url, String fieldName) {
        if (StringUtils.hasText(url) && url.length() > MAX_URL_LENGTH) {
            throw BusinessException.badRequest(fieldName + "不能超过" + MAX_URL_LENGTH + "个字符");
        }
        return url;
    }
}
