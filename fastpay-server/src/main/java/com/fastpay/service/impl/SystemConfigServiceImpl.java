package com.fastpay.service.impl;

import com.fastpay.common.BusinessException;
import com.fastpay.dto.SystemBrandConfigDTO;
import com.fastpay.dto.SystemMailConfigDTO;
import com.fastpay.service.SystemConfigService;
import com.fastpay.vo.SystemBrandConfigVO;
import com.fastpay.vo.SystemMailConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统配置服务实现类
 * 使用轻量配置表存储网站名称、署名、邮件服务和邮件模板等全局配置。
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

    private static final String MAIL_ENABLED_KEY = "mail.enabled";
    private static final String MAIL_SMTP_HOST_KEY = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT_KEY = "mail.smtp.port";
    private static final String MAIL_SMTP_USERNAME_KEY = "mail.smtp.username";
    private static final String MAIL_SMTP_PASSWORD_KEY = "mail.smtp.password";
    private static final String MAIL_FROM_EMAIL_KEY = "mail.from.email";
    private static final String MAIL_FROM_NAME_KEY = "mail.from.name";
    private static final String MAIL_SSL_ENABLED_KEY = "mail.smtp.ssl-enabled";
    private static final String MAIL_PUBLIC_BASE_URL_KEY = "mail.public-base-url";
    private static final String MAIL_ACTION_TOKEN_EXPIRE_MINUTES_KEY = "mail.action-token-expire-minutes";

    private static final String MAIL_ORDER_NOTIFY_ENABLED_KEY = "mail.event.order-notify.enabled";
    private static final String MAIL_ORDER_NOTIFY_SUBJECT_KEY = "mail.event.order-notify.subject";
    private static final String MAIL_ORDER_NOTIFY_TEMPLATE_KEY = "mail.event.order-notify.template";
    private static final String MAIL_ORDER_CONFIRM_NOTIFY_ENABLED_KEY = "mail.event.order-confirm.enabled";
    private static final String MAIL_ORDER_CONFIRM_NOTIFY_SUBJECT_KEY = "mail.event.order-confirm.subject";
    private static final String MAIL_ORDER_CONFIRM_NOTIFY_TEMPLATE_KEY = "mail.event.order-confirm.template";
    private static final String MAIL_ORDER_CLOSE_NOTIFY_ENABLED_KEY = "mail.event.order-close.enabled";
    private static final String MAIL_ORDER_CLOSE_NOTIFY_SUBJECT_KEY = "mail.event.order-close.subject";
    private static final String MAIL_ORDER_CLOSE_NOTIFY_TEMPLATE_KEY = "mail.event.order-close.template";

    private static final int DEFAULT_SMTP_PORT = 465;
    private static final int DEFAULT_ACTION_TOKEN_EXPIRE_MINUTES = 30;
    private static final String DEFAULT_ORDER_NOTIFY_SUBJECT = "【{{site_name}}】新订单通知：{{order_no}}";
    private static final String DEFAULT_ORDER_CONFIRM_NOTIFY_SUBJECT = "【{{site_name}}】订单确认成功：{{order_no}}";
    private static final String DEFAULT_ORDER_CLOSE_NOTIFY_SUBJECT = "【{{site_name}}】订单已关闭：{{order_no}}";
    private static final String DEFAULT_ORDER_NOTIFY_TEMPLATE = buildOrderNoticeTemplate();
    private static final String DEFAULT_ORDER_CONFIRM_NOTIFY_TEMPLATE = buildOrderConfirmTemplate();
    private static final String DEFAULT_ORDER_CLOSE_NOTIFY_TEMPLATE = buildOrderCloseTemplate();

    private final JdbcTemplate jdbcTemplate;

    public SystemConfigServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 构建统一邮件外壳。
     *
     * @param title       邮件标题
     * @param contentHtml 邮件正文 HTML
     * @return 完整 HTML 模板
     */
    private static String buildMailShell(String title, String contentHtml) {
        return "<!doctype html><html><body style=\"margin:0;padding:24px;background:#f3f5fa;"
                + "font-family:Arial,'Microsoft YaHei',sans-serif;color:#252a3a;\">"
                + "<div style=\"max-width:640px;margin:0 auto;overflow:hidden;border:1px solid #e2e5ef;"
                + "border-radius:10px;background:#ffffff;box-shadow:0 12px 34px rgba(35,42,72,.10);\">"
                + "<div style=\"padding:24px 28px;color:#ffffff;background:#5968df;\">"
                + "<div style=\"font-size:13px;opacity:.82;\">{{site_name}}</div>"
                + "<h1 style=\"margin:7px 0 0;font-size:24px;line-height:1.35;\">" + title + "</h1></div>"
                + "<div style=\"padding:28px;font-size:15px;line-height:1.8;\">" + contentHtml + "</div>"
                + "<div style=\"padding:16px 28px;border-top:1px solid #eceef4;color:#9298a8;background:#fafbfc;font-size:12px;\">"
                + "此邮件由 {{site_name}} 系统自动发送，请勿直接回复。{{author_text}}</div>"
                + "</div></body></html>";
    }

    /**
     * 构建订单通知默认模板。
     *
     * @return 订单通知模板
     */
    private static String buildOrderNoticeTemplate() {
        return buildMailShell("新订单通知",
                "<p style=\"margin:0 0 18px;\">商户 <b>{{merchant_name}}</b> 的店铺 <b>{{shop_name}}</b> 收到一笔待确认订单。</p>"
                        + "<div style=\"margin:18px 0;padding:18px;border-radius:8px;color:#5968df;background:#f0f2ff;text-align:center;\">"
                        + "<div style=\"font-size:13px;color:#70778a;\">订单金额</div>"
                        + "<div style=\"font-size:32px;font-weight:700;letter-spacing:1px;\">¥{{amount}}</div></div>"
                        + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
                        + "<tr><td style=\"width:128px;padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">平台订单号</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\"><b>{{order_no}}</b></td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">商户订单号</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{out_trade_no}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">商品名称</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{subject}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">支付类型</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{pay_type_text}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">创建时间</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{create_time}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;\">过期时间</td><td style=\"padding:10px 0;\">{{expire_time}}</td></tr></table>"
                        + "<div style=\"margin:24px 0 8px;\">{{action_buttons}}</div>"
                        + "<p style=\"margin:14px 0 0;color:#9298a8;font-size:13px;\">确认/关闭按钮为短时效单次操作链接，任意一个按钮使用后同订单其它按钮会失效。</p>");
    }

    /**
     * 构建订单确认默认模板。
     *
     * @return 订单确认模板
     */
    private static String buildOrderConfirmTemplate() {
        return buildMailShell("订单确认成功",
                "<p style=\"margin:0 0 18px;\">商户 <b>{{merchant_name}}</b> 的店铺 <b>{{shop_name}}</b> 订单已确认收款。</p>"
                        + "<div style=\"margin:18px 0;padding:18px;border-radius:8px;color:#1b9468;background:#eaf8f1;text-align:center;\">"
                        + "<div style=\"font-size:13px;color:#60776c;\">确认金额</div>"
                        + "<div style=\"font-size:32px;font-weight:700;letter-spacing:1px;\">¥{{pay_amount}}</div></div>"
                        + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
                        + "<tr><td style=\"width:128px;padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">平台订单号</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\"><b>{{order_no}}</b></td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">商户订单号</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{out_trade_no}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">商品名称</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{subject}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;\">确认时间</td><td style=\"padding:10px 0;\">{{pay_time}}</td></tr></table>"
                        + "<p style=\"margin:24px 0 0;\"><a href=\"{{order_url}}\" style=\"display:inline-block;padding:11px 18px;border-radius:6px;color:#ffffff;background:#5968df;text-decoration:none;font-weight:700;\">查看订单</a></p>");
    }

    /**
     * 构建订单关闭默认模板。
     *
     * @return 订单关闭模板
     */
    private static String buildOrderCloseTemplate() {
        return buildMailShell("订单已关闭",
                "<p style=\"margin:0 0 18px;\">商户 <b>{{merchant_name}}</b> 的店铺 <b>{{shop_name}}</b> 订单已关闭。</p>"
                        + "<div style=\"margin:18px 0;padding:18px;border-radius:8px;color:#d95050;background:#fff0f0;text-align:center;\">"
                        + "<div style=\"font-size:13px;color:#8f6a6a;\">订单金额</div>"
                        + "<div style=\"font-size:32px;font-weight:700;letter-spacing:1px;\">¥{{amount}}</div></div>"
                        + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
                        + "<tr><td style=\"width:128px;padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">平台订单号</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\"><b>{{order_no}}</b></td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">商户订单号</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{out_trade_no}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;\">商品名称</td><td style=\"padding:10px 0;border-bottom:1px solid #eceef4;\">{{subject}}</td></tr>"
                        + "<tr><td style=\"padding:10px 0;color:#70778a;\">关闭时间</td><td style=\"padding:10px 0;\">{{operation_time}}</td></tr></table>"
                        + "<p style=\"margin:24px 0 0;\"><a href=\"{{order_url}}\" style=\"display:inline-block;padding:11px 18px;border-radius:6px;color:#ffffff;background:#5968df;text-decoration:none;font-weight:700;\">查看订单</a></p>");
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
        upsertDefaultConfig(MAIL_ENABLED_KEY, "false", "是否启用邮件服务");
        upsertDefaultConfig(MAIL_SMTP_HOST_KEY, "", "SMTP服务器地址");
        upsertDefaultConfig(MAIL_SMTP_PORT_KEY, String.valueOf(DEFAULT_SMTP_PORT), "SMTP服务器端口");
        upsertDefaultConfig(MAIL_SMTP_USERNAME_KEY, "", "SMTP登录账号");
        upsertDefaultConfig(MAIL_SMTP_PASSWORD_KEY, "", "SMTP登录密码");
        upsertDefaultConfig(MAIL_FROM_EMAIL_KEY, "", "发件邮箱");
        upsertDefaultConfig(MAIL_FROM_NAME_KEY, DEFAULT_SITE_NAME, "发件名称");
        upsertDefaultConfig(MAIL_SSL_ENABLED_KEY, "true", "是否启用SMTP SSL");
        upsertDefaultConfig(MAIL_PUBLIC_BASE_URL_KEY, "", "平台外部访问地址");
        upsertDefaultConfig(MAIL_ACTION_TOKEN_EXPIRE_MINUTES_KEY, String.valueOf(DEFAULT_ACTION_TOKEN_EXPIRE_MINUTES), "邮件操作链接有效期分钟数");
        upsertDefaultConfig(MAIL_ORDER_NOTIFY_ENABLED_KEY, "true", "普通订单通知事件开关");
        upsertDefaultConfig(MAIL_ORDER_NOTIFY_SUBJECT_KEY, DEFAULT_ORDER_NOTIFY_SUBJECT, "普通订单通知邮件主题");
        upsertDefaultConfig(MAIL_ORDER_NOTIFY_TEMPLATE_KEY, DEFAULT_ORDER_NOTIFY_TEMPLATE, "普通订单通知HTML模板");
        upsertDefaultConfig(MAIL_ORDER_CONFIRM_NOTIFY_ENABLED_KEY, "true", "订单确认通知事件开关");
        upsertDefaultConfig(MAIL_ORDER_CONFIRM_NOTIFY_SUBJECT_KEY, DEFAULT_ORDER_CONFIRM_NOTIFY_SUBJECT, "订单确认通知邮件主题");
        upsertDefaultConfig(MAIL_ORDER_CONFIRM_NOTIFY_TEMPLATE_KEY, DEFAULT_ORDER_CONFIRM_NOTIFY_TEMPLATE, "订单确认通知HTML模板");
        upsertDefaultConfig(MAIL_ORDER_CLOSE_NOTIFY_ENABLED_KEY, "true", "订单关闭通知事件开关");
        upsertDefaultConfig(MAIL_ORDER_CLOSE_NOTIFY_SUBJECT_KEY, DEFAULT_ORDER_CLOSE_NOTIFY_SUBJECT, "订单关闭通知邮件主题");
        upsertDefaultConfig(MAIL_ORDER_CLOSE_NOTIFY_TEMPLATE_KEY, DEFAULT_ORDER_CLOSE_NOTIFY_TEMPLATE, "订单关闭通知HTML模板");
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
     * 获取邮件配置。
     *
     * @return 邮件配置
     */
    @Override
    public SystemMailConfigVO getMailConfig() {
        createConfigTableIfNeeded();
        return buildMailConfig();
    }

    /**
     * 更新邮件配置。
     *
     * @param dto 邮件配置
     * @return 更新后的邮件配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemMailConfigVO updateMailConfig(SystemMailConfigDTO dto) {
        createConfigTableIfNeeded();
        boolean mailEnabled = Boolean.TRUE.equals(dto.getMailEnabled());
        Integer smtpPort = dto.getSmtpPort() == null ? DEFAULT_SMTP_PORT : dto.getSmtpPort();
        Integer expireMinutes = dto.getActionTokenExpireMinutes() == null
                ? DEFAULT_ACTION_TOKEN_EXPIRE_MINUTES
                : dto.getActionTokenExpireMinutes();
        String smtpHost = cleanOptionalValue(dto.getSmtpHost());
        String smtpUsername = cleanOptionalValue(dto.getSmtpUsername());
        String fromEmail = cleanOptionalValue(dto.getFromEmail());
        String fromName = cleanValue(dto.getFromName(), getBrandConfig().getSiteName());

        if (mailEnabled) {
            validateRequired(smtpHost, "启用邮件服务后必须填写SMTP服务器");
            validateRequired(fromEmail, "启用邮件服务后必须填写发件邮箱");
            if (StringUtils.hasText(smtpUsername)
                    && !StringUtils.hasText(dto.getSmtpPassword())
                    && !StringUtils.hasText(getRawConfigValue(MAIL_SMTP_PASSWORD_KEY))) {
                throw BusinessException.badRequest("启用SMTP登录账号后必须填写SMTP密码或授权码");
            }
        }

        upsertConfig(MAIL_ENABLED_KEY, String.valueOf(mailEnabled), "是否启用邮件服务");
        upsertConfig(MAIL_SMTP_HOST_KEY, smtpHost, "SMTP服务器地址");
        upsertConfig(MAIL_SMTP_PORT_KEY, String.valueOf(smtpPort), "SMTP服务器端口");
        upsertConfig(MAIL_SMTP_USERNAME_KEY, smtpUsername, "SMTP登录账号");
        if (StringUtils.hasText(dto.getSmtpPassword())) {
            upsertConfig(MAIL_SMTP_PASSWORD_KEY, dto.getSmtpPassword().trim(), "SMTP登录密码");
        }
        upsertConfig(MAIL_FROM_EMAIL_KEY, fromEmail, "发件邮箱");
        upsertConfig(MAIL_FROM_NAME_KEY, fromName, "发件名称");
        upsertConfig(MAIL_SSL_ENABLED_KEY, String.valueOf(Boolean.TRUE.equals(dto.getSslEnabled())), "是否启用SMTP SSL");
        upsertConfig(MAIL_PUBLIC_BASE_URL_KEY, cleanOptionalValue(dto.getPublicBaseUrl()), "平台外部访问地址");
        upsertConfig(MAIL_ACTION_TOKEN_EXPIRE_MINUTES_KEY, String.valueOf(expireMinutes), "邮件操作链接有效期分钟数");
        upsertConfig(MAIL_ORDER_NOTIFY_ENABLED_KEY, String.valueOf(!Boolean.FALSE.equals(dto.getOrderNotifyEnabled())), "普通订单通知事件开关");
        upsertConfig(MAIL_ORDER_NOTIFY_SUBJECT_KEY, cleanValue(dto.getOrderNotifySubject(), DEFAULT_ORDER_NOTIFY_SUBJECT), "普通订单通知邮件主题");
        upsertConfig(MAIL_ORDER_NOTIFY_TEMPLATE_KEY, cleanValue(dto.getOrderNotifyTemplate(), DEFAULT_ORDER_NOTIFY_TEMPLATE), "普通订单通知HTML模板");
        upsertConfig(MAIL_ORDER_CONFIRM_NOTIFY_ENABLED_KEY, String.valueOf(!Boolean.FALSE.equals(dto.getOrderConfirmNotifyEnabled())), "订单确认通知事件开关");
        upsertConfig(MAIL_ORDER_CONFIRM_NOTIFY_SUBJECT_KEY, cleanValue(dto.getOrderConfirmNotifySubject(), DEFAULT_ORDER_CONFIRM_NOTIFY_SUBJECT), "订单确认通知邮件主题");
        upsertConfig(MAIL_ORDER_CONFIRM_NOTIFY_TEMPLATE_KEY, cleanValue(dto.getOrderConfirmNotifyTemplate(), DEFAULT_ORDER_CONFIRM_NOTIFY_TEMPLATE), "订单确认通知HTML模板");
        upsertConfig(MAIL_ORDER_CLOSE_NOTIFY_ENABLED_KEY, String.valueOf(!Boolean.FALSE.equals(dto.getOrderCloseNotifyEnabled())), "订单关闭通知事件开关");
        upsertConfig(MAIL_ORDER_CLOSE_NOTIFY_SUBJECT_KEY, cleanValue(dto.getOrderCloseNotifySubject(), DEFAULT_ORDER_CLOSE_NOTIFY_SUBJECT), "订单关闭通知邮件主题");
        upsertConfig(MAIL_ORDER_CLOSE_NOTIFY_TEMPLATE_KEY, cleanValue(dto.getOrderCloseNotifyTemplate(), DEFAULT_ORDER_CLOSE_NOTIFY_TEMPLATE), "订单关闭通知HTML模板");
        return buildMailConfig();
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
     * 获取原始配置值，不自动替换默认值。
     *
     * @param key 配置键
     * @return 原始配置值
     */
    private String getRawConfigValue(String key) {
        List<String> values = jdbcTemplate.query(
                "SELECT config_value FROM fp_system_config WHERE config_key = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("config_value"),
                key
        );
        if (values.isEmpty() || values.get(0) == null) {
            return "";
        }
        return values.get(0).trim();
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
     * 清理可为空的配置值。
     *
     * @param value 输入值
     * @return 去除首尾空格后的值
     */
    private String cleanOptionalValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 校验必填配置。
     *
     * @param value   配置值
     * @param message 错误提示
     */
    private void validateRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
    }

    /**
     * 获取布尔配置。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 布尔配置值
     */
    private boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取整型配置。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 整型配置值
     */
    private int getIntConfig(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

    /**
     * 构建邮件配置响应。
     *
     * @return 邮件配置
     */
    private SystemMailConfigVO buildMailConfig() {
        SystemMailConfigVO vo = new SystemMailConfigVO();
        vo.setMailEnabled(getBooleanConfig(MAIL_ENABLED_KEY, false));
        vo.setSmtpHost(getConfigValue(MAIL_SMTP_HOST_KEY, ""));
        vo.setSmtpPort(getIntConfig(MAIL_SMTP_PORT_KEY, DEFAULT_SMTP_PORT));
        vo.setSmtpUsername(getConfigValue(MAIL_SMTP_USERNAME_KEY, ""));
        vo.setPasswordConfigured(StringUtils.hasText(getRawConfigValue(MAIL_SMTP_PASSWORD_KEY)));
        vo.setFromEmail(getConfigValue(MAIL_FROM_EMAIL_KEY, ""));
        vo.setFromName(getConfigValue(MAIL_FROM_NAME_KEY, getBrandConfig().getSiteName()));
        vo.setSslEnabled(getBooleanConfig(MAIL_SSL_ENABLED_KEY, true));
        vo.setPublicBaseUrl(getConfigValue(MAIL_PUBLIC_BASE_URL_KEY, ""));
        vo.setActionTokenExpireMinutes(getIntConfig(MAIL_ACTION_TOKEN_EXPIRE_MINUTES_KEY, DEFAULT_ACTION_TOKEN_EXPIRE_MINUTES));
        vo.setOrderNotifyEnabled(getBooleanConfig(MAIL_ORDER_NOTIFY_ENABLED_KEY, true));
        vo.setOrderNotifySubject(getConfigValue(MAIL_ORDER_NOTIFY_SUBJECT_KEY, DEFAULT_ORDER_NOTIFY_SUBJECT));
        vo.setOrderNotifyTemplate(getConfigValue(MAIL_ORDER_NOTIFY_TEMPLATE_KEY, DEFAULT_ORDER_NOTIFY_TEMPLATE));
        vo.setOrderConfirmNotifyEnabled(getBooleanConfig(MAIL_ORDER_CONFIRM_NOTIFY_ENABLED_KEY, true));
        vo.setOrderConfirmNotifySubject(getConfigValue(MAIL_ORDER_CONFIRM_NOTIFY_SUBJECT_KEY, DEFAULT_ORDER_CONFIRM_NOTIFY_SUBJECT));
        vo.setOrderConfirmNotifyTemplate(getConfigValue(MAIL_ORDER_CONFIRM_NOTIFY_TEMPLATE_KEY, DEFAULT_ORDER_CONFIRM_NOTIFY_TEMPLATE));
        vo.setOrderCloseNotifyEnabled(getBooleanConfig(MAIL_ORDER_CLOSE_NOTIFY_ENABLED_KEY, true));
        vo.setOrderCloseNotifySubject(getConfigValue(MAIL_ORDER_CLOSE_NOTIFY_SUBJECT_KEY, DEFAULT_ORDER_CLOSE_NOTIFY_SUBJECT));
        vo.setOrderCloseNotifyTemplate(getConfigValue(MAIL_ORDER_CLOSE_NOTIFY_TEMPLATE_KEY, DEFAULT_ORDER_CLOSE_NOTIFY_TEMPLATE));
        return vo;
    }

    /**
     * 获取邮件 SMTP 密码。
     *
     * @return SMTP 密码
     */
    @Override
    public String getMailSmtpPassword() {
        createConfigTableIfNeeded();
        return getRawConfigValue(MAIL_SMTP_PASSWORD_KEY);
    }
}
