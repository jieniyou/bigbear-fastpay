-- 回调与跳转 URL 字段扩容迁移。
-- sub2api/new-api 等接入方可能会在 return_url 中追加订单号、状态和恢复 Token，
-- 完整回跳 URL 容易超过旧版 VARCHAR(255)，这里统一扩容到 2048 字符。
ALTER TABLE `fp_merchant`
    MODIFY COLUMN `notify_url` VARCHAR(2048) DEFAULT NULL
    COMMENT '支付成功回调地址（用户支付成功后，平台会将支付结果回推给商户系统）',
    MODIFY COLUMN `return_url` VARCHAR(2048) DEFAULT NULL
    COMMENT '支付成功跳转地址（默认商户支付成功跳转地址，创建订单时传了returnUrl会以传入的为准）';

ALTER TABLE `fp_pay_order`
    MODIFY COLUMN `notify_url` VARCHAR(2048) DEFAULT NULL
    COMMENT '回调通知地址（支付成功后通知商户系统的URL）',
    MODIFY COLUMN `return_url` VARCHAR(2048) DEFAULT NULL
    COMMENT '支付成功跳转地址（支付成功后用户浏览器跳转的URL）';
