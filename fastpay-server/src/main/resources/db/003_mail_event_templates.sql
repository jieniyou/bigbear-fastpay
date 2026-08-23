-- 邮件事件模板与一次性操作 Token 迁移。
-- 该脚本用于已有数据库升级；新库由 init.sql 直接创建同等结构。

CREATE TABLE IF NOT EXISTS `fp_mail_action_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `token_hash` VARCHAR(64) NOT NULL COMMENT 'Token SHA-256摘要',
    `merchant_id` BIGINT NOT NULL COMMENT '商户ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '平台订单号',
    `action` VARCHAR(20) NOT NULL COMMENT '操作类型：confirm/close',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_order_action` (`merchant_id`, `order_no`, `used_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件订单操作Token表';

INSERT INTO `fp_system_config` (`config_key`, `config_value`, `remark`) VALUES
('mail.event.order-notify.subject', '【{{site_name}}】新订单通知：{{order_no}}', '普通订单通知邮件主题'),
('mail.event.order-notify.template', '', '普通订单通知HTML模板，留空时使用系统默认模板'),
('mail.event.order-confirm.enabled', 'true', '订单确认通知事件开关'),
('mail.event.order-confirm.subject', '【{{site_name}}】订单确认成功：{{order_no}}', '订单确认通知邮件主题'),
('mail.event.order-confirm.template', '', '订单确认通知HTML模板，留空时使用系统默认模板'),
('mail.event.order-close.enabled', 'true', '订单关闭通知事件开关'),
('mail.event.order-close.subject', '【{{site_name}}】订单已关闭：{{order_no}}', '订单关闭通知邮件主题'),
('mail.event.order-close.template', '', '订单关闭通知HTML模板，留空时使用系统默认模板')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
