package com.aoe4Forum.web.controller;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.CanSetRequestParams;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class ABaseController {

    @Autowired
    private RedisComponent redisComponent;

    protected void saveToken2Cookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(Constants.WEB_TOKEN, token);
        cookie.setMaxAge(Constants.TIME_SECOND_WEEK);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    protected TokenUserInfoDto getTokenFromCookie() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
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
        return redisComponent.getTokenUserInfoDto(token);
    }

    protected void cleanCookie(HttpServletResponse response) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Cookie[] cookie = request.getCookies();
        if(cookie==null){
            return;
        }
        for(Cookie c : cookie) {
            if(c.getName().equals(Constants.WEB_TOKEN)) {
                redisComponent.cleanToken(c.getValue());
                c.setMaxAge(0);
                c.setPath("/");
                response.addCookie(c);
                break;
            }
        }
    }
    protected void setRequestParams(HttpServletRequest request, CanSetRequestParams t){
        t.setToken((String) request.getAttribute("token"));
        t.setUsername((String) request.getAttribute("username"));
        t.setUserId((Long) request.getAttribute("userId"));
    }




}
