package com.lsx.core.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.core.common.Util.JwtUtil;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/doc.html",
            "/doc.html/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v2/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/META-INF.resources/**",
            "/api/user/login",
            "/api/user/register",
            "/api/common/**"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // ===== 白名单放行 =====
        for (String whitePath : WHITE_LIST) {
            if (pathMatcher.match(whitePath, requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String token = request.getHeader("Authorization");

        try {
            if (token == null || token.trim().isEmpty()) {
                throw new RuntimeException("请先登录（Token 不存在）");
            }

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // ---- 1. 从 token 中解析 userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                UserContext.setUserId(userId);
            }
            
            // ---- 1.1 解析 role 和 communityId 并设置到 UserContext
            String roleStr = jwtUtil.getRoleFromToken(token);
            UserContext.setRole(roleStr);
            
            Long communityId = jwtUtil.getCommunityIdFromToken(token);
            if (communityId != null) {
                UserContext.setCommunityId(communityId);
            }

            // ---- 2. 解析用户名
            String username = jwtUtil.getUsernameFromToken(token);
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("Token 中未包含有效用户名");
            }

            // ---- 3. 查询数据库用户
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                throw new RuntimeException("Token 对应的用户不存在");
            }

            // ---- 4. 验证 token 是否有效
            if (!jwtUtil.validateToken(token, user)) {
                throw new RuntimeException("Token 已过期或无效，请重新登录");
            }

            // ---- 5. 设置权限 ⭐⭐⭐ 修复这里！
            Claims claims = Jwts.parser().setSigningKey(jwtUtil.getSecret()).parseClaimsJws(token).getBody();
            String role = claims.get("role", String.class);

            // ✅ 关键修复：确保权限字符串有 ROLE_ 前缀
            String authority;
            if (role == null || role.trim().isEmpty()) {
                authority = "ROLE_USER"; // 默认角色
            } else {
                // 如果JWT中已经有ROLE_前缀，保持原样；否则添加
                if (role.startsWith("ROLE_")) {
                    authority = role;
                } else {
                    authority = "ROLE_" + role;
                }
            }

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(authority)
            );

            Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // ---- 6.继续执行
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter out = response.getWriter();
            out.write("{\"code\":401,\"msg\":\"" + e.getMessage() + "\",\"data\":null}");
            out.flush();
            out.close();

        } finally {
            UserContext.clear();
        }
    }
}