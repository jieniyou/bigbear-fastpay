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
     * @param requestOrigin 下单请求公网 Origin，邮件配置外部地址留空时用于默认本站域名
     */
    void sendOrderCreatedNotice(PayOrder order, Merchant merchant, Shop shop, String requestOrigin);

    /**
     * 异步发送订单确认通知。
     *
     * @param order 支付订单
     */
    void sendOrderConfirmedNotice(PayOrder order);

    /**
     * 异步发送订单确认通知，并携带当前访问域名用于邮件链接兜底。
     *
     * @param order         支付订单
     * @param requestOrigin 当前请求公网 Origin
     */
    void sendOrderConfirmedNotice(PayOrder order, String requestOrigin);

    /**
     * 异步发送订单关闭通知。
     *
     * @param order 支付订单
     */
    void sendOrderClosedNotice(PayOrder order);

    /**
     * 异步发送订单关闭通知，并携带当前访问域名用于邮件链接兜底。
     *
     * @param order         支付订单
     * @param requestOrigin 当前请求公网 Origin
     */
    void sendOrderClosedNotice(PayOrder order, String requestOrigin);

    /**
     * 发送测试邮件。
     *
     * @param testEmail 测试收件邮箱
     */
    void sendTestMail(String testEmail);
}
