package com.fastpay.controller.merchant;

import com.fastpay.common.Result;
import com.fastpay.dto.LoginDTO;
import com.fastpay.entity.Merchant;
import com.fastpay.service.MerchantService;
import com.fastpay.util.ClientIpUtil;
import com.fastpay.util.MerchantSecurityUtil;
import com.fastpay.util.PublicUrlUtil;
import com.fastpay.vo.DashboardVO;
import com.fastpay.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商户平台 - 认证控制器
 *
 * @author FastPay
 */
@Tag(name = "商户平台-认证", description = "商户登录、信息、配置等接口")
@RestController
@RequestMapping("/api/merchant")
public class MerchantAuthController {

    private final MerchantService merchantService;
    
    @Value("${fastpay.notify.callback-url:}")
    private String notifyCallbackUrl;

    public MerchantAuthController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 商户登录
     */
    @Operation(summary = "商户登录", description = "商户账号密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String ip = ClientIpUtil.resolve(request);
        LoginVO vo = merchantService.login(dto, ip);
        return Result.success("登录成功", vo);
    }

    /**
     * 获取商户信息
     */
    @Operation(summary = "商户信息", description = "获取当前登录商户信息")
    @GetMapping("/info")
    public Result<Merchant> getInfo(HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.getById(merchantId);
        return Result.success(MerchantSecurityUtil.hidePassword(merchant));
    }

    /**
     * 获取控制台统计数据
     */
    @Operation(summary = "控制台数据", description = "获取商户控制台统计数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        DashboardVO vo = merchantService.getMerchantDashboard(merchantId);
        return Result.success(vo);
    }

    /**
     * 更新回调配置
     */
    @Operation(summary = "更新回调配置", description = "更新支付回调和跳转地址")
    @PutMapping("/callback-config")
    public Result<Void> updateCallbackConfig(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        String notifyUrl = params.get("notifyUrl");
        String returnUrl = params.get("returnUrl");
        merchantService.updateCallbackConfig(merchantId, notifyUrl, returnUrl);
        return Result.success("更新成功", null);
    }

    /**
     * 重置API密钥
     */
    @Operation(summary = "重置密钥", description = "重置商户API密钥")
    @PostMapping("/reset-key")
    public Result<Merchant> resetApiKey(HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        Merchant merchant = merchantService.resetApiKey(merchantId);
        return Result.success("重置成功", MerchantSecurityUtil.hidePassword(merchant));
    }

    /**
     * 获取监听回调配置
     */
    @Operation(summary = "监听回调配置", description = "获取监听软件回调地址等配置")
    @GetMapping("/notify/config")
    public Result<Map<String, String>> getNotifyConfig(HttpServletRequest request) {
        Map<String, String> config = new HashMap<>();
        String callbackUrl = StringUtils.hasText(notifyCallbackUrl)
                ? notifyCallbackUrl
                : PublicUrlUtil.getPublicOrigin(request) + request.getContextPath() + "/api/notify/callback";
        config.put("callbackUrl", callbackUrl);
        return Result.success(config);
    }

}
