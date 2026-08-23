package com.fastpay.controller;

import com.fastpay.common.Result;
import com.fastpay.dto.MailTestDTO;
import com.fastpay.dto.SystemBrandConfigDTO;
import com.fastpay.dto.SystemMailConfigDTO;
import com.fastpay.service.OrderMailService;
import com.fastpay.service.SystemConfigService;
import com.fastpay.vo.SystemBrandConfigVO;
import com.fastpay.vo.SystemMailConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置控制器
 * 提供公开品牌配置读取和管理后台品牌配置维护能力。
 *
 * @author FastPay
 */
@RestController
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final OrderMailService orderMailService;

    public SystemConfigController(SystemConfigService systemConfigService, OrderMailService orderMailService) {
        this.systemConfigService = systemConfigService;
        this.orderMailService = orderMailService;
    }

    /**
     * 获取公开品牌配置。
     *
     * @return 网站品牌配置
     */
    @GetMapping("/api/system/brand")
    public Result<SystemBrandConfigVO> getPublicBrandConfig() {
        return Result.success(systemConfigService.getBrandConfig());
    }

    /**
     * 获取管理后台品牌配置。
     *
     * @return 网站品牌配置
     */
    @GetMapping("/api/admin/system-config/brand")
    public Result<SystemBrandConfigVO> getAdminBrandConfig() {
        return Result.success(systemConfigService.getBrandConfig());
    }

    /**
     * 更新管理后台品牌配置。
     *
     * @param dto 网站品牌配置
     * @return 更新后的网站品牌配置
     */
    @PutMapping("/api/admin/system-config/brand")
    public Result<SystemBrandConfigVO> updateAdminBrandConfig(@Valid @RequestBody SystemBrandConfigDTO dto) {
        return Result.success("保存成功", systemConfigService.updateBrandConfig(dto));
    }

    /**
     * 获取管理后台邮件配置。
     *
     * @return 邮件配置
     */
    @GetMapping("/api/admin/system-config/mail")
    public Result<SystemMailConfigVO> getAdminMailConfig() {
        return Result.success(systemConfigService.getMailConfig());
    }

    /**
     * 更新管理后台邮件配置。
     *
     * @param dto 邮件配置
     * @return 更新后的邮件配置
     */
    @PutMapping("/api/admin/system-config/mail")
    public Result<SystemMailConfigVO> updateAdminMailConfig(@Valid @RequestBody SystemMailConfigDTO dto) {
        return Result.success("保存成功", systemConfigService.updateMailConfig(dto));
    }

    /**
     * 发送测试邮件。
     *
     * @param dto 测试邮件参数
     * @return 发送结果
     */
    @PostMapping("/api/admin/system-config/mail/test")
    public Result<Void> sendTestMail(@Valid @RequestBody MailTestDTO dto) {
        orderMailService.sendTestMail(dto.getTestEmail());
        return Result.success("测试邮件已发送", null);
    }
}
