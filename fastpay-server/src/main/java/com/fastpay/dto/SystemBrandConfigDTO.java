package com.fastpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统品牌配置 DTO
 * 用于管理员维护网站名称和署名信息。
 *
 * @author FastPay
 */
@Data
public class SystemBrandConfigDTO {

    /**
     * 网站名称
     */
    @NotBlank(message = "网站名称不能为空")
    private String siteName;

    /**
     * 网站署名
     */
    @NotBlank(message = "网站署名不能为空")
    private String siteAuthor;
}
