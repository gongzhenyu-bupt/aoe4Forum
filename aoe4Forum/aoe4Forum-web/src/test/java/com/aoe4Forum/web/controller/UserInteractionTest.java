package com.aoe4Forum.web.controller;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.entity.request.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserInteractionTest {

    private final String BASE_URL = "http://localhost:7071";
    private final RestTemplate restTemplate = new RestTemplate();

    private final List<Long> userIds = new ArrayList<>();
    private final Map<Long, String> userNames = new HashMap<>();

    @Resource
    private RedisComponent redisComponent;

    @Test
    public void testCreateAndFollowUsers() throws InterruptedException {
        int userCount = 500;
        registerUsers(userCount);
        followEachOther(userCount);
    }

    private void registerUsers(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            String username = "testuser" + i;
            String password = "Password123";
            String phone = String.format("13%09d", i);  // 生成合法的手机号

            // 获取验证码 key
            String checkCodeKey = getCheckCodeKey();
            // 从 Redis 获取验证码
            String checkCode = redisComponent.getCodeKey(checkCodeKey); // 这里使用 redis 获取验证码

            RegisterRequest request = new RegisterRequest();
            request.setName(username);
            request.setPassword(password);
            request.setPhoneNo(phone);
            request.setCheckCode(checkCode);
            request.setCheckCodeKey(checkCodeKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RegisterRequest> httpEntity = new HttpEntity<>(request, headers);

            try {
                ResponseEntity<ResponseVO<Map<String, String>>> response =
                        restTemplate.exchange(BASE_URL + "/account/register", HttpMethod.POST, httpEntity,
                                new ParameterizedTypeReference<ResponseVO<Map<String, String>>>() {});
                if (response.getBody() != null && response.getBody().getCode().equals("0")) {
                    String userIdStr = response.getBody().getData().get("userId");
                    Long userId = Long.valueOf(userIdStr);
                    userIds.add(userId);
                    userNames.put(userId, username);
                } else {
                    log.warn("注册失败: {}", response.getBody());
                }
            } catch (Exception e) {
                log.error("注册异常: {}", e.getMessage());
            }

            Thread.sleep(20); // 避免请求过快
        }

        log.info("注册完成，共成功 {} 个用户", userIds.size());
    }

    private String getCheckCodeKey() {
        ResponseEntity<ResponseVO<Map<String, String>>> response =
                restTemplate.exchange(BASE_URL + "/account/checkCode", HttpMethod.GET, null,
                        new ParameterizedTypeReference<ResponseVO<Map<String, String>>>() {});
        if (response.getBody() != null && response.getBody().getCode().equals("0")) {
            return response.getBody().getData().get("checkCodeKey");
        }
        return UUID.randomUUID().toString(); // fallback
    }

    private void followEachOther(int maxFollowsPerUser) {
        for (int i = 0; i < userIds.size(); i++) {
            Long followerId = userIds.get(i);
            for (int j = i + 1; j < Math.min(i + 1 + maxFollowsPerUser, userIds.size()); j++) {
                Long followeeId = userIds.get(j);
                try {
                    follow(followerId, followeeId);
                    follow(followeeId, followerId); // 互相关注
                } catch (Exception e) {
                    log.error("关注失败: {} -> {}", followerId, followeeId);
                }
            }
        }

        log.info("互相关注完成");
    }

    private void follow(Long followerId, Long followeeId) {
        String url = String.format("%s/follow/follow?followerId=%d&followeeId=%d", BASE_URL, followerId, followeeId);
        ResponseEntity<ResponseVO> response = restTemplate.getForEntity(url, ResponseVO.class);
        if (response.getBody() != null && !response.getBody().getCode().equals("0")) {
            log.warn("关注失败 {} -> {}，msg: {}", followerId, followeeId, response.getBody().getMessage());
        }
    }
}
