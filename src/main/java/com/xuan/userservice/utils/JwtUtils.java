package com.xuan.userservice.utils;

import com.xuan.userservice.entity.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 生成token、解析token等职责
 */
@Component
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // 将配置的字符串密钥转为 HMAC-SHA 算法要求的 SecretKey
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token（不含自定义载荷，仅 subject）
     */
    public String generateToken(String subject) {
        return generateToken(subject, new HashMap<>());
    }

    /**
     * 生成 token（含自定义载荷）
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration() * 1000L);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token，返回 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 token 中的 subject（通常是用户名 / 用户标识）
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 获取 token 中的指定载荷
     */
    public Object getClaim(String token, String claimKey) {
        return parseToken(token).get(claimKey);
    }

    /**
     * 校验 token 是否未过期
     */
    public boolean validateToken(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 token 剩余有效期（秒）
     */
    public long getRemainingExpiration(String token) {
        Date expiration = parseToken(token).getExpiration();
        return Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000L);
    }
}
