package com.fastpay.service.impl;

import com.fastpay.dto.SystemBrandConfigDTO;
import com.fastpay.service.SystemConfigService;
import com.fastpay.vo.SystemBrandConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统配置服务实现类
 * 使用轻量配置表存储网站名称和署名等全局配置。
 *
 * @author FastPay
 */
@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String SITE_NAME_KEY = "site.name";
    private static final String SITE_AUTHOR_KEY = "site.author";
    private static final String DEFAULT_SITE_NAME = "FAST 易支付";
    private static final String DEFAULT_SITE_AUTHOR = "大熊Bigbear";

    private final JdbcTemplate jdbcTemplate;

    public SystemConfigServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 初始化系统配置表和默认配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultConfig() {
        createConfigTableIfNeeded();
        upsertDefaultConfig(SITE_NAME_KEY, DEFAULT_SITE_NAME, "网站名称");
        upsertDefaultConfig(SITE_AUTHOR_KEY, DEFAULT_SITE_AUTHOR, "网站署名");
    }

    /**
     * 获取网站品牌配置。
     *
     * @return 网站品牌配置
     */
    @Override
    public SystemBrandConfigVO getBrandConfig() {
        createConfigTableIfNeeded();

        String siteName = getConfigValue(SITE_NAME_KEY, DEFAULT_SITE_NAME);
        String siteAuthor = getConfigValue(SITE_AUTHOR_KEY, DEFAULT_SITE_AUTHOR);
        return buildBrandConfig(siteName, siteAuthor);
    }

    /**
     * 更新网站品牌配置。
     *
     * @param dto 网站品牌配置
     * @return 更新后的品牌配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemBrandConfigVO updateBrandConfig(SystemBrandConfigDTO dto) {
        createConfigTableIfNeeded();

        String siteName = cleanValue(dto.getSiteName(), DEFAULT_SITE_NAME);
        String siteAuthor = cleanValue(dto.getSiteAuthor(), DEFAULT_SITE_AUTHOR);
        upsertConfig(SITE_NAME_KEY, siteName, "网站名称");
        upsertConfig(SITE_AUTHOR_KEY, siteAuthor, "网站署名");
        return buildBrandConfig(siteName, siteAuthor);
    }

    /**
     * 创建配置表。
     */
    private void createConfigTableIfNeeded() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS fp_system_config ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',"
                + "config_key VARCHAR(100) NOT NULL COMMENT '配置键',"
                + "config_value TEXT COMMENT '配置值',"
                + "remark VARCHAR(255) DEFAULT NULL COMMENT '配置说明',"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_config_key (config_key) COMMENT '配置键唯一索引'"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表 - 存储后台可编辑的网站基础信息'");
    }

    /**
     * 写入默认配置，不覆盖已存在配置。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @param remark       配置说明
     */
    private void upsertDefaultConfig(String key, String defaultValue, String remark) {
        jdbcTemplate.update("INSERT INTO fp_system_config (config_key, config_value, remark) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE config_key = config_key", key, defaultValue, remark);
    }

    /**
     * 写入或更新配置。
     *
     * @param key    配置键
     * @param value  配置值
     * @param remark 配置说明
     */
    private void upsertConfig(String key, String value, String remark) {
        jdbcTemplate.update("INSERT INTO fp_system_config (config_key, config_value, remark) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), remark = VALUES(remark)",
                key, value, remark);
    }

    /**
     * 获取配置值。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private String getConfigValue(String key, String defaultValue) {
        List<String> values = jdbcTemplate.query(
                "SELECT config_value FROM fp_system_config WHERE config_key = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("config_value"),
                key
        );
        if (values.isEmpty() || !StringUtils.hasText(values.get(0))) {
            return defaultValue;
        }
        return values.get(0).trim();
    }

    /**
     * 清理配置值。
     *
     * @param value        输入值
     * @param defaultValue 默认值
     * @return 清理后的配置值
     */
    private String cleanValue(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * 构建品牌配置响应。
     *
     * @param siteName   网站名称
     * @param siteAuthor 网站署名
     * @return 网站品牌配置
     */
    private SystemBrandConfigVO buildBrandConfig(String siteName, String siteAuthor) {
        SystemBrandConfigVO vo = new SystemBrandConfigVO();
        vo.setSiteName(siteName);
        vo.setSiteAuthor(siteAuthor);
        vo.setAuthorText("by " + siteAuthor);
        return vo;
    }
}
