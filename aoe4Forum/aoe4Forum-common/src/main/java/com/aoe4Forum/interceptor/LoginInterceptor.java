package com.aoe4Forum.interceptor;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

//    登录检测，检查token是否存在以及是否合法
    @Resource
    private RedisComponent redisComponent;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = null;

        // 从 Cookie 中查找名为 Constants.WEB_TOKEN 的 token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Constants.WEB_TOKEN.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        Map<String,String> result = new LinkedHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if(token == null || token.isEmpty()){
            result.put("code", "INVALID_TOKEN");
            result.put("message", "请求未包含Token,用户校验失败");
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(401);
            response.getWriter().println(objectMapper.writeValueAsString(result));
            return false;
        }
        try{
            TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
            request.setAttribute("userId", tokenUserInfoDto.getId());
            request.setAttribute("username",tokenUserInfoDto.getName());
            request.setAttribute("token", token);
        }catch (Exception e){
            result.put("code", "INVALID_TOKEN");
            result.put("message", "请求Token无效,用户校验失败");
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(401);
            response.getWriter().println(objectMapper.writeValueAsString(result));
            return false;
        }
        return true;
    }
}
