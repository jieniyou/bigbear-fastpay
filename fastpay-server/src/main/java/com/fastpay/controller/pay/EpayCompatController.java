package com.fastpay.controller.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastpay.common.BusinessException;
import com.fastpay.common.Constants;
import com.fastpay.dto.CreateOrderDTO;
import com.fastpay.entity.Merchant;
import com.fastpay.entity.PayOrder;
import com.fastpay.entity.PayQrcode;
import com.fastpay.entity.Shop;
import com.fastpay.mapper.MerchantMapper;
import com.fastpay.mapper.PayQrcodeMapper;
import com.fastpay.mapper.ShopMapper;
import com.fastpay.service.PayOrderService;
import com.fastpay.util.EpaySignUtil;
import com.fastpay.util.PublicUrlUtil;
import com.fastpay.util.SignUtil;
import com.fastpay.vo.PayResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 标准易支付兼容控制器
 * 兼容 new-api、sub2api 等系统常用的 submit.php、mapi.php、api.php 接入方式。
 *
 * @author FastPay
 */
@Slf4j
@RestController
public class EpayCompatController {

    private final PayOrderService payOrderService;
    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final PayQrcodeMapper payQrcodeMapper;

    @Value("${fastpay.pay.page-domain:}")
    private String pageDomain;

    public EpayCompatController(PayOrderService payOrderService, MerchantMapper merchantMapper,
                                ShopMapper shopMapper, PayQrcodeMapper payQrcodeMapper) {
        this.payOrderService = payOrderService;
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.payQrcodeMapper = payQrcodeMapper;
    }

    /**
     * 标准易支付页面跳转下单接口。
     *
     * @param params   易支付请求参数
     * @param request  当前请求
     * @param response 当前响应
     * @throws IOException 响应写入异常
     */
    @RequestMapping(value = "/submit.php", method = {RequestMethod.GET, RequestMethod.POST})
    public void submit(@RequestParam Map<String, String> params,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        try {
            PayResultVO vo = createEpayOrder(params, request);
            String payPageUrl = resolvePayPageDomain(request) + "/pay/" + vo.getOrderNo();
            response.sendRedirect(payPageUrl);
        } catch (Exception e) {
            log.error("Epay页面跳转下单失败: params={}", maskParams(params), e);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("error: " + e.getMessage());
        }
    }

    /**
     * 标准易支付接口下单接口。
     *
     * @param params  易支付请求参数
     * @param request 当前请求
     * @return 易支付格式响应
     */
    @RequestMapping(value = "/mapi.php", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> mapi(@RequestParam Map<String, String> params, HttpServletRequest request) {
        try {
            PayResultVO vo = createEpayOrder(params, request);
            String payPageUrl = resolvePayPageDomain(request) + "/pay/" + vo.getOrderNo();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", 1);
            result.put("msg", "success");
            result.put("trade_no", vo.getOrderNo());
            result.put("out_trade_no", vo.getOutTradeNo());
            result.put("payurl", payPageUrl);
            result.put("qrcode", payPageUrl);
            return result;
        } catch (Exception e) {
            log.error("Epay接口下单失败: params={}", maskParams(params), e);
            return error(e.getMessage());
        }
    }

    /**
     * 标准易支付查询接口。
     *
     * @param params 查询参数
     * @return 易支付格式响应
     */
    @RequestMapping(value = "/api.php", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> api(@RequestParam Map<String, String> params) {
        try {
            String act = value(params, "act");
            if (!StringUtils.hasText(act)) {
                return successMessage("FastPay Epay compatibility API");
            }

            Merchant merchant = getMerchant(required(params, "pid"));
            verifyApiKey(params, merchant);

            if ("check".equalsIgnoreCase(act) || "account".equalsIgnoreCase(act)) {
                Map<String, Object> result = successMessage("success");
                result.put("pid", merchant.getMerchantNo());
                return result;
            }

            if ("order".equalsIgnoreCase(act) || "query".equalsIgnoreCase(act) || "status".equalsIgnoreCase(act)) {
                return queryOrder(params, merchant);
            }

            return error("unsupported act: " + act);
        } catch (Exception e) {
            log.error("Epay查询接口失败: params={}", maskParams(params), e);
            return error(e.getMessage());
        }
    }

    /**
     * 创建标准易支付兼容订单。
     *
     * @param params  易支付请求参数
     * @param request 当前请求
     * @return 支付结果
     */
    private PayResultVO createEpayOrder(Map<String, String> params, HttpServletRequest request) {
        Merchant merchant = getMerchant(required(params, "pid"));
        verifyPaySign(params, merchant);

        String payType = normalizePayType(required(params, "type"));
        String shopNo = resolveShopNo(params, merchant.getId(), payType);
        BigDecimal amount = parseAmount(required(params, "money"));
        String subject = required(params, "name");

        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setMerchantNo(merchant.getMerchantNo());
        dto.setOutTradeNo(required(params, "out_trade_no"));
        dto.setShopNo(shopNo);
        dto.setAmount(amount);
        dto.setSubject(subject);
        dto.setPayType(payType);
        dto.setPayMethod(Constants.PayMethod.EPAY);
        dto.setReturnUrl(value(params, "return_url"));
        dto.setNotifyUrl(value(params, "notify_url"));
        dto.setExtParam(value(params, "param"));
        dto.setTimestamp(System.currentTimeMillis() / 1000);
        dto.setSign(buildFastPaySign(dto, merchant.getApiSecret()));

        return payOrderService.createOrder(dto, getClientIp(request));
    }

    /**
     * 查询订单并转换为标准易支付响应。
     *
     * @param params   查询参数
     * @param merchant 商户信息
     * @return 易支付格式订单信息
     */
    private Map<String, Object> queryOrder(Map<String, String> params, Merchant merchant) {
        String outTradeNo = value(params, "out_trade_no");
        PayOrder order = null;
        if (StringUtils.hasText(outTradeNo)) {
            order = payOrderService.queryOrderByOutTradeNo(merchant.getMerchantNo(), outTradeNo);
        }
        if (order == null && StringUtils.hasText(value(params, "trade_no"))) {
            order = payOrderService.queryOrder(value(params, "trade_no"));
        }
        if (order == null) {
            return error("订单不存在");
        }

        boolean paid = Constants.OrderStatus.PAID.equals(order.getStatus());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 1);
        result.put("msg", "success");
        result.put("pid", merchant.getMerchantNo());
        result.put("trade_no", order.getOrderNo());
        result.put("out_trade_no", order.getOutTradeNo());
        result.put("type", toEpayType(order.getPayType()));
        result.put("name", order.getSubject());
        result.put("money", formatMoney(order.getAmount()));
        result.put("status", paid ? 1 : 0);
        result.put("trade_status", paid ? "TRADE_SUCCESS" : "WAIT_BUYER_PAY");
        return result;
    }

    /**
     * 根据 PID 获取商户。
     *
     * @param pid 易支付 PID
     * @return 商户信息
     */
    private Merchant getMerchant(String pid) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .and(w -> w.eq(Merchant::getMerchantNo, pid)
                        .or()
                        .eq(Merchant::getApiKey, pid))
                .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        if (!Constants.Status.ENABLED.equals(merchant.getStatus())) {
            throw new BusinessException("商户已被禁用");
        }
        return merchant;
    }

