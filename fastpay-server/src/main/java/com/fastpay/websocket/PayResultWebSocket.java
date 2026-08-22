package com.fastpay.websocket;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastpay.common.Constants;
import com.fastpay.entity.Merchant;
import com.fastpay.entity.PayOrder;
import com.fastpay.mapper.MerchantMapper;
import com.fastpay.mapper.PayOrderMapper;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付结果 WebSocket 服务端
 * 用于向支付页面推送支付结果通知
 * 
 * 连接地址：ws://host:port/ws/pay/{merchantNo}/{outTradeNo}
 * merchantNo 为商户编号
 * outTradeNo 为商户的商品订单号
 * 
 * @author FastPay
 */
@Slf4j
@Component
@ServerEndpoint("/ws/pay/{merchantNo}/{outTradeNo}")
public class PayResultWebSocket {


    /**
     * 存储所有连接的会话，key 为 merchantNo + outTradeNo，避免不同商户订单号冲突。
     */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * WebSocket 会话 key 分隔符
     */
    private static final String SESSION_KEY_SEPARATOR = ":";

    /**
     * JSON 序列化工具
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 连接建立成功调用的方法
     *
     * @param session    会话
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("merchantNo") String merchantNo, @PathParam("outTradeNo") String outTradeNo) {
        log.info("========== WebSocket 连接请求 ==========");
        Merchant merchant = SpringUtil.getBean(MerchantMapper.class).selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getMerchantNo, merchantNo));
        if (merchant == null) {
            // 商户不存在, 关闭连接
            try {
                // 使用 VIOLATED_POLICY 或 CANNOT_ACCEPT 关闭码
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "商户不存在"));
            } catch (IOException e) {
                log.error("关闭非法连接时发生IO异常", e);
            }
            // 阻止后续代码执行
            return;
        }

        String sessionKey = buildSessionKey(merchantNo, outTradeNo);
        log.info("WebSocket 连接建立开始，merchantNo={}, outTradeNo={}, sessionId={}", merchantNo, outTradeNo, session.getId());
        SESSION_MAP.put(sessionKey, session);
        log.info("WebSocket 连接建立成功，merchantNo={}, outTradeNo={}, 当前连接数={}",
                merchantNo, outTradeNo, SESSION_MAP.size());
    }

    /**
     * 连接关闭调用的方法
     *
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     */
    @OnClose
    public void onClose(@PathParam("merchantNo") String merchantNo, @PathParam("outTradeNo") String outTradeNo, CloseReason closeReason) {
        SESSION_MAP.remove(buildSessionKey(merchantNo, outTradeNo));
        log.info("WebSocket 连接关闭，merchantNo={}, outTradeNo={}, 关闭原因: code={}, reason={}, 当前连接数={}",
                merchantNo, outTradeNo, closeReason.getCloseCode(), closeReason.getReasonPhrase(), SESSION_MAP.size());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message    客户端发送的消息
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     */
    @OnMessage
    public void onMessage(String message, @PathParam("merchantNo") String merchantNo, @PathParam("outTradeNo") String outTradeNo) {
        try {
            log.info("收到客户端消息，merchantNo={}, outTradeNo={}, message={}", merchantNo, outTradeNo, message);
            // 心跳响应
            if ("ping".equals(message)) {
                log.info("收到心跳请求，回复 pong，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo);
                Map<String, Object> data = Map.of(
                        "type", "HEARTBEAT",
                        "content", "pong"
                );
                sendMessage(merchantNo, outTradeNo, OBJECT_MAPPER.writeValueAsString(data));
            }
        } catch (Exception e) {
            log.warn("WebSocket会话不存在或已关闭，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo);
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session    会话
     * @param error      错误
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("merchantNo") String merchantNo, @PathParam("outTradeNo") String outTradeNo) {
        log.error("========== WebSocket 发生错误 ==========");
        log.error("WebSocket 错误，merchantNo={}, outTradeNo={}, sessionId={}, 错误类型: {}, 错误信息: {}",
                merchantNo, outTradeNo, session != null ? session.getId() : "null",
                error.getClass().getName(), error.getMessage());
        log.error("WebSocket 错误堆栈:", error);
        SESSION_MAP.remove(buildSessionKey(merchantNo, outTradeNo));
    }

    /**
     * 向指定商户订单的客户端发送消息
     *
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @param message    消息内容
     */
    public static void sendMessage(String merchantNo, String outTradeNo, String message) {
        Session session = SESSION_MAP.get(buildSessionKey(merchantNo, outTradeNo));
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("WebSocket消息发送成功，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo);
            } catch (IOException e) {
                log.error("WebSocket消息发送失败，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo, e);
            }
        } else {
            log.warn("WebSocket会话不存在或已关闭，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo);
        }
    }

    /**
     * 发送支付成功通知
     *
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @param orderNo    平台订单号
     * @param payAmount  实付金额
     */
    public static void sendPaySuccess(String merchantNo, String outTradeNo, String orderNo, String payAmount) {
        try {
            Map<String, Object> data = Map.of(
                    "type", "PAY_SUCCESS",
                    "outTradeNo", outTradeNo,
                    "orderNo", orderNo,
                    "payAmount", payAmount,
                    "message", "支付成功"
            );
            String message = OBJECT_MAPPER.writeValueAsString(data);
            sendMessage(merchantNo, outTradeNo, message);
            // 发送支付成功通知后，关闭该订单的连接
            endSession(merchantNo, outTradeNo, CloseReason.CloseCodes.NORMAL_CLOSURE, "支付已完成");
        } catch (Exception e) {
            log.error("发送支付成功通知失败，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo, e);
        }
    }

    /**
     * 发送支付失败通知
     *
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @param reason     失败原因
     */
    public static void sendPayFailed(String merchantNo, String outTradeNo, String reason) {
        try {
            Map<String, Object> data = Map.of(
                    "type", "PAY_FAILED",
                    "outTradeNo", outTradeNo,
                    "message", reason
            );
            String message = OBJECT_MAPPER.writeValueAsString(data);
            sendMessage(merchantNo, outTradeNo, message);
            endSession(merchantNo, outTradeNo, CloseReason.CloseCodes.NORMAL_CLOSURE, reason);
        } catch (Exception e) {
            log.error("发送支付失败通知失败，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo, e);
        }
    }

    /**
     * 发送订单超时通知
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @param reason     超时原因
     */
    public static void sendPayTimeOut(String merchantNo, String outTradeNo, String reason) {
        try {
            Map<String, Object> data = Map.of(
                    "type", "PAY_TIMEOUT",
                    "outTradeNo", outTradeNo,
                    "message", reason
            );
            String message = OBJECT_MAPPER.writeValueAsString(data);
            sendMessage(merchantNo, outTradeNo, message);
            endSession(merchantNo, outTradeNo, CloseReason.CloseCodes.NORMAL_CLOSURE, reason);
        } catch (Exception e) {
            log.error("发送支付超时通知失败，merchantNo={}, outTradeNo={}", merchantNo, outTradeNo, e);
        }
    }

    /**
     * 检查指定商户订单是否有活跃的 WebSocket 连接
     *
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @return 是否有活跃连接
     */
    public static boolean hasActiveConnection(String merchantNo, String outTradeNo) {
        Session session = SESSION_MAP.get(buildSessionKey(merchantNo, outTradeNo));
        return session != null && session.isOpen();
    }

    /**
     * 关闭连接
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @param code       关闭码
     * @param reason     关闭原因
     */
    public static void endSession(String merchantNo, String outTradeNo, CloseReason.CloseCodes code, String reason) {
        String sessionKey = buildSessionKey(merchantNo, outTradeNo);
        Session session = SESSION_MAP.get(sessionKey);
        if (session != null && session.isOpen()) {
            try {
                // 优雅地关闭连接，并告知客户端原因
                session.close(new CloseReason(code, reason));
                log.info("主动关闭WebSocket会话，merchantNo={}, outTradeNo={}, 原因: {}", merchantNo, outTradeNo, reason);
            } catch (IOException e) {
                log.error("主动关闭会话时发生异常", e);
            } finally {
                // 确保资源被清理
                SESSION_MAP.remove(sessionKey);
            }
        }
    }

    /**
     * 超期未支付的订单,关闭ws连接
     */
    public static void closePayTimeOut() {
        if (!SESSION_MAP.isEmpty()) {
            final Set<Map.Entry<String, Session>> entries = SESSION_MAP.entrySet();
            for (Map.Entry<String, Session> entry : entries) {
                String[] keyParts = parseSessionKey(entry.getKey());
                if (keyParts == null) {
                    SESSION_MAP.remove(entry.getKey());
                    continue;
                }

                final String merchantNo = keyParts[0];
                final String outTradeNo = keyParts[1];
                Merchant merchant = SpringUtil.getBean(MerchantMapper.class).selectOne(new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantNo, merchantNo));
                if (merchant == null) {
                    SESSION_MAP.remove(entry.getKey());
                    continue;
                }

                final LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getMerchantId, merchant.getId())
                        .eq(PayOrder::getOutTradeNo, outTradeNo)
                        .eq(PayOrder::getStatus, Constants.OrderStatus.UNPAID)
                        .lt(PayOrder::getExpireTime, LocalDateTime.now());
                final PayOrder payOrder = SpringUtil.getBean(PayOrderMapper.class).selectOne(wrapper);
                if (payOrder != null) {
                    // 超期未支付的订单,定期关闭ws连接
                    sendPayTimeOut(merchantNo, outTradeNo, "订单已过期");
                }

            }
        }

    }

    /**
     * 构造 WebSocket 会话 key。
     *
     * @param merchantNo 商户编号
     * @param outTradeNo 商户订单号
     * @return 会话 key
     */
    private static String buildSessionKey(String merchantNo, String outTradeNo) {
        return merchantNo + SESSION_KEY_SEPARATOR + outTradeNo;
    }

    /**
     * 解析 WebSocket 会话 key。
     *
     * @param sessionKey 会话 key
     * @return 下标 0 为商户编号，下标 1 为商户订单号；格式错误时返回 null
     */
    private static String[] parseSessionKey(String sessionKey) {
        int separatorIndex = sessionKey.indexOf(SESSION_KEY_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex >= sessionKey.length() - 1) {
            return null;
        }
        return new String[]{
                sessionKey.substring(0, separatorIndex),
                sessionKey.substring(separatorIndex + SESSION_KEY_SEPARATOR.length())
        };
    }
}
