package com.fastpay.service.impl;

import com.fastpay.common.BusinessException;
import com.fastpay.common.Constants;
import com.fastpay.entity.Merchant;
import com.fastpay.entity.PayOrder;
import com.fastpay.entity.Shop;
import com.fastpay.mapper.MerchantMapper;
import com.fastpay.mapper.ShopMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 订单邮件服务实现类
 * 动态读取后台邮件配置和 HTML 模板，订单通知失败不影响主支付流程。
 *
 * @author FastPay
 */
@Slf4j
@Service
public class OrderMailServiceImpl implements OrderMailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ACTION_BUTTONS_PLACEHOLDER = "{{action_buttons}}";
    private static final String CONFIRM_BUTTON_PLACEHOLDER = "{{confirm_button}}";
    private static final String CLOSE_BUTTON_PLACEHOLDER = "{{close_button}}";

    private final SystemConfigService systemConfigService;
    private final MailActionTokenService mailActionTokenService;
    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final Executor payNotifyExecutor;

    @Value("${fastpay.pay.page-domain:}")
    private String pageDomain;

    @Value("${server.servlet.context-path:/fastpay-server}")
    private String serverContextPath;

    public OrderMailServiceImpl(SystemConfigService systemConfigService,
                                MailActionTokenService mailActionTokenService,
                                MerchantMapper merchantMapper,
                                ShopMapper shopMapper,
                                @Qualifier("payNotifyExecutor") Executor payNotifyExecutor) {
        this.systemConfigService = systemConfigService;
        this.mailActionTokenService = mailActionTokenService;
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.payNotifyExecutor = payNotifyExecutor;
    }

    /**
     * 异步发送订单创建通知。
     *
     * @param order         支付订单
     * @param merchant      商户信息
     * @param shop          店铺信息
     * @param requestOrigin 下单请求公网 Origin
     */
    @Override
    public void sendOrderCreatedNotice(PayOrder order, Merchant merchant, Shop shop, String requestOrigin) {
        payNotifyExecutor.execute(() -> {
            try {
                doSendOrderNotice(order, merchant, shop, requestOrigin);
            } catch (Exception e) {
                String orderNo = order == null ? "-" : order.getOrderNo();
                log.warn("发送订单邮件通知失败: orderNo={}, error={}", orderNo, e.getMessage());
            }
        });
    }

    /**
     * 异步发送订单确认通知。
     *
     * @param order 支付订单
     */
    @Override
    public void sendOrderConfirmedNotice(PayOrder order) {
        sendOrderConfirmedNotice(order, null);
    }

    /**
     * 异步发送订单确认通知。
     *
     * @param order         支付订单
     * @param requestOrigin 当前请求公网 Origin
     */
    @Override
    public void sendOrderConfirmedNotice(PayOrder order, String requestOrigin) {
        sendOrderResultNotice(order, true, requestOrigin);
    }

    /**
     * 异步发送订单关闭通知。
     *
     * @param order 支付订单
     */
    @Override
    public void sendOrderClosedNotice(PayOrder order) {
        sendOrderClosedNotice(order, null);
    }

    /**
     * 异步发送订单关闭通知。
     *
     * @param order         支付订单
     * @param requestOrigin 当前请求公网 Origin
     */
    @Override
    public void sendOrderClosedNotice(PayOrder order, String requestOrigin) {
        sendOrderResultNotice(order, false, requestOrigin);
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
     * 执行普通订单通知发送。
     *
     * @param order         支付订单
     * @param merchant      商户信息
     * @param shop          店铺信息
     * @param requestOrigin 请求 Origin
     */
    private void doSendOrderNotice(PayOrder order, Merchant merchant, Shop shop, String requestOrigin) {
        SystemMailConfigVO config = systemConfigService.getMailConfig();
        if (!canSendOrderMail(config, merchant) || !Boolean.TRUE.equals(config.getOrderNotifyEnabled())) {
            return;
        }
        String template = config.getOrderNotifyTemplate();
        Map<String, String> plainValues = buildPlainValues(config, order, merchant, shop, requestOrigin);
        ActionLinks actionLinks = buildActionLinksIfNeeded(config, order, template, requestOrigin);
        Map<String, String> htmlValues = buildHtmlValues(plainValues, actionLinks);
        String subject = renderPlainText(config.getOrderNotifySubject(), plainValues);
        String html = renderHtmlTemplate(template, htmlValues);
        sendHtmlMail(config, merchant.getContactEmail(), subject, html);
    }

    /**
     * 异步发送订单结果通知。
     *
     * @param order     支付订单
     * @param confirmed 是否确认通知
     */
    private void sendOrderResultNotice(PayOrder order, boolean confirmed, String requestOrigin) {
        payNotifyExecutor.execute(() -> {
            try {
                doSendOrderResultNotice(order, confirmed, requestOrigin);
            } catch (Exception e) {
                String orderNo = order == null ? "-" : order.getOrderNo();
                log.warn("发送订单结果邮件失败: orderNo={}, confirmed={}, error={}", orderNo, confirmed, e.getMessage());
            }
        });
    }

    /**
     * 执行订单结果通知发送。
     *
     * @param order     支付订单
     * @param confirmed 是否确认通知
     */
    private void doSendOrderResultNotice(PayOrder order, boolean confirmed, String requestOrigin) {
        if (order == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        Shop shop = order.getShopId() == null ? null : shopMapper.selectById(order.getShopId());
        SystemMailConfigVO config = systemConfigService.getMailConfig();
        if (!canSendOrderMail(config, merchant)) {
            return;
        }
        if (confirmed && !Boolean.TRUE.equals(config.getOrderConfirmNotifyEnabled())) {
            return;
        }
        if (!confirmed && !Boolean.TRUE.equals(config.getOrderCloseNotifyEnabled())) {
            return;
        }

        String subjectTemplate = confirmed ? config.getOrderConfirmNotifySubject() : config.getOrderCloseNotifySubject();
        String htmlTemplate = confirmed ? config.getOrderConfirmNotifyTemplate() : config.getOrderCloseNotifyTemplate();
        Map<String, String> plainValues = buildPlainValues(config, order, merchant, shop, requestOrigin);
        plainValues.put("operation_name", confirmed ? "确认收款" : "关闭订单");
        plainValues.put("operation_time", formatTime(LocalDateTime.now()));
        Map<String, String> htmlValues = buildHtmlValues(plainValues, ActionLinks.empty());
        String subject = renderPlainText(subjectTemplate, plainValues);
        String html = renderHtmlTemplate(htmlTemplate, htmlValues);
        sendHtmlMail(config, merchant.getContactEmail(), subject, html);
    }

    /**
     * 判断是否允许发送订单邮件。
     *
     * @param config   邮件配置
     * @param merchant 商户信息
     * @return 是否允许发送
     */
    private boolean canSendOrderMail(SystemMailConfigVO config, Merchant merchant) {
        if (config == null || !Boolean.TRUE.equals(config.getMailEnabled())) {
            return false;
        }
        if (merchant == null || !StringUtils.hasText(merchant.getContactEmail())) {
            return false;
        }
        if (!isMailConfigReady(config)) {
            log.warn("邮件服务未完整配置，跳过订单邮件通知");
            return false;
        }
        return true;
    }

    /**
     * 构建普通文本占位符。
     *
     * @param config        邮件配置
     * @param order         支付订单
     * @param merchant      商户信息
     * @param shop          店铺信息
     * @param requestOrigin 请求 Origin
     * @return 占位符值
     */
    private Map<String, String> buildPlainValues(SystemMailConfigVO config, PayOrder order, Merchant merchant, Shop shop, String requestOrigin) {
        SystemBrandConfigVO brandConfig = systemConfigService.getBrandConfig();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("site_name", brandConfig.getSiteName());
        values.put("site_author", brandConfig.getSiteAuthor());
        values.put("author_text", brandConfig.getAuthorText());
        values.put("merchant_name", value(merchant.getMerchantName()));
        values.put("merchant_no", value(merchant.getMerchantNo()));
        values.put("shop_name", buildShopName(order, shop));
        values.put("shop_no", shop != null ? value(shop.getShopNo()) : value(order.getShopNo()));
        values.put("order_no", value(order.getOrderNo()));
        values.put("out_trade_no", value(order.getOutTradeNo()));
        values.put("subject", value(order.getSubject()));
        values.put("amount", formatAmount(order.getAmount()));
        values.put("pay_amount", formatAmount(order.getPayAmount() != null ? order.getPayAmount() : order.getAmount()));
        values.put("pay_type", value(order.getPayType()));
        values.put("pay_type_text", formatPayType(order.getPayType()));
        values.put("pay_method", value(order.getPayMethod()));
        values.put("order_status", formatStatus(order.getStatus()));
        values.put("notify_status", String.valueOf(order.getNotifyStatus() == null ? 0 : order.getNotifyStatus()));
        values.put("create_time", formatTime(order.getCreateTime()));
        values.put("expire_time", formatTime(order.getExpireTime()));
        values.put("pay_time", formatTime(order.getPayTime()));
        values.put("client_ip", value(order.getClientIp()));
        values.put("notify_url", value(order.getNotifyUrl()));
        values.put("return_url", value(order.getReturnUrl()));
        values.put("order_url", buildMerchantOrderUrl(config, order.getOrderNo(), requestOrigin));
        return values;
    }

    /**
     * 构建 HTML 占位符。
     *
     * @param plainValues 文本占位符
     * @param actionLinks 操作链接
     * @return HTML占位符
     */
    private Map<String, String> buildHtmlValues(Map<String, String> plainValues, ActionLinks actionLinks) {
        Map<String, String> htmlValues = new LinkedHashMap<>();
        plainValues.forEach((key, value) -> htmlValues.put(key, escape(value)));
        htmlValues.put("confirm_button", actionLinks.confirmButton);
        htmlValues.put("close_button", actionLinks.closeButton);
        htmlValues.put("action_buttons", actionLinks.actionButtons);
        return htmlValues;
    }

    /**
     * 按模板需要创建操作链接。
     *
     * @param config        邮件配置
     * @param order         支付订单
     * @param template      HTML模板
     * @param requestOrigin 请求 Origin
     * @return 操作链接HTML
     */
    private ActionLinks buildActionLinksIfNeeded(SystemMailConfigVO config, PayOrder order, String template, String requestOrigin) {
        if (!templateNeedsActions(template)) {
            return ActionLinks.empty();
        }
        String confirmUrl = buildActionUrl(config, order, "confirm", requestOrigin);
        String closeUrl = buildActionUrl(config, order, "close", requestOrigin);
        String confirmButton = "<a href=\"" + escape(confirmUrl) + "\" style=\"display:inline-block;margin-right:10px;padding:11px 18px;border-radius:6px;background:#3b82f6;color:#fff;text-decoration:none;font-weight:700;\">确认收款</a>";
        String closeButton = "<a href=\"" + escape(closeUrl) + "\" style=\"display:inline-block;padding:11px 18px;border-radius:6px;background:#d95050;color:#fff;text-decoration:none;font-weight:700;\">关闭订单</a>";
        return new ActionLinks(confirmButton, closeButton, confirmButton + closeButton);
    }

    /**
     * 判断模板是否包含操作按钮占位符。
     *
     * @param template HTML模板
     * @return 是否需要操作按钮
     */
    private boolean templateNeedsActions(String template) {
        return StringUtils.hasText(template)
                && (template.contains(ACTION_BUTTONS_PLACEHOLDER)
                || template.contains(CONFIRM_BUTTON_PLACEHOLDER)
                || template.contains(CLOSE_BUTTON_PLACEHOLDER));
    }

    /**
     * 渲染 HTML 模板。
     *
     * @param template HTML模板
     * @param values   占位符
     * @return 渲染结果
     */
    private String renderHtmlTemplate(String template, Map<String, String> values) {
        String result = template == null ? "" : template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    /**
     * 渲染邮件主题。
     *
     * @param template 主题模板
     * @param values   占位符
     * @return 主题文本
     */
    private String renderPlainText(String template, Map<String, String> values) {
        String result = template == null ? "" : template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
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
     * @param config        邮件配置
     * @param orderNo       平台订单号
     * @param requestOrigin 请求 Origin
     * @return 订单列表链接
     */
    private String buildMerchantOrderUrl(SystemMailConfigVO config, String orderNo, String requestOrigin) {
        String base = normalizeMerchantBaseUrl(config, requestOrigin);
        return base + "/console/order?orderNo=" + urlEncode(orderNo);
    }

    /**
     * 构建订单操作链接。
     *
     * @param config        邮件配置
     * @param order         支付订单
     * @param action        操作类型
     * @param requestOrigin 请求 Origin
     * @return 操作链接
     */
    private String buildActionUrl(SystemMailConfigVO config, PayOrder order, String action, String requestOrigin) {
        String token = mailActionTokenService.generateOrderActionToken(
                order.getMerchantId(),
                order.getOrderNo(),
                action,
                config.getActionTokenExpireMinutes()
        );
        return normalizeServerBaseUrl(config, requestOrigin) + "/api/mail/order-action?token=" + urlEncode(token);
    }

    /**
     * 归一化商户端基础地址。
     *
     * @param config        邮件配置
     * @param requestOrigin 请求 Origin
     * @return 商户端基础地址
     */
    private String normalizeMerchantBaseUrl(SystemMailConfigVO config, String requestOrigin) {
        if (StringUtils.hasText(pageDomain)) {
            return trimTrailingSlash(pageDomain);
        }
        String origin = resolvePublicOrigin(config, requestOrigin);
        return origin + "/fastpay-merchant";
    }

    /**
     * 归一化服务端基础地址。
     *
     * @param config        邮件配置
     * @param requestOrigin 请求 Origin
     * @return 服务端基础地址
     */
    private String normalizeServerBaseUrl(SystemMailConfigVO config, String requestOrigin) {
        String origin = resolvePublicOrigin(config, requestOrigin);
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
     * @param config        邮件配置
     * @param requestOrigin 请求 Origin
     * @return 公网 Origin
     */
    private String resolvePublicOrigin(SystemMailConfigVO config, String requestOrigin) {
        if (StringUtils.hasText(config.getPublicBaseUrl())) {
            return stripKnownAppPath(trimTrailingSlash(config.getPublicBaseUrl()));
        }
        if (StringUtils.hasText(requestOrigin)) {
            return stripKnownAppPath(trimTrailingSlash(requestOrigin));
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
        return value(payType);
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
     * 空值兜底。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private String value(String value) {
        return StringUtils.hasText(value) ? value : "-";
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

    /**
     * 邮件操作按钮 HTML。
     */
    private record ActionLinks(String confirmButton, String closeButton, String actionButtons) {
        /**
         * 空按钮组。
         *
         * @return 空按钮组
         */
        private static ActionLinks empty() {
            return new ActionLinks("", "", "");
        }
    }
}
