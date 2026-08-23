package com.fastpay.service.impl;

import com.fastpay.common.BusinessException;
import com.fastpay.common.Constants;
import com.fastpay.entity.Merchant;
import com.fastpay.entity.PayOrder;
import com.fastpay.entity.Shop;
import com.fastpay.service.MailActionTokenService;
import com.fastpay.service.OrderMailService;
import com.fastpay.service.SystemConfigService;
import com.fastpay.vo.SystemBrandConfigVO;
import com.fastpay.vo.SystemMailConfigVO;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 订单邮件服务实现类
 * 动态读取后台邮件配置，订单通知失败不影响主支付流程。
 *
 * @author FastPay
 */
@Slf4j
@Service
public class OrderMailServiceImpl implements OrderMailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemConfigService systemConfigService;
    private final MailActionTokenService mailActionTokenService;
    private final Executor payNotifyExecutor;

    @Value("${fastpay.pay.page-domain:}")
    private String pageDomain;

    @Value("${server.servlet.context-path:/fastpay-server}")
    private String serverContextPath;

    public OrderMailServiceImpl(SystemConfigService systemConfigService,
                                MailActionTokenService mailActionTokenService,
                                @Qualifier("payNotifyExecutor") Executor payNotifyExecutor) {
        this.systemConfigService = systemConfigService;
        this.mailActionTokenService = mailActionTokenService;
        this.payNotifyExecutor = payNotifyExecutor;
    }

    /**
     * 异步发送订单创建通知。
     *
     * @param order    支付订单
     * @param merchant 商户信息
     * @param shop     店铺信息
     */
    @Override
    public void sendOrderCreatedNotice(PayOrder order, Merchant merchant, Shop shop) {
        payNotifyExecutor.execute(() -> {
            try {
                doSendOrderCreatedNotice(order, merchant, shop);
            } catch (Exception e) {
                String orderNo = order == null ? "-" : order.getOrderNo();
                log.warn("发送订单邮件通知失败: orderNo={}, error={}", orderNo, e.getMessage());
            }
        });
    }

    /**
     * 发送测试邮件。
     *
     * @param testEmail 测试收件邮箱
     */
    @Override
    public void sendTestMail(String testEmail) {
        SystemMailConfigVO config = systemConfigService.getMailConfig();
        validateMailConfig(config, testEmail);

        String subject = "[" + systemConfigService.getBrandConfig().getSiteName() + "] 邮件配置测试";
        String html = "<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.7;color:#303133;\">"
                + "<h2 style=\"margin:0 0 12px;\">邮件配置测试成功</h2>"
                + "<p>如果你收到这封邮件，说明当前 SMTP 配置可以正常发信。</p>"
                + "<p style=\"color:#909399;font-size:13px;\">FAST 易支付系统邮件测试</p>"
                + "</div>";
        sendHtmlMail(config, testEmail, subject, html);
    }

    /**
     * 执行订单邮件通知发送。
     *
     * @param order    支付订单
     * @param merchant 商户信息
     * @param shop     店铺信息
     */
    private void doSendOrderCreatedNotice(PayOrder order, Merchant merchant, Shop shop) {
        if (order == null || merchant == null) {
            return;
        }

        SystemMailConfigVO config = systemConfigService.getMailConfig();
        if (!Boolean.TRUE.equals(config.getMailEnabled())) {
            return;
        }
        if (!Boolean.TRUE.equals(config.getOrderNotifyEnabled())
                && !Boolean.TRUE.equals(config.getOrderActionNotifyEnabled())) {
            return;
        }
        if (!StringUtils.hasText(merchant.getContactEmail())) {
            log.warn("商户未配置联系邮箱，跳过订单邮件通知: merchantId={}, orderNo={}", merchant.getId(), order.getOrderNo());
            return;
        }
        if (!isMailConfigReady(config)) {
            log.warn("邮件服务未完整配置，跳过订单邮件通知: orderNo={}", order.getOrderNo());
            return;
        }

        SystemBrandConfigVO brandConfig = systemConfigService.getBrandConfig();
        boolean withActions = Boolean.TRUE.equals(config.getOrderActionNotifyEnabled());
        String subject = "[" + brandConfig.getSiteName() + "] 新订单通知"
                + (withActions ? "（可直接操作）" : "");
        String html = buildOrderNoticeHtml(config, brandConfig, order, merchant, shop, withActions);
        sendHtmlMail(config, merchant.getContactEmail(), subject, html);
    }

    /**
     * 构建订单通知邮件正文。
     *
     * @param config      邮件配置
     * @param brandConfig 品牌配置
     * @param order       支付订单
     * @param merchant    商户信息
     * @param shop        店铺信息
     * @param withActions 是否带操作按钮
     * @return HTML 邮件正文
     */
    private String buildOrderNoticeHtml(SystemMailConfigVO config, SystemBrandConfigVO brandConfig,
                                        PayOrder order, Merchant merchant, Shop shop, boolean withActions) {
        StringBuilder rows = new StringBuilder();
        appendRow(rows, "商户", merchant.getMerchantName() + "（" + merchant.getMerchantNo() + "）");
        appendRow(rows, "店铺", buildShopName(order, shop));
        appendRow(rows, "平台订单号", order.getOrderNo());
        appendRow(rows, "商户订单号", order.getOutTradeNo());
        appendRow(rows, "商品名称", order.getSubject());
        appendRow(rows, "订单金额", "¥" + formatAmount(order.getAmount()));
        appendRow(rows, "支付类型", formatPayType(order.getPayType()));
        appendRow(rows, "订单状态", formatStatus(order.getStatus()));
        appendRow(rows, "创建时间", formatTime(order.getCreateTime()));
        appendRow(rows, "过期时间", formatTime(order.getExpireTime()));
        appendRow(rows, "客户端IP", order.getClientIp());
        if (StringUtils.hasText(order.getNotifyUrl())) {
            appendRow(rows, "回调地址", order.getNotifyUrl());
        }
        if (StringUtils.hasText(order.getReturnUrl())) {
            appendRow(rows, "跳转地址", order.getReturnUrl());
        }

        String orderUrl = buildMerchantOrderUrl(config, order.getOrderNo());
        StringBuilder actions = new StringBuilder();
        actions.append("<a href=\"").append(escape(orderUrl)).append("\" ")
                .append("style=\"display:inline-block;margin-right:10px;padding:10px 16px;border-radius:4px;background:#409eff;color:#fff;text-decoration:none;\">查看订单</a>");
        if (withActions) {
            String confirmUrl = buildActionUrl(config, order, "confirm");
            String closeUrl = buildActionUrl(config, order, "close");
            actions.append("<a href=\"").append(escape(confirmUrl)).append("\" ")
                    .append("style=\"display:inline-block;margin-right:10px;padding:10px 16px;border-radius:4px;background:#67c23a;color:#fff;text-decoration:none;\">确认收款</a>");
            actions.append("<a href=\"").append(escape(closeUrl)).append("\" ")
                    .append("style=\"display:inline-block;padding:10px 16px;border-radius:4px;background:#f56c6c;color:#fff;text-decoration:none;\">关闭订单</a>");
        }

        String actionTip = withActions
                ? "确认/关闭按钮为一次业务操作入口，链接会在 " + config.getActionTokenExpireMinutes() + " 分钟后过期。"
                : "请登录商户后台核对后再处理订单。";

        return "<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.7;color:#303133;max-width:760px;\">"
                + "<h2 style=\"margin:0 0 8px;\">" + escape(brandConfig.getSiteName()) + " 新订单通知</h2>"
                + "<p style=\"margin:0 0 16px;color:#606266;\">收到一笔待确认订单，请核对收款记录后处理。</p>"
                + "<table style=\"border-collapse:collapse;width:100%;font-size:14px;\">" + rows + "</table>"
                + "<div style=\"margin:22px 0;\">" + actions + "</div>"
                + "<p style=\"margin:0;color:#909399;font-size:13px;\">" + escape(actionTip) + "</p>"
                + "<p style=\"margin:12px 0 0;color:#909399;font-size:13px;\">"
                + escape(brandConfig.getAuthorText()) + "</p>"
                + "</div>";
    }

    /**
     * 发送 HTML 邮件。
     *
     * @param config  邮件配置
     * @param to      收件人
     * @param subject 邮件标题
     * @param html    HTML 正文
     */
    private void sendHtmlMail(SystemMailConfigVO config, String to, String subject, String html) {
        try {
            JavaMailSenderImpl sender = buildMailSender(config);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(config.getFromEmail(), config.getFromName(), StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("订单邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            throw new BusinessException("邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 创建动态 JavaMailSender。
     *
     * @param config 邮件配置
     * @return JavaMailSender
     */
    private JavaMailSenderImpl buildMailSender(SystemMailConfigVO config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (StringUtils.hasText(config.getSmtpUsername())) {
            sender.setUsername(config.getSmtpUsername());
            sender.setPassword(systemConfigService.getMailSmtpPassword());
        }

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(config.getSmtpUsername())));
        properties.put("mail.smtp.ssl.enable", String.valueOf(Boolean.TRUE.equals(config.getSslEnabled())));
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    /**
     * 校验测试邮件配置。
     *
     * @param config    邮件配置
     * @param testEmail 测试邮箱
     */
    private void validateMailConfig(SystemMailConfigVO config, String testEmail) {
        if (!Boolean.TRUE.equals(config.getMailEnabled())) {
            throw BusinessException.badRequest("请先启用邮件服务");
        }
        if (!StringUtils.hasText(testEmail)) {
            throw BusinessException.badRequest("测试邮箱不能为空");
        }
        if (!isMailConfigReady(config)) {
            throw BusinessException.badRequest("请先完善SMTP服务器、端口和发件邮箱");
        }
    }

    /**
     * 判断邮件配置是否足够发信。
     *
     * @param config 邮件配置
     * @return 是否可发信
     */
    private boolean isMailConfigReady(SystemMailConfigVO config) {
        return config != null
                && StringUtils.hasText(config.getSmtpHost())
                && config.getSmtpPort() != null
                && StringUtils.hasText(config.getFromEmail())
                && (!StringUtils.hasText(config.getSmtpUsername())
                || StringUtils.hasText(systemConfigService.getMailSmtpPassword()));
    }

    /**
     * 增加表格行。
     *
     * @param rows  表格内容
     * @param label 标签
     * @param value 值
     */
    private void appendRow(StringBuilder rows, String label, String value) {
        rows.append("<tr>")
                .append("<td style=\"width:130px;padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;\">")
                .append(escape(label))
                .append("</td><td style=\"padding:9px 12px;border:1px solid #ebeef5;\">")
                .append(escape(StringUtils.hasText(value) ? value : "-"))
                .append("</td></tr>");
    }

    /**
     * 构建店铺显示名称。
     *
     * @param order 支付订单
     * @param shop  店铺信息
     * @return 店铺显示名称
     */
    private String buildShopName(PayOrder order, Shop shop) {
        if (shop != null) {
            return shop.getShopName() + "（" + shop.getShopNo() + "）";
        }
        if (StringUtils.hasText(order.getShopName())) {
            return order.getShopName();
        }
        return String.valueOf(order.getShopId());
    }

    /**
     * 构建商户端订单列表链接。
     *
     * @param config  邮件配置
     * @param orderNo 平台订单号
     * @return 订单列表链接
     */
    private String buildMerchantOrderUrl(SystemMailConfigVO config, String orderNo) {
        String base = normalizeMerchantBaseUrl(config);
        return base + "/console/order?orderNo=" + urlEncode(orderNo);
    }

    /**
     * 构建订单操作链接。
     *
     * @param config 邮件配置
     * @param order  支付订单
     * @param action 操作类型
     * @return 操作链接
     */
    private String buildActionUrl(SystemMailConfigVO config, PayOrder order, String action) {
        String token = mailActionTokenService.generateOrderActionToken(
                order.getMerchantId(),
                order.getOrderNo(),
                action,
                config.getActionTokenExpireMinutes()
        );
        return normalizeServerBaseUrl(config) + "/api/mail/order-action?token=" + urlEncode(token);
    }

    /**
     * 归一化商户端基础地址。
     *
     * @param config 邮件配置
     * @return 商户端基础地址
     */
    private String normalizeMerchantBaseUrl(SystemMailConfigVO config) {
        if (StringUtils.hasText(pageDomain)) {
            return trimTrailingSlash(pageDomain);
        }
        String origin = resolvePublicOrigin(config);
        return origin + "/fastpay-merchant";
    }

    /**
     * 归一化服务端基础地址。
     *
     * @param config 邮件配置
     * @return 服务端基础地址
     */
    private String normalizeServerBaseUrl(SystemMailConfigVO config) {
        String origin = resolvePublicOrigin(config);
        String contextPath = StringUtils.hasText(serverContextPath) ? serverContextPath : "/fastpay-server";
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (origin.endsWith(contextPath)) {
            return origin;
        }
        return origin + contextPath;
    }

    /**
     * 解析公网 Origin。
     *
     * @param config 邮件配置
     * @return 公网 Origin
     */
    private String resolvePublicOrigin(SystemMailConfigVO config) {
        if (StringUtils.hasText(config.getPublicBaseUrl())) {
            return stripKnownAppPath(trimTrailingSlash(config.getPublicBaseUrl()));
        }
        if (StringUtils.hasText(pageDomain)) {
            return stripKnownAppPath(trimTrailingSlash(pageDomain));
        }
        return "";
    }

    /**
     * 移除常见应用前缀，保留域名 Origin。
     *
     * @param value 外部地址
     * @return Origin
     */
    private String stripKnownAppPath(String value) {
        if (value.endsWith("/fastpay-merchant")) {
            return value.substring(0, value.length() - "/fastpay-merchant".length());
        }
        if (value.endsWith("/fastpay-admin")) {
            return value.substring(0, value.length() - "/fastpay-admin".length());
        }
        if (value.endsWith("/fastpay-server")) {
            return value.substring(0, value.length() - "/fastpay-server".length());
        }
        return value;
    }

    /**
     * 去掉尾部斜杠。
     *
     * @param value 原始地址
     * @return 规范地址
     */
    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 格式化金额。
     *
     * @param amount 金额
     * @return 两位小数金额
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 格式化时间。
     *
     * @param time 时间
     * @return 时间文本
     */
    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : time.format(DATE_TIME_FORMATTER);
    }

    /**
     * 格式化支付类型。
     *
     * @param payType 支付类型
     * @return 支付类型文本
     */
    private String formatPayType(String payType) {
        if (Constants.PayType.WXPAY.equals(payType)) {
            return "微信支付";
        }
        if (Constants.PayType.ALIPAY.equals(payType)) {
            return "支付宝";
        }
        return payType;
    }

    /**
     * 格式化订单状态。
     *
     * @param status 状态值
     * @return 状态文本
     */
    private String formatStatus(Integer status) {
        if (Constants.OrderStatus.UNPAID.equals(status)) {
            return "待支付";
        }
        if (Constants.OrderStatus.PAID.equals(status)) {
            return "已支付";
        }
        if (Constants.OrderStatus.EXPIRED.equals(status)) {
            return "已过期";
        }
        if (Constants.OrderStatus.CLOSED.equals(status)) {
            return "已关闭";
        }
        return "未知";
    }

    /**
     * HTML 转义。
     *
     * @param value 原始文本
     * @return 转义文本
     */
    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }

    /**
     * URL 编码。
     *
     * @param value 原始文本
     * @return 编码文本
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
