package com.xuan.userservice.config;

import com.xuan.userservice.entity.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.xuan.userservice.filter.LoginFilter;

/**
 * Spring Security 配置
 * - 放行认证相关接口（注册/登录/发送验证码/加密配置查询），其余请求经 {@link LoginFilter} 校验 token
 * - 提供 BCrypt PasswordEncoder Bean 供密码加密存储使用
 */
@Configuration
public class SecurityConfig {

    private final LoginFilter loginFilter;
    private final SecurityProperties securityProperties;

    public SecurityConfig(LoginFilter loginFilter, SecurityProperties securityProperties) {
        this.loginFilter = loginFilter;
        this.securityProperties = securityProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 认证相关接口全部放行
                        .requestMatchers(securityProperties.getLoginWhitelist().toArray(String[]::new)).permitAll()
                        // 其余接口需经过 LoginFilter 校验 token
                        .anyRequest().authenticated()
                )
                // 将自定义 token 校验拦截器插入到 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
