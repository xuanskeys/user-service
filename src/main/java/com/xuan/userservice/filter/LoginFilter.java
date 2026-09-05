package com.xuan.userservice.filter;

import com.xuan.userservice.entity.properties.SecurityProperties;
import com.xuan.userservice.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/**
 * 登录拦截器
 * 校验请求头中的 token（Authorization: Bearer <token>），校验失败直接返回 401。
 * 校验通过后把 userId / username 写入 request attribute，供 Controller 取用。
 */
@Slf4j
@Component
public class LoginFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final SecurityProperties securityProperties;

    public LoginFilter(JwtUtils jwtUtils, SecurityProperties securityProperties) {
        this.jwtUtils = jwtUtils;
        this.securityProperties = securityProperties;
    }

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return securityProperties.getLoginWhitelist().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(TOKEN_PREFIX)) {
            writeUnauthorized(response, "缺失或非法的 Authorization 头");
            return;
        }

        String token = header.substring(TOKEN_PREFIX.length());
        if (!jwtUtils.validateToken(token)) {
            writeUnauthorized(response, "token 无效或已过期");
            return;
        }

        try {
            String subject = jwtUtils.getSubject(token);
            request.setAttribute("userId", subject);
            request.setAttribute("username", jwtUtils.getClaim(token, "username"));
            request.setAttribute("tenantId", jwtUtils.getClaim(token, "tenantId"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(subject, null, java.util.List.of());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            writeUnauthorized(response, "token 解析失败");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + msg + "\",\"data\":null}");
    }
}
