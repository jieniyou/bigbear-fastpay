package com.fastpay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 邮件配置 DTO
 * 用于管理后台维护 SMTP 和订单通知事件配置。
 *
 * @author FastPay
 */
@Data
public class SystemMailConfigDTO {

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
    @Min(value = 1, message = "SMTP端口必须大于0")
    @Max(value = 65535, message = "SMTP端口不能超过65535")
    private Integer smtpPort;

    /**
     * SMTP 登录账号
     */
    private String smtpUsername;

    /**
     * SMTP 登录密码，留空表示保持原密码
     */
    private String smtpPassword;

    /**
     * 发件邮箱
     */
    @Email(message = "发件邮箱格式不正确")
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
     * 平台外部访问地址，用于生成邮件中的订单链接和操作链接
     */
    private String publicBaseUrl;

    /**
     * 是否启用普通订单通知
     */
    private Boolean orderNotifyEnabled;

    /**
     * 是否启用带操作按钮的订单通知
     */
    private Boolean orderActionNotifyEnabled;

    /**
     * 邮件操作链接有效期，单位：分钟
     */
    @Min(value = 1, message = "操作链接有效期不能小于1分钟")
    @Max(value = 1440, message = "操作链接有效期不能超过1440分钟")
    private Integer actionTokenExpireMinutes;
}
