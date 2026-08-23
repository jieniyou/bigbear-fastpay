package com.fastpay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 测试邮件 DTO
 * 用于管理后台发送 SMTP 连通性测试邮件。
 *
 * @author FastPay
 */
@Data
public class MailTestDTO {

    /**
     * 测试收件邮箱
     */
    @NotBlank(message = "测试邮箱不能为空")
    @Email(message = "测试邮箱格式不正确")
    private String testEmail;
}
