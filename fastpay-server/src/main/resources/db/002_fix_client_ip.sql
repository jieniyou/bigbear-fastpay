-- 订单客户端 IP 修复迁移。
-- 旧版本可能使用 VARCHAR(20)/VARCHAR(50)，并且错误地保存整段 X-Forwarded-For。
-- 该脚本用于已有数据库升级；新库由 init.sql 直接创建 VARCHAR(64)。
ALTER TABLE `fp_pay_order`
    MODIFY COLUMN `client_ip` VARCHAR(64) DEFAULT NULL
    COMMENT '客户端IP地址（仅保存单个合法IPv4/IPv6）';
