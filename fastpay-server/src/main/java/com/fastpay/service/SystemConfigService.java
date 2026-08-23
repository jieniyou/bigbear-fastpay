package com.fastpay.service;

import com.fastpay.dto.SystemBrandConfigDTO;
import com.fastpay.dto.SystemMailConfigDTO;
import com.fastpay.vo.SystemBrandConfigVO;
import com.fastpay.vo.SystemMailConfigVO;

/**
 * 系统配置服务接口
 * 负责读取和维护站点基础配置。
 *
 * @author FastPay
 */
public interface SystemConfigService {

    /**
     * 初始化系统配置表和默认配置。
     */
    void initDefaultConfig();

    /**
     * 获取网站品牌配置。
     *
     * @return 网站品牌配置
     */
    SystemBrandConfigVO getBrandConfig();

    /**
     * 更新网站品牌配置。
     *
     * @param dto 网站品牌配置
     * @return 更新后的品牌配置
     */
    SystemBrandConfigVO updateBrandConfig(SystemBrandConfigDTO dto);

    /**
     * 获取邮件配置。
     *
     * @return 邮件配置
     */
    SystemMailConfigVO getMailConfig();

    /**
     * 更新邮件配置。
     *
     * @param dto 邮件配置
     * @return 更新后的邮件配置
     */
    SystemMailConfigVO updateMailConfig(SystemMailConfigDTO dto);

    /**
     * 获取邮件 SMTP 密码，仅服务端内部发送邮件时使用。
     *
     * @return SMTP 密码
     */
    String getMailSmtpPassword();
}