    /**
     * 验证标准易支付支付签名。
     *
     * @param params   支付参数
     * @param merchant 商户信息
     */
    private void verifyPaySign(Map<String, String> params, Merchant merchant) {
        if (!EpaySignUtil.verifySign(params, merchant.getApiSecret())) {
            throw new BusinessException("签名验证失败");
        }
    }

    /**
     * 验证标准易支付查询密钥。
     *
     * @param params   查询参数
     * @param merchant 商户信息
     */
    private void verifyApiKey(Map<String, String> params, Merchant merchant) {
        String key = value(params, "key");
        if (!StringUtils.hasText(key) || !key.equals(merchant.getApiSecret())) {
            throw new BusinessException("商户密钥错误");
        }
    }

    /**
     * 解析标准易支付店铺编号。
     *
     * @param params     支付参数
     * @param merchantId 商户ID
     * @param payType    支付类型
     * @return 店铺编号
     */
    private String resolveShopNo(Map<String, String> params, Long merchantId, String payType) {
        String[] keys = Constants.PayType.ALIPAY.equals(payType)
                ? new String[]{"alipay_channel_id", "ali_channel_id", "alipayChannelId", "channel_id", "channelId", "shop_no", "shopNo"}
                : new String[]{"wxpay_channel_id", "wx_channel_id", "wechat_channel_id", "wechatChannelId", "weixinChannelId", "channel_id", "channelId", "shop_no", "shopNo"};

        for (String key : keys) {
            String candidate = value(params, key);
            if (StringUtils.hasText(candidate)) {
                return resolveShopNoCandidate(candidate, merchantId, payType);
            }
        }

        PayQrcode qrcode = payQrcodeMapper.selectOne(new LambdaQueryWrapper<PayQrcode>()
                .eq(PayQrcode::getMerchantId, merchantId)
                .eq(PayQrcode::getPayType, payType)
                .eq(PayQrcode::getStatus, Constants.Status.ENABLED)
                .orderByDesc(PayQrcode::getSortWeight)
                .orderByAsc(PayQrcode::getTotalCount)
                .last("LIMIT 1"));
        if (qrcode == null) {
            throw new BusinessException("暂无可用的收款通道");
        }

        Shop shop = shopMapper.selectById(qrcode.getShopId());
        if (shop == null || !StringUtils.hasText(shop.getShopNo())) {
            throw new BusinessException("店铺不存在");
        }
        return shop.getShopNo();
    }

