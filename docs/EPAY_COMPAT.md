# 标准易支付兼容接口说明

FAST 易支付已兼容 new-api、sub2api 等系统常见的标准 Epay 接入方式。部署后请在第三方系统里选择「易支付 / Epay」服务商。

## 第三方系统配置

| 字段 | 填写内容 |
|------|----------|
| Epay 端点 / API 基础地址 | `https://你的域名/fastpay-server` |
| PID / 易支付商户 ID | FAST 易支付商户中心的「商户编号」，例如 `M787477530213` |
| PKey / Epay 密钥 | FAST 易支付商户中心的「API Secret」，不是 API Key |
| 微信渠道 ID | 可填 FAST 易支付店铺编号 `shopNo`；也兼容二维码 ID / 通道 ID；不填时系统会自动选择该商户下启用的微信收款码 |
| 支付宝渠道 ID | 可填 FAST 易支付店铺编号 `shopNo`；也兼容二维码 ID / 通道 ID；不使用支付宝可留空 |
| 回调地址 | 第三方系统自己的异步通知地址 |
| 跳转地址 | 第三方系统自己的同步跳转地址 |

## 已兼容接口

```text
/fastpay-server/submit.php  # 页面跳转支付接口，创建订单后跳转到 FAST 易支付收银台
/fastpay-server/mapi.php    # 接口支付接口，返回 payurl/qrcode/trade_no 等字段
/fastpay-server/api.php     # 查询接口，支持 act=check/account/order/query/status
```

## 支付请求参数

| 参数 | 说明 |
|------|------|
| `pid` | 商户编号，兼容误填 API Key 的场景 |
| `type` | 支付类型，支持 `wxpay`、`alipay` |
| `out_trade_no` | 第三方系统订单号 |
| `notify_url` | 第三方系统异步通知地址 |
| `return_url` | 第三方系统同步跳转地址 |
| `name` | 商品名称 |
| `money` | 订单金额 |
| `param` | 透传参数，支付成功回调时原样返回 |
| `sign` | 标准 Epay MD5 签名 |
| `sign_type` | 固定 `MD5` |

## 回调格式

支付成功后，FAST 易支付会向 `notify_url` 发送标准 Epay 表单参数：

```text
pid=M787477530213                  # FAST 易支付商户编号
trade_no=FP20260822120000123456    # FAST 易支付平台订单号
out_trade_no=ORDER202608220001     # 第三方系统订单号
type=wxpay                         # 支付类型
name=测试商品                       # 商品名称
money=1.00                         # 支付金额
trade_status=TRADE_SUCCESS         # 支付成功状态
param=透传参数                      # 原样返回支付请求里的 param
sign=md5签名                        # 标准 Epay 签名
sign_type=MD5                      # 签名类型
```

第三方系统返回 `success` 时，FAST 易支付会标记通知成功。
