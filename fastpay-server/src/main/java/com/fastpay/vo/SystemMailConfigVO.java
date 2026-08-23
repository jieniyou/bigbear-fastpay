package com.fastpay.vo;

import lombok.Data;

/**
 * 邮件配置 VO
 * 返回管理后台展示所需配置，敏感密码不回显。
 *
 * @author FastPay
 */
@Data
public class SystemMailConfigVO {

    /**
     * 是否启用邮件服务
     */
    private Boolean mailEnabled;

    /**
     * SMTP 服务器地址
     */
    private String smtpHost;

    /**
     * SMTP 服务器端口
     */
    private Integer smtpPort;

    /**
     * SMTP 登录账号
     */
    private String smtpUsername;

    /**
     * SMTP 密码是否已配置
     */
    private Boolean passwordConfigured;

    /**
     * 发件邮箱
     */
    private String fromEmail;

    /**
     * 发件名称
     */
    private String fromName;

    /**
     * 是否启用 SSL
     */
    private Boolean sslEnabled;

    /**
     * 平台外部访问地址
     */
    private String publicBaseUrl;

    /**
     * 是否启用普通订单通知
     */
    private Boolean orderNotifyEnabled;

    /**
     * 订单通知邮件主题
     */
    private String orderNotifySubject;

    /**
     * 订单通知 HTML 模板
     */
    private String orderNotifyTemplate;

    /**
     * 是否启用订单确认通知
     */
    private Boolean orderConfirmNotifyEnabled;

    /**
     * 订单确认通知邮件主题
     */
    private String orderConfirmNotifySubject;

    /**
     * 订单确认通知 HTML 模板
     */
    private String orderConfirmNotifyTemplate;

    /**
     * 是否启用订单关闭通知
     */
    private Boolean orderCloseNotifyEnabled;

    /**
     * 订单关闭通知邮件主题
     */
    private String orderCloseNotifySubject;

    /**
     * 订单关闭通知 HTML 模板
     */
    private String orderCloseNotifyTemplate;

    /**
     * 邮件操作链接有效期，单位：分钟
     */
    private Integer actionTokenExpireMinutes;
}