    /**
     * 解析第三方传入的店铺或渠道标识。
     *
     * @param candidate  第三方传入的店铺编号、二维码ID或通道ID
     * @param merchantId 商户ID
     * @param payType    支付类型
     * @return 店铺编号
     */
    private String resolveShopNoCandidate(String candidate, Long merchantId, String payType) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchantId)
                .eq(Shop::getShopNo, candidate)
                .last("LIMIT 1"));
        if (shop != null) {
            return shop.getShopNo();
        }

        PayQrcode qrcode = payQrcodeMapper.selectOne(new LambdaQueryWrapper<PayQrcode>()
                .eq(PayQrcode::getMerchantId, merchantId)
                .eq(PayQrcode::getPayType, payType)
                .eq(PayQrcode::getStatus, Constants.Status.ENABLED)
                .and(w -> w.eq(PayQrcode::getChannelId, candidate)
                        .or()
                        .eq(PayQrcode::getId, candidate))
                .orderByDesc(PayQrcode::getSortWeight)
                .orderByAsc(PayQrcode::getTotalCount)
                .last("LIMIT 1"));
        if (qrcode != null) {
            Shop qrcodeShop = shopMapper.selectById(qrcode.getShopId());
            if (qrcodeShop != null && StringUtils.hasText(qrcodeShop.getShopNo())) {
                return qrcodeShop.getShopNo();
            }
        }

        throw new BusinessException("店铺或渠道不存在: " + candidate);
    }

    /**
     * 生成内部 FAST 易支付签名。
     *
     * @param dto       内部下单参数
     * @param apiSecret 商户 API Secret
     * @return FAST 易支付签名
     */
    private String buildFastPaySign(CreateOrderDTO dto, String apiSecret) {
        TreeMap<String, Object> signParams = new TreeMap<>();
        signParams.put("merchantNo", dto.getMerchantNo());
        signParams.put("outTradeNo", dto.getOutTradeNo());
        signParams.put("shopNo", dto.getShopNo());
        signParams.put("payType", dto.getPayType());
        signParams.put("amount", dto.getAmount().toPlainString());
        signParams.put("subject", dto.getSubject());
        signParams.put("timestamp", String.valueOf(dto.getTimestamp()));
        if (StringUtils.hasText(dto.getReturnUrl())) {
            signParams.put("returnUrl", dto.getReturnUrl());
        }
        if (StringUtils.hasText(dto.getExtParam())) {
            signParams.put("extParam", dto.getExtParam());
        }
        return SignUtil.generateSign(signParams, apiSecret);
    }

    /**
     * 转换支付类型。
     *
     * @param type 标准易支付支付类型
     * @return FAST 易支付支付类型
     */
    private String normalizePayType(String type) {
        if ("alipay".equalsIgnoreCase(type) || "ali".equalsIgnoreCase(type)) {
            return Constants.PayType.ALIPAY;
        }
        if ("wxpay".equalsIgnoreCase(type) || "wx".equalsIgnoreCase(type)
                || "wechat".equalsIgnoreCase(type) || "wechatpay".equalsIgnoreCase(type)) {
            return Constants.PayType.WXPAY;
        }
        throw new BusinessException("不支持的支付类型: " + type);
    }

    /**
     * 转换为标准易支付支付类型。
     *
     * @param payType FAST 易支付支付类型
     * @return 标准易支付支付类型
     */
    private String toEpayType(String payType) {
        if (Constants.PayType.ALIPAY.equals(payType)) {
            return "alipay";
        }
        return "wxpay";
    }

    /**
     * 解析订单金额。
     *
     * @param money 金额字符串
     * @return 金额
     */
    private BigDecimal parseAmount(String money) {
        try {
            BigDecimal amount = new BigDecimal(money);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("金额必须大于0");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new BusinessException("金额格式错误");
        }
    }

    /**
     * 格式化金额。
     *
     * @param amount 金额
     * @return 两位小数字符串
     */
    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 获取必填参数。
     *
     * @param params 参数集合
     * @param key    参数名
     * @return 参数值
     */
    private String required(Map<String, String> params, String key) {
        String value = value(params, key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("缺少参数: " + key);
        }
        return value;
    }

    /**
     * 获取参数值。
     *
     * @param params 参数集合
     * @param key    参数名
     * @return 参数值
     */
    private String value(Map<String, String> params, String key) {
        String value = params.get(key);
        return value == null ? "" : value.trim();
    }

    /**
     * 获取客户端 IP。
     *
     * @param request 当前请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 解析支付页访问地址。
     *
     * @param request 当前请求
     * @return 支付页基础地址
     */
    private String resolvePayPageDomain(HttpServletRequest request) {
        if (StringUtils.hasText(pageDomain)) {
            return pageDomain;
        }
        return PublicUrlUtil.getPublicOrigin(request) + "/fastpay-merchant";
    }

    /**
     * 构建成功响应。
     *
     * @param message 响应消息
     * @return 响应内容
     */
    private Map<String, Object> successMessage(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 1);
        result.put("msg", message);
        return result;
    }

    /**
     * 构建错误响应。
     *
     * @param message 错误消息
     * @return 响应内容
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", -1);
        result.put("msg", message);
        return result;
    }

    /**
     * 脱敏日志参数。
     *
     * @param params 原始参数
     * @return 脱敏参数
     */
    private Map<String, String> maskParams(Map<String, String> params) {
        Map<String, String> masked = new HashMap<>(params);
        if (masked.containsKey("key")) {
            masked.put("key", "***");
        }
        if (masked.containsKey("sign")) {
            masked.put("sign", "***");
        }
        return masked;
    }
}
