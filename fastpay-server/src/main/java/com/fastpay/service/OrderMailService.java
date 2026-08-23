package com.fastpay.service;

import com.fastpay.entity.Merchant;
import com.fastpay.entity.PayOrder;
import com.fastpay.entity.Shop;

/**
 * 订单邮件服务接口
 * 负责订单创建后的商户邮件通知和 SMTP 测试。
 *
 * @author FastPay
 */
public interface OrderMailService {

    /**
     * 异步发送订单创建通知。
     *
     * @param order    支付订单
     * @param merchant 商户信息
     * @param shop     店铺信息
     */
    void sendOrderCreatedNotice(PayOrder order, Merchant merchant, Shop shop);

    /**
     * 发送测试邮件。
     *
     * @param testEmail 测试收件邮箱
     */
    void sendTestMail(String testEmail);
}
