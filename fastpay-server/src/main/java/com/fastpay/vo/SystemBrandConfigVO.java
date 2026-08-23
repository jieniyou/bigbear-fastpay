package com.fastpay.vo;

import lombok.Data;

/**
 * 系统品牌配置 VO
 * 用于前端展示网站名称和署名信息。
 *
 * @author FastPay
 */
@Data
public class SystemBrandConfigVO {

    /**
     * 网站名称
     */
    private String siteName;

    /**
     * 网站署名
     */
    private String siteAuthor;

    /**
     * 署名展示文本
     */
    private String authorText;
}
