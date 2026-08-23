package com.fastpay.config;

import com.fastpay.service.AdminService;
import com.fastpay.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 系统初始化配置
 * 在应用启动时执行初始化操作
 *
 * @author FastPay
 */
@Slf4j
@Component
public class InitConfig implements CommandLineRunner {

    private final AdminService adminService;
    private final SystemConfigService systemConfigService;

    public InitConfig(AdminService adminService, SystemConfigService systemConfigService) {
        this.adminService = adminService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void run(String... args) {
        // 初始化系统配置表和默认品牌配置
        try {
            systemConfigService.initDefaultConfig();
        } catch (Exception e) {
            log.warn("初始化系统配置失败（可能数据库未就绪）: {}", e.getMessage());
        }

        // 初始化默认管理员账号
        try {
            adminService.initDefaultAdmin();
        } catch (Exception e) {
            log.warn("初始化管理员账号失败（可能数据库未就绪）: {}", e.getMessage());
        }
    }
}
