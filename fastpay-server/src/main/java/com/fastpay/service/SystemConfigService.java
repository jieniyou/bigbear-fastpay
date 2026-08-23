package com.fastpay.service;

import com.fastpay.dto.SystemBrandConfigDTO;
import com.fastpay.vo.SystemBrandConfigVO;

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
}
