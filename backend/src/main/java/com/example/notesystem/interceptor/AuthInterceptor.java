package com.example.notesystem.interceptor;

import com.example.notesystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 身份验证拦截器
 * 负责解析 JWT Token 并将用户 ID 注入请求上下文
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // 🚀 1. 核心修复：放行跨域预检请求 (OPTIONS)
        // 浏览器在发起跨域 POST 请求前会先发 OPTIONS 请求，如果不放行会导致 403 Forbidden
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. Token 提取逻辑
        String token = null;
        String authHeader = request.getHeader("Authorization");

        // 优先从 Header 获取 (格式: Bearer <token>)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 备选方案：从 URL 参数获取 (用于文件下载或某些特殊跳转场景)
            token = request.getParameter("token");
        }

        // 3. 校验 Token 是否存在
        if (token == null || token.isEmpty()) {
            sendErrorResponse(response, "Missing or invalid token");
            return false;
        }

        try {
            // 4. 解析 Token
            Claims claims = jwtUtil.parseToken(token);
            
            // 5. 注入上下文：将 userId 存入 request 作用域
            // 后面的 Controller 可以通过 (Long)request.getAttribute("userId") 直接获取
            request.setAttribute("userId", claims.get("userId"));
            return true;
            
        } catch (Exception e) {
            // 6. 处理 Token 过期或非法的情况
            sendErrorResponse(response, "Token expired or invalid");
            return false;
        }
    }

    /**
     * 辅助方法：返回标准化的 JSON 错误信息
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"message\": \"%s\"}", message));
    }
}
