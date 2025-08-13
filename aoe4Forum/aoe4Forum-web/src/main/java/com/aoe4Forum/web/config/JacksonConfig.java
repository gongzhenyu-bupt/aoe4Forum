package com.aoe4Forum.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 解决 LocalDateTime 问题
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 使用 ISO-8601 字符串格式
        return objectMapper;
    }
}