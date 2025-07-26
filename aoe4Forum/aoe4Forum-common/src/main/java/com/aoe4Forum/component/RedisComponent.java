package com.aoe4Forum.component;

import com.aoe4Forum.entity.User;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.entity.dto.UserInfoDto;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.utils.CopyUtil;
import com.aoe4Forum.utils.StringTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Component
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private RedisTemplate redisTemplate;

    //验证码相关
    public String saveCheckCode(String code){
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setEx(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code,Constants.REDIS_KEY_EXPIRES_ONE_MIN*10, TimeUnit.MILLISECONDS);
        return checkCodeKey;
    }

    public String getCodeKey(String checkCodeKey){
        return (String)redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }

    public void cleanCheckCode(String checkCodeKey){
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }

    //   以userid为键的用户信息缓存
    public void saveUserInfo(User user){
        UserInfoDto userInfoDto = CopyUtil.copy(user, UserInfoDto.class);
        userInfoDto.setUsername(user.getName());
        userInfoDto.setExpireTime(System.currentTimeMillis()+Constants.REDIS_KEY_EXPIRES_ONE_WEEK/7);
        ObjectMapper objectMapper = new ObjectMapper();
        String dtoJson;
        try {
            dtoJson = objectMapper.writeValueAsString(userInfoDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("UserInfoDto 转换 JSON 失败", e);
        }
        redisUtils.setEx(
                Constants.USER_INFO+user.getId(),
                dtoJson, // 这里是字符串值
                Constants.REDIS_KEY_EXPIRES_ONE_WEEK/7,
                TimeUnit.MILLISECONDS
        );
    }

    public UserInfoDto getUserInfo(Long id){
        String res = redisUtils.get(Constants.USER_INFO+id);
        if(StringTools.isEmpty(res)){
            return null;
        }
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(res, UserInfoDto.class);
        }catch(JsonProcessingException e) {
            throw new RuntimeException("Redis TokenUserInfoDto Json解析失败", e);
        }
    }

    public void cleanUserInfo(Long id){
        redisUtils.delete(Constants.USER_INFO+id);
    }

    //    以userToken为键的用户信息缓存，用于登录验证
    public void saveToken(TokenUserInfoDto tokenUserInfoDto){
        String token = UUID.randomUUID().toString();
        tokenUserInfoDto.setToken(token);
        tokenUserInfoDto.setExpireTime(System.currentTimeMillis()+Constants.REDIS_KEY_EXPIRES_ONE_WEEK);
        ObjectMapper objectMapper = new ObjectMapper();
        String dtoJson;
        try {
            dtoJson = objectMapper.writeValueAsString(tokenUserInfoDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TokenUserInfoDto 转换 JSON 失败", e);
        }
        // 存储字符串到 Redis
        redisUtils.setEx(
                Constants.REDIS_WEB_TOKEN + token,
                dtoJson, // 这里是字符串值
                Constants.REDIS_KEY_EXPIRES_ONE_WEEK,
                TimeUnit.MILLISECONDS
        );
    }
    //  转json格式
    public void changeToken(TokenUserInfoDto tokenUserInfoDto){
        ObjectMapper objectMapper = new ObjectMapper();
        String dtoJson;
        try {
            dtoJson = objectMapper.writeValueAsString(tokenUserInfoDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("TokenUserInfoDto 转换 JSON 失败", e);
        }
        redisUtils.set(Constants.REDIS_WEB_TOKEN + tokenUserInfoDto.getToken(),dtoJson);
    }

    public void cleanToken(String token){
        redisUtils.delete(Constants.REDIS_WEB_TOKEN + token);
    }

    public TokenUserInfoDto getTokenUserInfoDto(String token){
        String res = redisUtils.get(Constants.REDIS_WEB_TOKEN+token);
        if(StringTools.isEmpty(res)){
            return null;
        }
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(res, TokenUserInfoDto.class);
        }catch(JsonProcessingException e) {
            throw new RuntimeException("Redis TokenUserInfoDto Json解析失败", e);
        }
    }

}