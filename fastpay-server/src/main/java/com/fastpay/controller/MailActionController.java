package com.fastpay.controller;

import com.fastpay.common.BusinessException;
import com.fastpay.service.MailActionTokenService;
import com.fastpay.service.PayOrderService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/**
 * 邮件订单操作控制器
 * 提供邮件中确认收款和关闭订单按钮的短时效公开入口。
 *
 * @author FastPay
 */
@RestController
@RequestMapping("/api/mail")
public class MailActionController {

    private final MailActionTokenService mailActionTokenService;
    private final PayOrderService payOrderService;

    public MailActionController(MailActionTokenService mailActionTokenService, PayOrderService payOrderService) {
        this.mailActionTokenService = mailActionTokenService;
        this.payOrderService = payOrderService;
    }

    /**
     * 执行邮件订单操作。
     *
     * @param token 邮件操作 Token
     * @return HTML 操作结果页
     */
    @GetMapping(value = "/order-action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleOrderAction(@RequestParam String token) {
        try {
            MailActionTokenService.OrderActionPayload payload = mailActionTokenService.verifyOrderActionToken(token);
            if (mailActionTokenService.isConfirmAction(payload.getAction())) {
                payOrderService.confirmPay(payload.getOrderNo(), payload.getMerchantId());
                return html("订单已确认", "订单 " + payload.getOrderNo() + " 已确认收款，并已触发商户回调通知。", true);
            }
            if (mailActionTokenService.isCloseAction(payload.getAction())) {
                payOrderService.closeOrder(payload.getOrderNo(), payload.getMerchantId());
                return html("订单已关闭", "订单 " + payload.getOrderNo() + " 已关闭。", true);
            }
            return html("操作失败", "不支持的订单操作。", false);
        } catch (BusinessException e) {
            return html("操作失败", e.getMessage(), false);
        } catch (Exception e) {
            return html("操作失败", "系统暂时无法处理该邮件操作，请登录后台确认订单。", false);
        }
    }

    /**
     * 构建 HTML 响应。
     *
     * @param title   标题
     * @param message 消息
     * @param success 是否成功
     * @return HTML 响应
     */
    private ResponseEntity<String> html(String title, String message, boolean success) {
        String color = success ? "#67c23a" : "#f56c6c";
        String body = "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escape(title) + "</title></head>"
                + "<body style=\"margin:0;background:#f5f7fa;font-family:Arial,'Microsoft YaHei',sans-serif;color:#303133;\">"
                + "<main style=\"max-width:560px;margin:10vh auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:8px;text-align:center;\">"
                + "<div style=\"width:52px;height:52px;margin:0 auto 16px;border-radius:50%;background:" + color + ";color:#fff;line-height:52px;font-size:28px;\">"
                + (success ? "✓" : "!")
                + "</div><h1 style=\"font-size:22px;margin:0 0 12px;\">" + escape(title) + "</h1>"
                + "<p style=\"font-size:15px;line-height:1.8;color:#606266;margin:0;\">" + escape(message) + "</p>"
                + "</main></body></html>";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(body);
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
}
