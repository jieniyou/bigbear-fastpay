package com.fastpay.service;

import com.fastpay.common.BusinessException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 邮件操作 Token 服务
 * 使用 HMAC 签名和数据库消费记录实现短时效、一次性订单操作链接。
 *
 * @author FastPay
 */
@Service
public class MailActionTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CLOSE = "close";

    private final JdbcTemplate jdbcTemplate;

    @Value("${fastpay.jwt.secret}")
    private String secret;

    public MailActionTokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成并登记订单操作 Token。
     *
     * @param merchantId    商户ID
     * @param orderNo       平台订单号
     * @param action        操作类型
     * @param expireMinutes 有效期分钟数
     * @return 签名 Token
     */
    @Transactional(rollbackFor = Exception.class)
    public String generateOrderActionToken(Long merchantId, String orderNo, String action, int expireMinutes) {
        if (merchantId == null || !StringUtils.hasText(orderNo) || !isAllowedAction(action)) {
            throw BusinessException.badRequest("邮件操作参数不完整");
        }
        createActionTokenTableIfNeeded();

        long expireAt = Instant.now().plusSeconds(Math.max(1, expireMinutes) * 60L).getEpochSecond();
        String payload = merchantId + "\n" + orderNo.trim() + "\n" + action + "\n" + expireAt + "\n" + UUID.randomUUID();
        String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
        String token = encodedPayload + "." + sign(encodedPayload);
        jdbcTemplate.update("INSERT INTO fp_mail_action_token "
                        + "(token_hash, merchant_id, order_no, action, expire_time) VALUES (?, ?, ?, ?, ?)",
                hashToken(token),
                merchantId,
                orderNo.trim(),
                action,
                LocalDateTime.ofInstant(Instant.ofEpochSecond(expireAt), ZoneId.systemDefault()));
        return token;
    }

    /**
     * 校验并消费订单操作 Token，消费后同一订单其它操作链接也会失效。
     *
     * @param token 邮件操作 Token
     * @return 操作 Token 载荷
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderActionPayload consumeOrderActionToken(String token) {
        OrderActionPayload payload = verifyOrderActionToken(token);
        createActionTokenTableIfNeeded();

        String tokenHash = hashToken(token);
        List<TokenState> states = jdbcTemplate.query(
                "SELECT used_time, expire_time FROM fp_mail_action_token WHERE token_hash = ? LIMIT 1",
                (rs, rowNum) -> {
                    TokenState state = new TokenState();
                    state.setUsedTime(rs.getTimestamp("used_time") == null
                            ? null
                            : rs.getTimestamp("used_time").toLocalDateTime());
                    state.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
                    return state;
                },
                tokenHash
        );
        if (states.isEmpty()) {
            throw BusinessException.badRequest("邮件操作链接未登记或已失效");
        }
        TokenState state = states.get(0);
        if (state.getUsedTime() != null) {
            throw BusinessException.badRequest("邮件操作链接已使用");
        }
        if (LocalDateTime.now().isAfter(state.getExpireTime())) {
            throw BusinessException.badRequest("邮件操作链接已过期");
        }

        int updated = jdbcTemplate.update(
                "UPDATE fp_mail_action_token SET used_time = NOW() "
                        + "WHERE token_hash = ? AND used_time IS NULL AND expire_time > NOW()",
                tokenHash
        );
        if (updated != 1) {
            throw BusinessException.badRequest("邮件操作链接已使用或已过期");
        }

        jdbcTemplate.update(
                "UPDATE fp_mail_action_token SET used_time = NOW() "
                        + "WHERE merchant_id = ? AND order_no = ? AND used_time IS NULL",
                payload.getMerchantId(),
                payload.getOrderNo()
        );
        return payload;
    }

    /**
     * 校验并解析订单操作 Token。
     *
     * @param token 邮件操作 Token
     * @return 操作 Token 载荷
     */
    public OrderActionPayload verifyOrderActionToken(String token) {
        if (!StringUtils.hasText(token) || !token.contains(".")) {
            throw BusinessException.badRequest("邮件操作链接无效");
        }

        String[] parts = token.split("\\.", 2);
        String expectedSignature = sign(parts[0]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw BusinessException.badRequest("邮件操作链接签名无效");
        }

        String payloadText = new String(base64UrlDecode(parts[0]), StandardCharsets.UTF_8);
        String[] values = payloadText.split("\\n", -1);
        if (values.length < 4) {
            throw BusinessException.badRequest("邮件操作链接格式错误");
        }

        Long merchantId = parseMerchantId(values[0]);
        String orderNo = values[1];
        String action = values[2];
        long expireAt = parseExpireAt(values[3]);
        if (!isAllowedAction(action)) {
            throw BusinessException.badRequest("邮件操作类型无效");
        }
        if (Instant.now().getEpochSecond() > expireAt) {
            throw BusinessException.badRequest("邮件操作链接已过期");
        }

        OrderActionPayload payload = new OrderActionPayload();
        payload.setMerchantId(merchantId);
        payload.setOrderNo(orderNo);
        payload.setAction(action);
        payload.setExpireAt(expireAt);
        return payload;
    }

    /**
     * 判断是否为确认操作。
     *
     * @param action 操作类型
     * @return 是否确认操作
     */
    public boolean isConfirmAction(String action) {
        return ACTION_CONFIRM.equals(action);
    }

    /**
     * 判断是否为关闭操作。
     *
     * @param action 操作类型
     * @return 是否关闭操作
     */
    public boolean isCloseAction(String action) {
        return ACTION_CLOSE.equals(action);
    }

    /**
     * 创建邮件操作 Token 记录表。
     */
    private void createActionTokenTableIfNeeded() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS fp_mail_action_token ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',"
                + "token_hash VARCHAR(64) NOT NULL COMMENT 'Token SHA-256摘要',"
                + "merchant_id BIGINT NOT NULL COMMENT '商户ID',"
                + "order_no VARCHAR(32) NOT NULL COMMENT '平台订单号',"
                + "action VARCHAR(20) NOT NULL COMMENT '操作类型：confirm/close',"
                + "expire_time DATETIME NOT NULL COMMENT '过期时间',"
                + "used_time DATETIME DEFAULT NULL COMMENT '使用时间',"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_token_hash (token_hash),"
                + "KEY idx_order_action (merchant_id, order_no, used_time)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件订单操作Token表'");
    }

    /**
     * 计算 Token 的 SHA-256 摘要。
     *
     * @param token 原始 Token
     * @return 十六进制摘要
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new BusinessException("生成邮件操作摘要失败");
        }
    }

    /**
     * 计算 HMAC 签名。
     *
     * @param value 待签名内容
     * @return URL 安全签名
     */
    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("生成邮件操作签名失败");
        }
    }

    /**
     * 判断操作类型是否允许。
     *
     * @param action 操作类型
     * @return 是否允许
     */
    private boolean isAllowedAction(String action) {
        return ACTION_CONFIRM.equals(action) || ACTION_CLOSE.equals(action);
    }

    /**
     * 解析商户ID。
     *
     * @param value 商户ID文本
     * @return 商户ID
     */
    private Long parseMerchantId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("邮件操作商户信息无效");
        }
    }

    /**
     * 解析过期时间。
     *
     * @param value 过期时间文本
     * @return 过期时间秒级时间戳
     */
    private long parseExpireAt(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("邮件操作过期时间无效");
        }
    }

    /**
     * Base64 URL 安全编码。
     *
     * @param bytes 原始字节
     * @return 编码文本
     */
    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Base64 URL 安全解码。
     *
     * @param value 编码文本
     * @return 原始字节
     */
    private byte[] base64UrlDecode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("邮件操作链接编码无效");
        }
    }

    /**
     * 邮件订单操作载荷。
     */
    @Data
    public static class OrderActionPayload {

        /**
         * 商户ID
         */
        private Long merchantId;

        /**
         * 平台订单号
         */
        private String orderNo;

        /**
         * 操作类型
         */
        private String action;

        /**
         * 过期时间秒级时间戳
         */
        private Long expireAt;
    }

    /**
     * Token 使用状态。
     */
    @Data
    private static class TokenState {

        /**
         * 使用时间
         */
        private LocalDateTime usedTime;

        /**
         * 过期时间
         */
        private LocalDateTime expireTime;
    }
}
