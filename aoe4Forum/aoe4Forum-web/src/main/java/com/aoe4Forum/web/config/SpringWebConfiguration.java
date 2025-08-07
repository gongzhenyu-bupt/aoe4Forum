package com.aoe4Forum.web.config;

import com.aoe4Forum.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class SpringWebConfiguration implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 使用注入的loginInterceptor实例，而不是new一个新的
        InterceptorRegistration registration = registry.addInterceptor(loginInterceptor);

        registration.addPathPatterns("/**");

        registration.excludePathPatterns(
                "/img/**", "/static/**",
                "/account/login", "/account/register", "/account/checkCode",
                "/community/queryPostContent", "/community/queryPostByHot", "/community/queryPostByForum",
                "/community/getComment","/community/getCommentsByPostId","/community/getCommentsByParentId",
                "/community/getCommentsByParentIds","/swagger-ui/**","/v3/**","/community/queryPostById"
        );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/tempImg/**")
                .addResourceLocations("file:C:/Users/81595/Desktop/all/tempImg/");
        registry.addResourceHandler("/avatarImg/**")
                .addResourceLocations("file:C:/Users/81595/Desktop/all/avatarImg/");
    }
}

