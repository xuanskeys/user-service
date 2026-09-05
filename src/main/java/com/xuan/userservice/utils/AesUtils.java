package com.xuan.userservice.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具类（CBC/PKCS5Padding）
 *
 * <p>前端使用相同算法对密码做加密传输，后端收到密文后用 {@link #decrypt(String, String)} 解密，
 * 再交由 BCrypt 加密后落库。AES 密钥来源于 Nacos 配置中心（通过 encrypt_config 表记录配置标识后拉取）。
 *
 * <p>密钥长度需为 16/24/32 字节（对应 AES-128/192/256）；IV 长度固定 16 字节。
 * 约定：前端与后端使用相同的 key，IV 约定固定值（实际生产建议由后端下发随机 IV）。
 */
@Slf4j
public final class AesUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /** 默认 IV（16 字节），生产环境建议改为后端随机下发 */
    private static final String DEFAULT_IV = "0000000000000000";

    private AesUtils() {
    }

    /**
     * AES 解密（使用默认 IV）
     *
     * @param base64Key Base64 或明文字符串形式的密钥（密钥本身按 UTF-8 取字节，长度需满足 AES 要求）
     * @param cipherText Base64 编码的密文
     * @return 明文
     */
    public static String decrypt(String base64Key, String cipherText) {
        return decrypt(base64Key, DEFAULT_IV, cipherText);
    }

    /**
     * AES 解密（指定 IV）
     */
    public static String decrypt(String key, String iv, String cipherText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes(key), ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new RuntimeException("AES 解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * AES 加密（使用默认 IV），用于后端回传（如需要）
     */
    public static String encrypt(String key, String plainText) {
        return encrypt(key, DEFAULT_IV, plainText);
    }

    /**
     * AES 加密（指定 IV）
     */
    public static String encrypt(String key, String iv, String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes(key), ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("AES 加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 支持 16/24/32 字节明文密钥或其 Base64 表示；不静默补齐或截断密钥。
     */
    private static byte[] keyBytes(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (validLength(keyBytes)) {
            return keyBytes;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(key);
            if (validLength(decoded)) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to the explicit validation error below.
        }
        throw new IllegalArgumentException("AES 密钥必须是 16/24/32 字节明文或对应的 Base64 字符串");
    }

    private static boolean validLength(byte[] bytes) {
        return bytes.length == 16 || bytes.length == 24 || bytes.length == 32;
    }
}
