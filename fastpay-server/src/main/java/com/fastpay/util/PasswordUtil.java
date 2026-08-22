package com.fastpay.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 密码工具类
 * 统一处理密码加密、校验和旧 MD5 密码兼容迁移。
 *
 * @author FastPay
 */
public class PasswordUtil {

    /**
     * MD5 十六进制格式匹配，用于兼容历史密码
     */
    private static final Pattern MD5_HEX_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    /**
     * 已知弱默认密码集合，初始化管理员时禁止使用
     */
    private static final Set<String> WEAK_DEFAULT_PASSWORDS = Set.of(
            "123456",
            "123456@",
            "admin123",
            "password",
            "fastpay"
    );

    private PasswordUtil() {
    }

    /**
     * 使用 BCrypt 加密明文密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 密码哈希
     */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }

    /**
     * 校验明文密码是否匹配已存储密码
     * 兼容历史 MD5 哈希，便于老账号登录后迁移。
     *
     * @param rawPassword    明文密码
     * @param storedPassword 已存储密码哈希
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        if (isMd5(storedPassword)) {
            return SecureUtil.md5(rawPassword).equalsIgnoreCase(storedPassword);
        }
        return false;
    }

    /**
     * 判断密码哈希是否需要升级为 BCrypt
     *
     * @param storedPassword 已存储密码哈希
     * @return 是否需要升级
     */
    public static boolean needsRehash(String storedPassword) {
        return !isBcrypt(storedPassword);
    }

    /**
     * 判断初始化管理员密码是否为弱密码
     *
     * @param password 初始化密码
     * @return 是否弱密码
     */
    public static boolean isWeakInitialPassword(String password) {
        return !StringUtils.hasText(password)
                || password.length() < 12
                || WEAK_DEFAULT_PASSWORDS.contains(password.toLowerCase());
    }

    /**
     * 判断是否为 BCrypt 哈希
     *
     * @param password 已存储密码哈希
     * @return 是否 BCrypt
     */
    private static boolean isBcrypt(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    /**
     * 判断是否为历史 MD5 哈希
     *
     * @param password 已存储密码哈希
     * @return 是否 MD5
     */
    private static boolean isMd5(String password) {
        return MD5_HEX_PATTERN.matcher(password).matches();
    }
}
