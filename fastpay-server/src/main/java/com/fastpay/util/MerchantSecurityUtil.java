package com.fastpay.util;

import com.fastpay.entity.Merchant;

import java.util.List;

/**
 * 商户信息脱敏工具类
 * 统一清理接口响应中的登录密码和批量列表中的密钥 Secret。
 *
 * @author FastPay
 */
public class MerchantSecurityUtil {

    private MerchantSecurityUtil() {
    }

    /**
     * 隐藏商户登录密码，保留 API Secret 供授权用户查看或重置后复制。
     *
     * @param merchant 商户实体
     * @return 已脱敏的商户实体
     */
    public static Merchant hidePassword(Merchant merchant) {
        if (merchant != null) {
            merchant.setPassword(null);
        }
        return merchant;
    }

    /**
     * 隐藏商户敏感凭据，适用于分页和下拉列表等批量查询场景。
     *
     * @param merchant 商户实体
     * @return 已脱敏的商户实体
     */
    public static Merchant hideCredentials(Merchant merchant) {
        if (merchant != null) {
            merchant.setPassword(null);
            merchant.setApiSecret(null);
        }
        return merchant;
    }

    /**
     * 批量隐藏商户敏感凭据。
     *
     * @param merchants 商户列表
     * @return 已脱敏的商户列表
     */
    public static List<Merchant> hideCredentials(List<Merchant> merchants) {
        if (merchants != null) {
            merchants.forEach(MerchantSecurityUtil::hideCredentials);
        }
        return merchants;
    }
}
