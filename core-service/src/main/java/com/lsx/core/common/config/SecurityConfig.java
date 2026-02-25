package com.lsx.core.common.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Resource
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Spring Security 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 核心安全配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("===== 加载 Security 配置：启用 JWT 登录校验 =====");

        http
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())

                // 使用无状态会话（JWT 必须这样）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置接口权限规则
                .authorizeHttpRequests(auth -> auth

                        /* ===== Swagger/Knife4j 放行 ===== */
                        .antMatchers(
                                "/doc.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v2/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        /* ===== 登录注册放行 ===== */
                        .antMatchers(
                                "/api/user/login",
                                "/api/user/register",
                                "/api/common/**"
                        ).permitAll()

                        /* ===== 管理员（ADMIN）可访问 ===== ⭐ 先配置具体的admin路径 */
                        .antMatchers("/api/repair/admin/**").hasRole("ADMIN")  // ⭐ 放在前面！
                        .antMatchers("/api/house/admin/**").hasRole("ADMIN")
                        .antMatchers("/api/parking/order/admin/**").hasRole("ADMIN") // 停车订单管理
                        .antMatchers("/api/parking/space/admin/**").hasRole("ADMIN") // 车位管理
                        .antMatchers("/api/parking/reserve/admin/**").hasRole("ADMIN") // 车位预订管理
                        .antMatchers("/api/house/updateUserHouseStatus").hasRole("ADMIN")

                        /* ===== 普通业主（OWNER）可访问 ===== ⭐ 后配置通用路径 */
                        .antMatchers("/api/repair/**").hasRole("OWNER")  // ⭐ 放在后面！
                        .antMatchers("/api/user/bindUserToHouse").hasRole("OWNER")

                        /* ===== 其他接口需要登录 ===== */
                        .anyRequest().authenticated()
                )

                // 自定义无权限处理
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // 加载 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 禁用默认登录/注销
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}
