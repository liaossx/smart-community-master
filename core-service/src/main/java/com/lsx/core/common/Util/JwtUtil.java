package com.lsx.core.common.Util;

import com.lsx.core.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // BCrypt 加密器（用于密码加密和校验）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret:defaultSecretKey}") // 从配置文件读取密钥，默认值仅用于开发
    private String secret;

    @Value("${jwt.expiration:86400000}") // 过期时间，默认24小时（单位：毫秒）
    private long expiration;

    // 暴露 secret 供过滤器使用（解析角色时需要）
    public String getSecret() {
        return secret;
    }

    @PostConstruct
    public void printJwtConfig() {
        System.out.println("===== JWT配置加载 =====");
        System.out.println("jwt.secret: " + secret);
        System.out.println("jwt.expiration: " + expiration + "ms（" + expiration/1000/3600 + "小时）");
        System.out.println("======================");
    }

    /**
     * 密码加密（注册/修改密码时使用）
     */
    public String encryptPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 密码校验（登录时使用）
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 生成JWT令牌（登录成功后使用）
     * 关键：角色添加 ROLE_ 前缀，适配 Spring Security
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());

        // ✅ 修改这里：直接使用数据库的角色，不加 ROLE_ 前缀
        claims.put("role", user.getRole()); 
        
        // 增加 communityId
        if (user.getCommunityId() != null) {
            claims.put("communityId", user.getCommunityId());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    /**
     * 从令牌中获取角色
     */
    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }
    
    /**
     * 从令牌中获取社区ID
     */
    public Long getCommunityIdFromToken(String token) {
        return getClaimsFromToken(token).get("communityId", Long.class);
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    /**
     * 验证令牌是否有效（不查库，仅校验签名和过期时间）
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查令牌是否过期
     */
    private boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 获取令牌过期时间
     */
    private Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * 从令牌中获取所有声明（包含角色、用户ID等信息）
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}