-- 登录 IP 字段修复迁移。
-- 旧版本登录接口可能把整段 X-Forwarded-For 代理链写入 last_login_ip，
-- 这里统一放宽管理员和商户登录 IP 字段，并配合 ClientIpUtil 只保存单个合法地址。
ALTER TABLE `fp_admin`
    MODIFY COLUMN `last_login_ip` VARCHAR(64) DEFAULT NULL
    COMMENT '最后登录IP地址（仅保存单个合法IPv4/IPv6）';

ALTER TABLE `fp_merchant`
    MODIFY COLUMN `last_login_ip` VARCHAR(64) DEFAULT NULL
    COMMENT '最后登录IP地址（仅保存单个合法IPv4/IPv6）';
